package server.router;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import server.service.TmdbService;

import java.util.function.Consumer;

import static server.entity.Status.badRequest;
import static server.entity.Status.serviceUnavailable;

public class TmdbRouter extends Routable {
    private static final Logger log = LoggerFactory.getLogger(TmdbRouter.class);
    private static final String CONTENT_JSON = "application/json";

    private static final String API_GET = "/api/:movieName";

    private final TmdbService tmdb;

    public TmdbRouter(Vertx vertx, TmdbService tmdb) {
        super(vertx);
        this.tmdb = tmdb;
    }

    @Override
    public void route(Router router) {
        router.get(API_GET).handler(this::handleApiGet);
    }

    private void handleApiGet(RoutingContext ctx) { // handleb filmi json päringut /api/:filminimi -> localhost:8080/api/avatar
        String name = ctx.request().getParam(parseParam(API_GET));
        if (name == null) {
            badRequest(ctx);
            return;
        }
        tmdb.getMovie(name).setHandler(resultHandler(ctx, jsonResponse(ctx)));
    }

    private static String parseParam(String requestUri) {
        return requestUri.split(":")[1];
    }

    private <T> Consumer<T> jsonResponse(RoutingContext ctx) {
        return result -> ctx.response()
                .putHeader(HttpHeaders.CONTENT_TYPE, CONTENT_JSON)
                .end(Json.encodePrettily(result));
    }

    public static <T> Handler<AsyncResult<T>> resultHandler(RoutingContext ctx, Consumer<T> success) {
        return ar -> {
            if (ar.succeeded()) {
                success.accept(ar.result());
            } else {
                log.info(ar.cause());
                serviceUnavailable(ctx);
            }
        };
    }
}
