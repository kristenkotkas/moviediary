package server.router;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import server.service.rxjava.TmdbService;

import static server.entity.Status.badRequest;
import static server.util.HandlerUtils.parseParam;

/**
 * Contains routes that handle TheMovieDatabase services.
 */
public class TmdbRouter extends EventBusRoutable {
  private static final String API_TMDB_GET_SEARCH = "/private/api/v1/search/:movieName";
  private static final String API_TMDB_GET_MOVIE = "/private/api/v1/movie/:movieId";

  private static final String API_TMDB_GET_TV_SEARCH = "/private/api/v1/search_tv/:tvName";
  private static final String API_TMDB_GET_TV = "/private/api/v1/tv/:tvId";

  public static final String API_GET_SEARCH = "api_get_search";
  public static final String API_GET_MOVIE = "api_get_movie";

  public static final String API_GET_TV_SEARCH = "api_get_tv_search";
  public static final String API_GET_TV = "api_get_tv";

  public static final String API_GET_RECOMMENDATIONS = "api_get_recommendations";
  private static final String INSERT_SEASON_VIEWS = "database_insert_season_views";

  private final TmdbService tmdb;

  public TmdbRouter(Vertx vertx, server.service.TmdbService tmdb) {
    super(vertx);
    this.tmdb = new TmdbService(tmdb);
    // TODO: 18/06/2017 service proxies instead of listeners
    listen(API_GET_SEARCH, reply(name -> Future.<JsonObject>future(fut -> tmdb.getMovieByName(name, fut.completer()))));
    listen(API_GET_MOVIE, reply(id -> Future.<JsonObject>future(fut -> tmdb.getMovieById(id, fut.completer()))));
    listen(API_GET_TV_SEARCH, reply(name -> Future.<JsonObject>future(fut -> tmdb.getTVByName(name, fut.completer()))));
    listen(API_GET_TV, reply(id -> Future.<JsonObject>future(fut -> tmdb.getTVById(id, fut.completer()))));
    listen(API_GET_RECOMMENDATIONS, reply(
        id -> Future.<JsonObject>future(fut -> tmdb.getMovieRecommendation(id, fut.completer()))));
    listen(INSERT_SEASON_VIEWS, reply(
        (user, json) -> Future.<JsonObject>future(fut -> tmdb.insertSeasonViews(user, json, fut.completer()))));
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
    tmdb.rxGetMovieByName(name).subscribe(json -> ctx.response().end(json.encodePrettily()), ctx::fail);
  }

  private void handleApiGetMovie(RoutingContext ctx) {
    String id = ctx.request().getParam(parseParam(API_TMDB_GET_MOVIE));
    if (id == null) {
      badRequest(ctx);
      return;
    }
    tmdb.rxGetMovieById(id).subscribe(json -> ctx.response().end(json.encodePrettily()), ctx::fail);
  }

  private void handleApiGetTVSearch(RoutingContext ctx) {
    String name = ctx.request().getParam(parseParam(API_TMDB_GET_TV_SEARCH));
    if (name == null) {
      badRequest(ctx);
      return;
    }
    tmdb.rxGetTVByName(name).subscribe(json -> ctx.response().end(json.encodePrettily()), ctx::fail);
  }

  private void handleApiGetTV(RoutingContext ctx) {
    String param = ctx.request().getParam(parseParam(API_TMDB_GET_TV));
    if (param == null) {
      badRequest(ctx);
      return;
    }
    tmdb.rxGetTVById(param).subscribe(json -> ctx.response().end(json.encodePrettily()), ctx::fail);
  }
}