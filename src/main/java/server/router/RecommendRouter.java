package server.router;

import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import server.service.RecommendService;
import server.util.CommonUtils;
import static server.entity.Status.OK;
import static server.entity.Status.badRequest;
import static server.util.HandlerUtils.parseParam;

public class RecommendRouter implements Routable {
  private static final String API_RECOMMEND = "/public/api/v1/recommend/:movieId";

  private final RecommendService recommendService;

  public RecommendRouter(RecommendService recommendService) {
    this.recommendService = recommendService;
  }

  @Override
  public void route(Router router) {
    router.get(API_RECOMMEND).handler(this::handleGetRecommendations);
  }

  private void handleGetRecommendations(RoutingContext ctx) {
    String movieId = ctx.request().getParam(parseParam(API_RECOMMEND));
    if (movieId == null) {
      badRequest(ctx);
      return;
    }
    String host = ctx.request().remoteAddress().host();
    CommonUtils.ifTrue(host.equals("127.0.0.1"),
        () -> ctx.response().putHeader("Access-Control-Allow-Origin", "http://localhost:8080"));
    recommendService.getMoviePredictions(movieId)
        .rxSetHandler()
        .subscribe(response -> ctx.response()
            .setStatusCode(OK)
            .end(response.encodePrettily()), ctx::fail);
  }
}