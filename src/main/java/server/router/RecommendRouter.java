package server.router;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import server.service.RecommendService;
import server.util.CommonUtils;
import static server.entity.Status.OK;
import static server.entity.Status.badRequest;
import static server.util.HandlerUtils.parseParam;

public class RecommendRouter implements Routable {
  private static final String API_RECOMMEND = "/public/api/v1/recommend/:movieId";
  private static final String API_GENRES = "/public/api/v1/genres";

  private final RecommendService recommendService;

  public RecommendRouter(RecommendService recommendService) {
    this.recommendService = recommendService;
  }

  @Override
  public void route(Router router) {
    router.get(API_RECOMMEND).handler(this::handleGetRecommendations);
    router.post(API_GENRES).handler(this::handlePostGenres);
  }

  private void allowCorsForLocalDevelopment(RoutingContext ctx) {
    String host = ctx.request().remoteAddress().host();
    CommonUtils.ifTrue(host.equals("127.0.0.1"),
        () -> ctx.response().putHeader("Access-Control-Allow-Origin", "http://localhost:8080"));
  }

  private void handleGetRecommendations(RoutingContext ctx) {
    allowCorsForLocalDevelopment(ctx);
    String movieId = ctx.request().getParam(parseParam(API_RECOMMEND));
    if (movieId == null) {
      badRequest(ctx);
      return;
    }
    recommendService.getMoviePredictions(movieId)
        .rxSetHandler()
        .subscribe(response -> ctx.response().setStatusCode(OK).end(response.encodePrettily()), ctx::fail);
  }

  private void handlePostGenres(RoutingContext ctx) {
    allowCorsForLocalDevelopment(ctx);
    JsonObject body = ctx.getBodyAsJson();
    if (body == null) {
      badRequest(ctx);
      return;
    }
    recommendService.getGenrePredictions(body)
        .rxSetHandler()
        .subscribe(response -> ctx.response().setStatusCode(OK).end(response.encodePrettily()), ctx::fail);
  }
}