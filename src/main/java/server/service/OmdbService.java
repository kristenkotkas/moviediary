package server.service;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * Service which interacts with The Open Movie Database API.
 */
@VertxGen
@ProxyGen
public interface OmdbService {

  @GenIgnore
  static OmdbService create(Vertx vertx, JsonObject config) {
    return new OmdbServiceImpl(vertx, config);
  }

  @Fluent
  OmdbService getMovieAwards(String imdbId, Handler<AsyncResult<JsonObject>> handler);
}
