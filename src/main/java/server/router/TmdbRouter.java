package server.router;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import server.service.TmdbService;

import static server.entity.Status.badRequest;
import static server.util.HandlerUtils.*;

/**
 * Contains routes that handle TheMovieDatabase services.
 */
public class TmdbRouter extends EventBusRoutable {
    private static final Logger LOG = LoggerFactory.getLogger(TmdbRouter.class);
    private static final String API_TMDB_GET_SEARCH = "/private/api/v1/search/:movieName";
    private static final String API_TMDB_GET_MOVIE = "/private/api/v1/movie/:movieId";

    private static final String API_TMDB_GET_TV_SEARCH = "/private/api/v1/search_tv/:tvName";
    private static final String API_TMDB_GET_TV = "/private/api/v1/tv/:tvId";

    public static final String API_GET_SEARCH = "api_get_search";
    public static final String API_GET_MOVIE = "api_get_movie";

    public static final String API_GET_TV_SEARCH = "api_get_tv_search";
    public static final String API_GET_TV = "api_get_tv";

    private final TmdbService tmdb;

    public TmdbRouter(Vertx vertx, TmdbService tmdb) {
        super(vertx);
        this.tmdb = tmdb;
        listen(API_GET_SEARCH, reply(tmdb::getMovieByName));
        listen(API_GET_MOVIE, reply(tmdb::getMovieById));
        listen(API_GET_TV_SEARCH, reply(tmdb::getTVByName));
        listen(API_GET_TV, reply(tmdb::getTVById));
    }

    @Override
    public void route(Router router) {
        router.get(API_TMDB_GET_SEARCH).handler(this::handleApiGetSearch);
        router.get(API_TMDB_GET_MOVIE).handler(this::handleApiGetMovie);
        router.get(API_TMDB_GET_TV_SEARCH).handler(this::handleApiGetTVSearch);
        router.get(API_TMDB_GET_TV).handler(this::handleApiGetTV);
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

    private void handleApiGetTVSearch(RoutingContext ctx) {
        String name = ctx.request().getParam(parseParam(API_TMDB_GET_TV_SEARCH));
        if (name == null) {
            badRequest(ctx);
            return;
        }
        tmdb.getTVByName(name).setHandler(resultHandler(ctx, jsonResponse(ctx)));
    }

    private void handleApiGetTV(RoutingContext ctx) {
        String param = ctx.request().getParam(parseParam(API_TMDB_GET_TV));
        if (param == null) {
            badRequest(ctx);
            return;
        }
        tmdb.getTVById(param).setHandler(resultHandler(ctx, jsonResponse(ctx))); //fixme arvestama page count
    }
}