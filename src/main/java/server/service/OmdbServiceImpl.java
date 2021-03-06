package server.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;
import server.entity.Retryable;

import static io.vertx.rxjava.ext.web.codec.BodyCodec.jsonObject;
import static java.util.concurrent.TimeUnit.SECONDS;
import static server.entity.Status.OK;
import static server.service.OmdbServiceImpl.Cache.AWARD;
import static server.util.CommonUtils.check;

/**
 * The Open Movie Database service implementation.
 */
public class OmdbServiceImpl extends CachingServiceImpl<JsonObject> implements OmdbService {
    private static final String APIKEY = "omdb_key";
    private static final int HTTPS = 443;
    private static final String ENDPOINT = "omdbapi.com";
    private static final long DEFAULT_DELAY = SECONDS.toMillis(1);

    private final Vertx vertx;
    private final JsonObject config;
    private final WebClient client;
    private final DatabaseService database;
    private final String apikey;

    protected OmdbServiceImpl(Vertx vertx, JsonObject config, DatabaseService database) {
        super(CachingServiceImpl.DEFAULT_MAX_CACHE_SIZE);
        this.vertx = vertx;
        this.config = config;
        this.database = database;
        this.client = WebClient.create(vertx, new WebClientOptions().setSsl(true).setKeepAlive(false));
        this.apikey = "/?apikey=" + config.getString(APIKEY) + "&";
    }

    @Override
    public Future<JsonObject> getMovieAwards(String imdbId) {
        return get("i=" + imdbId, getCached(AWARD.get(imdbId)));
    }

    private Future<JsonObject> get(String uri, CacheItem<JsonObject> cacheItem) {
        return cacheItem.get(true, (fut, cache) -> get(uri, cache, fut, Retryable.create(5)));
    }

    private void get(String uri, CacheItem<JsonObject> cache, Future<JsonObject> future, Retryable retryable) {
        client.get(HTTPS, ENDPOINT, apikey + uri)
                .timeout(5000L)
                .as(jsonObject())
                .send((AsyncResult<HttpResponse<JsonObject>> ar) -> {
                    System.out.println("STATUSCODE ------------ " + ar.cause());
                    check(ar.succeeded(),
                            () -> check(ar.result().statusCode() == OK,
                                    () -> future.complete(cache.set(ar.result().body())),
                                    () -> future.fail("OMDB API returned code: " + ar.result().statusCode() +
                                            "; message: " + ar.result().statusMessage())),
                            () -> retryable.retry(
                                    () -> vertx.setTimer(DEFAULT_DELAY, timer -> get(uri, cache, future, retryable)),
                                    () -> future.fail("Too many failures.")));
                });
    }

    public enum Cache {
        AWARD("award_");

        private final String prefix;

        Cache(String prefix) {
            this.prefix = prefix;
        }

        public String get(String id) {
            return prefix + id;
        }
    }
}
