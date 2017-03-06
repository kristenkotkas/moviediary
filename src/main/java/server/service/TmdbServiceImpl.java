package server.service;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import server.entity.Retryable;

import java.util.concurrent.TimeUnit;

import static server.entity.Status.OK;
import static server.entity.Status.RATE_LIMIT;
import static server.service.TmdbServiceImpl.Cache.MOVIE;
import static server.service.TmdbServiceImpl.Cache.SEARCH;

/**
 * TheMovieDatabase service implementation.
 */
public class TmdbServiceImpl extends CachingServiceImpl<JsonObject> implements TmdbService {
    private static final Logger log = LoggerFactory.getLogger(TmdbServiceImpl.class);
    private static final int HTTPS = 443;
    private static final long DEFAULT_DELAY = TimeUnit.SECONDS.toMillis(1);
    private static final String ENDPOINT = "api.themoviedb.org";
    private static final String RATE_RESET_HEADER = "X-RateLimit-Reset";
    private static final String APIKEY_PREFIX1 = "&api_key=";
    private static final String APIKEY_PREFIX2 = "?api_key=";
    private static final String APIKEY = "tmdb_key";

    private static final String MOVIE_NAME = "/3/search/movie?query=";
    private static final String MOVIE_ID = "/3/movie/";

    private final Vertx vertx;
    private final JsonObject config;
    private final HttpClient client;
    private final DatabaseService database;

    protected TmdbServiceImpl(Vertx vertx, JsonObject config, DatabaseService database) {
        super(CachingServiceImpl.DEFAULT_MAX_CACHE_SIZE);
        this.vertx = vertx;
        this.config = config;
        this.database = database;
        this.client = vertx.createHttpClient(new HttpClientOptions().setSsl(true));
    }

    @Override
    public Future<JsonObject> getMovieByName(String name) { //pärib tmdb-st otsingu
        return get(MOVIE_NAME + name.replaceAll(" ", "-") + APIKEY_PREFIX1, getCached(SEARCH.get(name)));
    }

    @Override
    public Future<JsonObject> getMovieById(String id) { //pärib tmdb-st filmi
        Future<JsonObject> future = get(MOVIE_ID + id + APIKEY_PREFIX2, getCached(MOVIE.get(id)));
        // FIXME: 4. märts. 2017 filmi lisamine andmebaasi
        /*Future<JsonObject> future1 = database.insertMovie(json.getInteger("movieId"), json.getString("title"), json.getInteger("year"));
        */
        return future;
    }

    private Future<JsonObject> get(String uri, CacheItem<JsonObject> cache) {
        Future<JsonObject> future = Future.future();
        if (!tryCachedResult(true, cache, future)) {
            get(uri + config.getString(APIKEY, ""), cache, future, Retryable.create(5)); //max 5 korda uuesti proovimisi
        }
        return future;
    }

    private void get(String uri, CacheItem<JsonObject> cache, Future<JsonObject> future, Retryable retryable) {
        client.get(HTTPS, ENDPOINT, uri, res -> handleResponse(res, uri, cache, future, retryable))
                .exceptionHandler(event -> retryable.retry(
                        () -> vertx.setTimer(DEFAULT_DELAY, timer -> get(uri, cache, future, retryable)),
                        () -> future.fail("Too many failures.")))
                .end();
    }

    private void handleResponse(HttpClientResponse res, String uri, CacheItem<JsonObject> cache,
                                Future<JsonObject> future, Retryable retryable) {
        if (res.statusCode() == OK) {
            res.bodyHandler(body -> future.complete(cache.set(body.toJsonObject())));
        } else if (res.statusCode() == RATE_LIMIT) { // TODO: 2.03.2017 test
            retryable.retry(
                    () -> vertx.setTimer(getTimeTillReset(res), timer -> get(uri, cache, future, retryable)),
                    () -> future.fail("Too many retries."));
        } else {
            future.fail("API returned code: " + res.statusCode() +
                    "; message: " + res.statusMessage());
        }
    }

    public enum Cache {
        SEARCH("search_"),
        MOVIE("movie_");

        private final String prefix;

        Cache(String prefix) {
            this.prefix = prefix;
        }

        public String get(String id) {
            return prefix + id;
        }
    }

    private long getTimeTillReset(HttpClientResponse res) {
        return Long.parseLong(res.getHeader(RATE_RESET_HEADER)) - System.currentTimeMillis() + 500L;
    }
}