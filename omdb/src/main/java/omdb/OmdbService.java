package omdb;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

/**
 * Service which interacts with The Open Movie Database API.
 */
@VertxGen
@ProxyGen
public interface OmdbService {
  String SERVICE_NAME = "omdb-eventbus-service";
  String SERVICE_ADDRESS = "service.omdb";

  static OmdbService create(Vertx vertx, JsonObject config, WebClient webClient) {
    return new OmdbServiceImpl(vertx, config, webClient);
  }

  static OmdbService createProxy(Vertx vertx, String address) {
    return new OmdbServiceVertxEBProxy(vertx, address);
  }

  @Fluent
  OmdbService getMovieAwards(String imdbId, Handler<AsyncResult<JsonObject>> handler);
}
