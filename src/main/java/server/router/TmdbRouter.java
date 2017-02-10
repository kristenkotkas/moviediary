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

    private static final String API_GET_SEARCH = "/search/:movieName";
    private static final String API_GET_MOVIE = "/movie/:movieId";

    private final TmdbService tmdb;

    public TmdbRouter(Vertx vertx, TmdbService tmdb) {
        super(vertx);
        this.tmdb = tmdb;
    }

    @Override
    public void route(Router router) {
        router.get(API_GET_SEARCH).handler(this::handleApiGetSearch);
        router.get(API_GET_MOVIE).handler(this::handleApiGetMovie);
    }

    private void handleApiGetSearch(RoutingContext ctx) { // handleb otsingu json päringut /search/:filminimi -> localhost:8080/search/avatar
        String name = ctx.request().getParam(parseParam(API_GET_SEARCH));
        if (name == null) {
            badRequest(ctx);
            return;
        }
        tmdb.getMovieByName(name).setHandler(resultHandler(ctx, jsonResponse(ctx)));
    }

    private void handleApiGetMovie(RoutingContext ctx) { // handleb filmi json päringut /movie/:filminimi -> localhost:8080/movie/1234
        String id = ctx.request().getParam(parseParam(API_GET_MOVIE));
        if (id == null) {
            badRequest(ctx);
            return;
        }
        tmdb.getMovieById(id).setHandler(resultHandler(ctx, jsonResponse(ctx)));
    }

    private static String parseParam(String requestUri) {
        return requestUri.split(":")[1];
    }

    private <T> Consumer<T> jsonResponse(RoutingContext ctx) {
        return result -> ctx.response()
                .putHeader(HttpHeaders.CONTENT_TYPE, CONTENT_JSON)
                .end(Json.encodePrettily(result));
    }

    private <T> Handler<AsyncResult<T>> resultHandler(RoutingContext ctx, Consumer<T> success) {
        return ar -> {
            if (ar.succeeded()) {
                success.accept(ar.result());
            } else {
                log.error(ar.cause());
                serviceUnavailable(ctx);
            }
        };
    }
}
