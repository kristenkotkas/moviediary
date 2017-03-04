package server.service;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * Service which interacts with TheMovieDatabase API.
 */
public interface TmdbService extends CachingService<JsonObject> {

    static TmdbService create(Vertx vertx, JsonObject config, DatabaseService database) {
        return new TmdbServiceImpl(vertx, config, database);
    }

    Future<JsonObject> getMovieByName(String name);

    Future<JsonObject> getMovieById(String id);
}