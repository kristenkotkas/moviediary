package server.service;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.client.WebClient;
import static io.vertx.rxjava.core.Future.future;

public class RecommendServiceImpl implements RecommendService {
  private final WebClient client;

  protected RecommendServiceImpl(Vertx vertx) {
    this.client = WebClient.create(vertx, new WebClientOptions().setKeepAlive(false));
  }

  @Override
  public Future<JsonObject> getMoviePredictions(String movieId) {
    //todo store port etc in config
    return future(fut -> client.get(9998, "localhost", "/")
        .addQueryParam("id", movieId)
        .send(ar -> {
          if (ar.failed()) {
            fut.fail(ar.cause());
            return;
          }
          fut.complete(ar.result().bodyAsJsonObject());
        }));
  }
}
