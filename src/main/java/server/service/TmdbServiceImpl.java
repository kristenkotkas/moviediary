package server.service;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;
import io.vertx.rxjava.ext.web.codec.BodyCodec;
import server.entity.Retryable;

import java.util.concurrent.TimeUnit;

import static io.vertx.rxjava.core.Future.future;
import static server.entity.Status.OK;
import static server.entity.Status.RATE_LIMIT;
import static server.service.TmdbServiceImpl.Cache.*;

/**
 * TheMovieDatabase service implementation.
 */
public class TmdbServiceImpl extends CachingServiceImpl<JsonObject> implements TmdbService {
    private static final Logger LOG = LoggerFactory.getLogger(TmdbServiceImpl.class);
    private static final int HTTPS = 443;
    private static final long DEFAULT_DELAY = TimeUnit.SECONDS.toMillis(1);
    private static final String ENDPOINT = "api.themoviedb.org";
    private static final String RATE_RESET_HEADER = "X-RateLimit-Reset";
    private static final String APIKEY_PREFIX1 = "&api_key="; // for search
    private static final String APIKEY_PREFIX2 = "?api_key="; // with specific id
    private static final String APPEND_TO_RESPONSE = "&append_to_response=";
    private static final String APIKEY = "tmdb_key";

    private static final String MOVIE_NAME = "/3/search/movie?query=";
    private static final String MOVIE_ID = "/3/movie/";

    private static final String TV_NAME = "/3/search/tv?query=";
    private static final String TV_ID = "/3/tv/";

    private final Vertx vertx;
    private final JsonObject config;
    private final WebClient client;
    private final DatabaseService database;

    protected TmdbServiceImpl(Vertx vertx, JsonObject config, DatabaseService database) {
        super(CachingServiceImpl.DEFAULT_MAX_CACHE_SIZE);
        this.vertx = vertx;
        this.config = config;
        this.database = database;
        this.client = WebClient.create(vertx, new WebClientOptions().setSsl(true).setKeepAlive(false));
    }

    @Override
    public Future<JsonObject> getMovieByName(String name) { //pärib tmdb-st otsingu
        return get(MOVIE_NAME + name.replaceAll(" ", "-") + APIKEY_PREFIX1,
                getCached(SEARCH.get(name)), "");
    }

    @Override
    public Future<JsonObject> getMovieById(String id) { //pärib tmdb-st filmi
        return future(fut -> get(MOVIE_ID + id + APIKEY_PREFIX2, getCached(MOVIE.get(id)), "").setHandler(ar -> {
            if (ar.succeeded()) {
                JsonObject json = ar.result();
                fut.complete(json);
                if (!json.getString("release_date").equals("")) {
                    database.insertMovie(json.getInteger("id"), json.getString("title"),
                            Integer.parseInt(json.getString("release_date").split("-")[0]), // TODO: 18.04.2017 release date null check
                            json.getString("poster_path") == null ? "" : json.getString("poster_path"));
                }
            } else {
                LOG.error("TMDB getMovieByID failed, could not add movie to DB: " + ar.cause());
            }
        }));
    }

    @Override
    public Future<JsonObject> getTVByName(String name) {
        return get(TV_NAME + name.replaceAll(" ", "-") + APIKEY_PREFIX1, getCached(TV_SEARCH.get(name)),"");
    }

    @Override
    public Future<JsonObject> getTVById(String id, int page) {
        return future(fut -> get(TV_ID + id + APIKEY_PREFIX2, getCached(TV.get(id)),"").setHandler(ar -> {
            if (ar.succeeded()) {
                JsonObject json = ar.result();
                future(fut2 -> getTVEpisodes(id, json.getInteger("number_of_seasons"), page).setHandler(ar2 -> {
                    if (ar2.succeeded()) {
                        JsonObject json2 = ar2.result();
                        fut.complete(json2);
                        database.insertSeries(json2.getInteger("id"), json2.getString("name"),
                                json.getString("poster_path") == null ? "" : json.getString("poster_path"));
                    }
                }));
            }
        }));
    }

    private Future<JsonObject> getTVEpisodes(String id, int seasonCount, int page) {
        StringBuilder seasons = new StringBuilder(APPEND_TO_RESPONSE);
        for (int i = ((page - 1) * 10); i <= seasonCount && i <= page * 10; i++) {
            if (i != seasonCount) {
                seasons.append("season/").append(i).append(",");
            } else {
                seasons.append("season/").append(i);
            }
        }
        System.out.println("PAGE: " + page);
        System.out.println("QUERY: " + seasons);
        return get(TV_ID + id + APIKEY_PREFIX2, getCached(TV_EPISODE.get(id)), seasons.toString());
    }

    private Future<JsonObject> get(String uri, CacheItem<JsonObject> cacheItem, String appendToResponse) {
        System.out.println(uri + config.getString(APIKEY) + appendToResponse);
        return cacheItem.get(true,
                (fut, cache) -> get(uri + config.getString(APIKEY) + appendToResponse, cache, fut, Retryable.create(5)));
    }

    private void get(String uri, CacheItem<JsonObject> cache, Future<JsonObject> future, Retryable retryable) {
        client.get(HTTPS, ENDPOINT, uri).timeout(5000L).as(BodyCodec.jsonObject()).send(ar -> {
            if (ar.succeeded()) {
                HttpResponse<JsonObject> res = ar.result();
                if (res.statusCode() == OK) {
                    future.complete(cache.set(res.body()));
                } else if (res.statusCode() == RATE_LIMIT) {
                    retryable.retry(
                            () -> vertx.setTimer(getTimeTillReset(res), timer -> get(uri, cache, future, retryable)),
                            () -> future.fail("Too many retries."));
                } else {
                    future.fail("TMDB API returned code: " + res.statusCode() +
                            "; message: " + res.statusMessage());
                }
            } else {
                retryable.retry(
                        () -> vertx.setTimer(DEFAULT_DELAY, timer -> get(uri, cache, future, retryable)),
                        () -> future.fail("Too many failures."));
            }
        });
    }

    public enum Cache {
        SEARCH("search_"),
        MOVIE("movie_"),
        TV("tv_"),
        TV_EPISODE("tv_episode_"),
        TV_SEARCH("tv_search_");

        private final String prefix;

        Cache(String prefix) {
            this.prefix = prefix;
        }

        public String get(String id) {
            return prefix + id;
        }
    }

    private long getTimeTillReset(HttpResponse res) {
        return Long.parseLong(res.getHeader(RATE_RESET_HEADER)) - System.currentTimeMillis() + 500L;
    }
}