package server.service;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;

/**
 * Service which interacts with The Open Movie Database API.
 */
public interface OmdbService extends CachingService<JsonObject> {

    static OmdbService create(Vertx vertx, JsonObject config, DatabaseService database) {
        return new OmdbServiceImpl(vertx, config, database);
    }

    Future<JsonObject> getMovieAwards(String imdbId);
}
