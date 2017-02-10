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

public class TmdbServiceImpl extends CachingServiceImpl<JsonObject> implements TmdbService {
    private static final Logger log = LoggerFactory.getLogger(TmdbServiceImpl.class);
    private static final int HTTPS = 443;
    private static final String ENDPOINT = "api.themoviedb.org";
    private static final String APIKEY = "tmdb_key";

    private final Vertx vertx;
    private final JsonObject config;
    private final HttpClient client;

    private String cacheName;

    protected TmdbServiceImpl(Vertx vertx, JsonObject config) {
        super(CachingServiceImpl.DEFAULT_MAX_CACHE_SIZE);
        this.vertx = vertx;
        this.config = config;
        this.client = vertx.createHttpClient(new HttpClientOptions().setSsl(true));
    }

    @Override
    public Future<JsonObject> getSearch(String name) { //pärib tmdb-st otsingu
        cacheName = "search" + name;
        return getResponse("/3/search/movie?api_key=" + config.getString(APIKEY) + "&query=" + name);
    }

    @Override
    public Future<JsonObject> getMovie(String id) { //pärib tmdb-st filmi
        cacheName = "movie_" + id;
        return getResponse("/3/movie/" + id + "?api_key=" + config.getString(APIKEY));
    }

    private Future<JsonObject> getResponse(String request) {
        Future<JsonObject> future = Future.future();
        CacheItem<JsonObject> cache = getCached(cacheName); //proovime cachei enne
        if (!tryCachedResult(true, cache, future)) {
            client.get(HTTPS, ENDPOINT, request, response -> handleResponse(response, cache, future)).end();
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
}