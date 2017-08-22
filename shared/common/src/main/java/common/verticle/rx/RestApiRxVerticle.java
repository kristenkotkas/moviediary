package common.verticle.rx;

import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.CookieHandler;
import io.vertx.rxjava.ext.web.handler.SessionHandler;
import io.vertx.rxjava.ext.web.sstore.LocalSessionStore;
import lombok.extern.slf4j.Slf4j;
import rx.Single;
import java.util.Optional;
import java.util.function.BiConsumer;
import static common.util.Status.UNAUTHORIZED;

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
}
