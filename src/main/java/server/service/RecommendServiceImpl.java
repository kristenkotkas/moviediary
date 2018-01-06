package server.service;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.client.WebClient;
import static io.vertx.rxjava.core.Future.future;

public class RecommendServiceImpl implements RecommendService {
  private static final int PORT = 9998;
  private static final String LOCALHOST = "localhost";
  private static final String URI = "/";

  private final WebClient client;

  protected RecommendServiceImpl(Vertx vertx) {
    this.client = WebClient.create(vertx, new WebClientOptions().setKeepAlive(false));
  }

  @Override
  public Future<JsonObject> getMoviePredictions(String movieId) {
    //todo store port etc in config
    return future(fut -> client.get(PORT, LOCALHOST, URI).addQueryParam("id", movieId).send(ar -> {
      if (ar.failed()) {
        fut.fail(ar.cause());
        return;
      }
      fut.complete(ar.result().bodyAsJsonObject());
    }));
  }

  @Override
  public Future<JsonObject> getGenrePredictions(JsonObject json) {
    return future(fut -> client.post(PORT, LOCALHOST, URI).sendJsonObject(json, ar -> {
      if (ar.failed()) {
        fut.fail(ar.cause());
        return;
      }
      fut.complete(ar.result().bodyAsJsonObject());
    }));
  }
}
