package server.router;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import server.service.TmdbService;

import static server.entity.Status.badRequest;
import static server.util.HandlerUtils.*;

/**
 * Contains routes that handle TheMovieDatabase services.
 */
public class TmdbRouter extends Routable {
    private static final Logger LOG = LoggerFactory.getLogger(TmdbRouter.class);
    private static final String API_TMDB_GET_SEARCH = "/private/api/v1/search/:movieName";
    private static final String API_TMDB_GET_MOVIE = "/private/api/v1/movie/:movieId";

    private final TmdbService tmdb;

    public TmdbRouter(Vertx vertx, TmdbService tmdb) {
        super(vertx);
        this.tmdb = tmdb;
    }

    @Override
    public void route(Router router) {
        router.get(API_TMDB_GET_SEARCH).handler(this::handleApiGetSearch);
        router.get(API_TMDB_GET_MOVIE).handler(this::handleApiGetMovie);
    }

    private void handleApiGetSearch(RoutingContext ctx) {
        String name = ctx.request().getParam(parseParam(API_TMDB_GET_SEARCH));
        if (name == null) {
            badRequest(ctx);
            return;
        }
        tmdb.getMovieByName(name).setHandler(resultHandler(ctx, jsonResponse(ctx)));
    }

    private void handleApiGetMovie(RoutingContext ctx) {
        String id = ctx.request().getParam(parseParam(API_TMDB_GET_MOVIE));
        if (id == null) {
            badRequest(ctx);
            return;
        }
        tmdb.getMovieById(id).setHandler(resultHandler(ctx, jsonResponse(ctx)));
    }
}