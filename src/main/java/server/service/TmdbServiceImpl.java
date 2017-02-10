package server.service;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import server.entity.Status;

import static server.service.TmdbServiceImpl.Cache.MOVIE;
import static server.service.TmdbServiceImpl.Cache.SEARCH;

public class TmdbServiceImpl extends CachingServiceImpl<JsonObject> implements TmdbService {
    private static final Logger log = LoggerFactory.getLogger(TmdbServiceImpl.class);
    private static final int HTTPS = 443;
    private static final String ENDPOINT = "api.themoviedb.org";
    private static final String APIKEY_PREFIX = "&api_key=";
    private static final String APIKEY = "tmdb_key";

    private static final String MOVIE_NAME = "/3/search/movie?query=";
    private static final String MOVIE_ID = "/3/movie/";

    private final Vertx vertx;
    private final JsonObject config;
    private final HttpClient client;

    protected TmdbServiceImpl(Vertx vertx, JsonObject config) {
        super(CachingServiceImpl.DEFAULT_MAX_CACHE_SIZE);
        this.vertx = vertx;
        this.config = config;
        this.client = vertx.createHttpClient(new HttpClientOptions().setSsl(true));
    }

    @Override
    public Future<JsonObject> getMovieByName(String name) { //pärib tmdb-st otsingu
        return get(MOVIE_NAME + name, getCached(SEARCH.get(name)));
    }

    @Override
    public Future<JsonObject> getMovieById(String id) { //pärib tmdb-st filmi
        return get(MOVIE_ID + id, getCached(MOVIE.get(id)));
    }

    private Future<JsonObject> get(String uri, CacheItem<JsonObject> cache) {
        Future<JsonObject> future = Future.future();
        if (!tryCachedResult(true, cache, future)) {
            client.get(HTTPS, ENDPOINT, uri + APIKEY_PREFIX + config.getString(APIKEY, ""),
                    response -> handleResponse(response, cache, future)).end();
        }
        return future;
    }

    private void handleResponse(HttpClientResponse response, CacheItem<JsonObject> cache, Future<JsonObject> future) {
        if (response.statusCode() == Status.OK) {
            response.bodyHandler(body -> future.complete(cache.set(body.toJsonObject())));
        } else {
            future.fail("API returned code: " + response.statusCode() +
                    "; message: " + response.statusMessage());
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
}