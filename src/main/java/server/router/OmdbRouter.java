package server.router;

import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import omdb.rxjava.OmdbService;

import static server.entity.Status.badRequest;
import static util.StringUtils.parseParam;

/**
 * Contains routes that handle The Open Movie Database services.
 */
public class OmdbRouter extends EventBusRoutable {
  private static final String API_OMDB_GET_AWARDS = "/private/api/v1/omdb/awards/:imdbId";
  private static final String API_GET_AWARDS = "api_get_awards";

  private final OmdbService omdb;

  public OmdbRouter(Vertx vertx, omdb.OmdbService omdbDelegate) {
    super(vertx);
    this.omdb = new OmdbService(omdbDelegate);
    // TODO: 18/06/2017 service proxies instead of listeners
    listen(API_GET_AWARDS, reply((user, param) -> omdb.rxGetMovieAwards(param)));
  }

  @Override
  public void route(Router router) {
    router.get(API_OMDB_GET_AWARDS).handler(this::handleGetMovieAwards);
  }

  private void handleGetMovieAwards(RoutingContext ctx) {
    String imdbId = ctx.request().getParam(parseParam(API_OMDB_GET_AWARDS));
    if (imdbId == null) {
      badRequest(ctx);
      return;
    }
    omdb.rxGetMovieAwards(imdbId).subscribe(json -> ctx.response().end(json.encodePrettily()), ctx::fail);
  }
}