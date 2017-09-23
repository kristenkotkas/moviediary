package common.verticle.rx;

import common.util.Status;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.CookieHandler;
import io.vertx.rxjava.ext.web.handler.SessionHandler;
import io.vertx.rxjava.ext.web.sstore.LocalSessionStore;
import lombok.extern.slf4j.Slf4j;
import rx.Single;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static common.util.ConditionUtils.chain;
import static common.util.NetworkUtils.getRandomUnboundPort;
import static common.util.Status.UNAUTHORIZED;
import static common.util.rx.RxUtils.toSubscriber;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@Slf4j
public abstract class RestApiRxVerticle extends BaseRxVerticle {

  protected final Single<Void> createHttpServer(Router router, String host, int port) {
    return vertx.createHttpServer(new HttpServerOptions()
        .setCompressionSupported(true)
        .setCompressionLevel(3))
                .requestHandler(router::accept)
                .rxListen(port, host)
                .map(server -> null);
  }

  protected final Single<Void> createHttpServer(Router router, int port) {
    return createHttpServer(router, "localhost", port);
  }

  // TODO: 21.08.2017 some stuff probably only needed in auth/api-gateway

  protected void enableLocalSession(Router router) {
    router.route().handler(CookieHandler.create());
    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
  }

  protected void requireLogin(RoutingContext ctx, BiConsumer<RoutingContext, JsonObject> biHandler) {
    Optional<JsonObject> principal = Optional.ofNullable(ctx.request().getHeader("user-principal"))
                                             .map(JsonObject::new);
    if (principal.isPresent()) {
      biHandler.accept(ctx, principal.get());
    } else {
      ctx.response()
         .setStatusCode(UNAUTHORIZED)
         .end(new JsonObject().put("message", "unauthorized").encodePrettily());
    }
  }

  @SafeVarargs
  protected final void startRouter(String address, Class clazz, Future<Void> future, Consumer<EBRouter>... consumers) {
    chain(config().getInteger("http.port", getRandomUnboundPort()), port ->
        createHttpServer(chain(EBRouter.create(vertx, address, clazz), consumers).start(), port)
            .flatMap(v -> publishEventBusService(clazz))
            .flatMap(v -> publishHttpEndpoint(port))
            .subscribe(toSubscriber(future.completer())));
  }

  protected static class EBRouter {
    private final Router router;
    private final Vertx vertx;
    private final String address;
    private final Set<String> serviceMethods;

    private EBRouter(Vertx vertx, String serviceAddress, Class serviceClass) {
      this.router = Router.router(vertx).exceptionHandler(thr -> log.error("EventbusRouter error", thr));
      this.vertx = vertx;
      this.address = serviceAddress;
      this.serviceMethods = findServiceMethods(serviceClass);
    }

    public static EBRouter create(Vertx vertx, String serviceAddress, Class serviceClass) {
      return new EBRouter(vertx, serviceAddress, serviceClass);
    }

    public EBRouter route(String path, Handler<RoutingContext> requestHandler) {
      router.route(path).handler(requestHandler);
      return this;
    }

    public EBRouter get(String path, Handler<RoutingContext> requestHandler) {
      router.get(path).handler(requestHandler);
      return this;
    }

    public EBRouter post(String path, Handler<RoutingContext> requestHandler) {
      router.post(path).handler(requestHandler);
      return this;
    }

    public Router start() {
      router.route().last().handler(this::mapRequestToEventbus);
      return router;
    }

    private void mapRequestToEventbus(RoutingContext ctx) {
      String[] split = ctx.request().uri().split("/");
      Optional<String> action = mapToServiceAction(split[1]);
      if (!action.isPresent()) {
        Status.notFound(ctx);
        return;
      }
      // TODO: 24.09.2017 merge request params into body json
      DeliveryOptions options = new DeliveryOptions().addHeader("action", action.get());
      ctx.request().bodyHandler(body -> vertx.eventBus()
          .<JsonObject>rxSend(address, body.length() > 0 ? body.toJsonObject() : new JsonObject(), options)
          .doOnError(err -> Status.serviceUnavailable(ctx, err))
          .subscribe(msg -> Status.ok(ctx, msg.body())));
    }

    private Optional<String> mapToServiceAction(String reqAction) {
      return serviceMethods.stream()
                           .filter(s -> s.toLowerCase(Locale.ENGLISH).equals(reqAction))
                           .findAny();
    }

    private Set<String> findServiceMethods(Class clazz) {
      return Stream.of(clazz.getMethods())
                   .filter(m -> isLastParameterOfHandlerType(m.getParameterTypes()))
                   .filter(m -> m.getReturnType().equals(clazz))
                   .map(Method::getName)
                   .collect(Collectors.toSet());
    }

    private boolean isLastParameterOfHandlerType(Class<?>[] types) {
      return types.length != 0 && Handler.class.equals(types[types.length - 1]);
    }
  }
}
