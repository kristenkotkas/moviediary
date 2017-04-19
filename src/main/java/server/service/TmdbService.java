package server.service;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;

/**
 * Service which interacts with TheMovieDatabase API.
 */
public interface TmdbService extends CachingService<JsonObject> {

    static TmdbService create(Vertx vertx, JsonObject config, DatabaseService database) {
        return new TmdbServiceImpl(vertx, config, database);
    }

    Future<JsonObject> getMovieByName(String name);

    Future<JsonObject> getMovieById(String id);

    Future<JsonObject> getTVByName(String name);

    Future<JsonObject> getTVById(String id, int page);
}