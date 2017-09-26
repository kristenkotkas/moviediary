package common.verticle.rx;

import common.entity.JsonObj;
import common.util.Status;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.MultiMap;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.CookieHandler;
import io.vertx.rxjava.ext.web.handler.SessionHandler;
import io.vertx.rxjava.ext.web.sstore.LocalSessionStore;
import lombok.extern.slf4j.Slf4j;
import rx.Single;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static common.util.ConditionUtils.chain;
import static common.util.ConditionUtils.ifTrue;
import static common.util.NetworkUtils.getRandomUnboundPort;
import static common.util.Status.UNAUTHORIZED;
import static common.util.rx.RxUtils.toSubscriber;
import static common.verticle.rx.RestApiRxVerticle.RestRouter.create;
import static common.verticle.rx.RestApiRxVerticle.RestRouter.createMappingToEventbus;

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
  protected final void startEBRouter(String address, Class clazz, Future<Void> fut, Consumer<RestRouter>... consumers) {
    chain(config().getInteger("http.port", getRandomUnboundPort()), port ->
        createHttpServer(chain(chain(createMappingToEventbus(vertx, address, clazz), consumers).build()), port)
            .flatMap(v -> publishEventBusService(clazz))
            .flatMap(v -> publishHttpEndpoint(port))
            .subscribe(toSubscriber(fut.completer())));

  }

  @SafeVarargs
  protected final void startRestRouter(Future<Void> future, Consumer<RestRouter>... consumers) {
    chain(config().getInteger("http.port", getRandomUnboundPort()), port ->
        createHttpServer(chain(create(vertx), consumers).build(), port)
            .flatMap(v -> publishHttpEndpoint(port))
            .subscribe(toSubscriber(future.completer())));
  }

  protected static class RestRouter {
    private final Router router;
    private final Vertx vertx;
    private final String address;
    private final Set<String> serviceMethods = new HashSet<>();
    private final boolean mapRequestsToEventbus;

    private RestRouter(Vertx vertx, String serviceAddress, Class serviceClass, boolean mapRequestsToEventbus) {
      this.router = Router.router(vertx).exceptionHandler(thr -> log.error("EventbusRouter error", thr));
      this.vertx = vertx;
      this.address = serviceAddress;
      this.mapRequestsToEventbus = mapRequestsToEventbus;
      ifTrue(mapRequestsToEventbus, () -> serviceMethods.addAll(findServiceMethods(serviceClass)));
    }

    public static RestRouter create(Vertx vertx) {
      return new RestRouter(vertx, null, null, false);
    }

    public static RestRouter createMappingToEventbus(Vertx vertx, String serviceAddress, Class serviceClass) {
      return new RestRouter(vertx, serviceAddress, serviceClass, true);
    }

    public RestRouter route(String path, Handler<RoutingContext> requestHandler) {
      router.route(path).handler(requestHandler);
      return this;
    }

    public RestRouter get(String path, Handler<RoutingContext> requestHandler) {
      router.get(path).handler(requestHandler);
      return this;
    }

    public RestRouter post(String path, Handler<RoutingContext> requestHandler) {
      router.post(path).handler(requestHandler);
      return this;
    }

    public Router build() {
      ifTrue(mapRequestsToEventbus, () -> router.route().last().handler(this::mapRequestToEventbus));
      return router;
    }

    private void mapRequestToEventbus(RoutingContext ctx) {
      Optional<String> action = mapToServiceAction(ctx.request().path());
      if (!action.isPresent()) {
        Status.notFound(ctx);
        return;
      }
      ctx.request().bodyHandler(body -> vertx.eventBus()
          .<JsonObject>rxSend(address, mergeParamsToBody(body, ctx.request().params()),
              new DeliveryOptions().addHeader("action", action.get()))
          .doOnError(err -> Status.serviceUnavailable(ctx, err))
          .subscribe(msg -> Status.ok(ctx, msg.body())));
    }

    private Optional<String> mapToServiceAction(String path) {
      String[] split = path.split("/");
      if (split.length <= 1) {
        return Optional.empty();
      }
      return serviceMethods.stream()
                           .filter(s -> s.toLowerCase(Locale.ENGLISH).equals(split[1]))
                           .findAny();
    }

    private JsonObject mergeParamsToBody(Buffer body, MultiMap params) {
      JsonObj json = body.length() > 0 ? JsonObj.fromParent(body.toJsonObject()) : new JsonObj();
      params.getDelegate().entries().forEach(e -> json.putParse(e.getKey(), e.getValue()));
      return json;
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
