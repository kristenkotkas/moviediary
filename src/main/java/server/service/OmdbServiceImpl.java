package server.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.ext.web.client.WebClient;
import server.entity.CacheItem;
import server.entity.Retryable;

import static io.vertx.rxjava.ext.web.codec.BodyCodec.jsonObject;
import static java.util.concurrent.TimeUnit.SECONDS;
import static server.entity.Status.OK;
import static server.service.OmdbServiceImpl.Cache.AWARD;

/**
 * The Open Movie Database service implementation.
 */
public class OmdbServiceImpl extends CachingService<JsonObject> implements OmdbService {
  private static final Logger LOG = LoggerFactory.getLogger(OmdbServiceImpl.class);
  private static final String APIKEY = "omdb_key";
  private static final int HTTPS = 443;
  private static final String ENDPOINT = "omdbapi.com";
  private static final long DEFAULT_DELAY = SECONDS.toMillis(1);

  private final Vertx vertx;
  private final WebClient client;
  private final String apikey;

  protected OmdbServiceImpl(Vertx vertx, JsonObject config) {
    super();
    this.vertx = vertx;
    this.apikey = "/?apikey=" + config.getString(APIKEY) + "&";
    this.client = new WebClient(io.vertx.ext.web.client.WebClient.create(vertx, new WebClientOptions()
        .setSsl(true)
        .setKeepAlive(false)));
  }

  @Override
  public OmdbService getMovieAwards(String imdbId, Handler<AsyncResult<JsonObject>> handler) {
    get("i=" + imdbId, getCached(AWARD.get(imdbId))).setHandler(handler);
    return this;
  }

  private Future<JsonObject> get(String uri, CacheItem<JsonObject> cacheItem) {
    return cacheItem.get(true, (fut, cache) -> get(uri, cache, fut, Retryable.create(5)));
  }

  private void get(String uri, CacheItem<JsonObject> cache, Future<JsonObject> future, Retryable retryable) {
    client.get(HTTPS, ENDPOINT, apikey + uri)
        .timeout(5000L)
        .as(jsonObject())
        .send(ar -> {
          if (ar.succeeded()) {
            if (ar.result().statusCode() == OK) {
              future.complete(cache.set(ar.result().body()));
            } else {
              future.fail("OMDB API returned code: " + ar.result().statusCode() +
                  "; message: " + ar.result().statusMessage());
            }
          } else {
            LOG.info("OMDB API request failed: " + ar.cause());
            retryable.retry(
                () -> vertx.setTimer(DEFAULT_DELAY, timer -> get(uri, cache, future, retryable)),
                () -> future.fail("Too many failures."));
          }
        });
  }

  public enum Cache {
    AWARD("award");

    private final String prefix;

    Cache(String prefix) {
      this.prefix = prefix;
    }

    public String get(String id) {
      return prefix + "_" + id;
    }
  }
}
