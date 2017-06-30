package tmdb;

import database.DatabaseService;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

/**
 * Service which interacts with TheMovieDatabase API.
 */
@VertxGen
@ProxyGen
public interface TmdbService {
  String SERVICE_NAME = "tmdb-eventbus-service";
  String SERVICE_ADDRESS = "service.tmdb";

  static TmdbService create(Vertx vertx, JsonObject config, WebClient webClient, DatabaseService database) {
    return new TmdbServiceImpl(vertx, config, webClient, database);
  }

  static TmdbService createProxy(Vertx vertx, String address) {
    return new TmdbServiceVertxEBProxy(vertx, address);
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