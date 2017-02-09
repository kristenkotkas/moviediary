package server.service;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public interface TmdbService extends CachingService<JsonObject> {

    static TmdbService create(Vertx vertx, JsonObject config) {
        return new TmdbServiceImpl(vertx, config);
    }

    Future<JsonObject> getMovie(String name);
}