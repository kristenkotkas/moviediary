package server.service;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;

public interface RecommendService {

  static RecommendService create(Vertx vertx) {
    return new RecommendServiceImpl(vertx);
  }

  Future<JsonObject> getMoviePredictions(String movieId);
}
