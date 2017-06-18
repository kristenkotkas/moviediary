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
 * Service which interacts with TheMovieDatabase API.
 */
@VertxGen
@ProxyGen
public interface TmdbService {

  @GenIgnore
  static TmdbService create(Vertx vertx, JsonObject config, DatabaseService database) {
    return new TmdbServiceImpl(vertx, config, database);
  }

  @Fluent
  TmdbService getMovieByName(String name, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  TmdbService getMovieById(String id, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  TmdbService getTVByName(String name, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  TmdbService getTVById(String param, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  TmdbService getMovieRecommendation(String id, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  TmdbService getTvSeason(String param, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  TmdbService insertSeasonViews(String username, String jsonParam, Handler<AsyncResult<JsonObject>> handler);
}