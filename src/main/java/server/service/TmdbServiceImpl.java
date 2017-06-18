package server.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;
import io.vertx.rxjava.ext.web.codec.BodyCodec;
import server.entity.CacheItem;
import server.entity.Retryable;

import static java.util.concurrent.TimeUnit.SECONDS;
import static server.entity.Status.OK;
import static server.entity.Status.RATE_LIMIT;
import static server.service.TmdbServiceImpl.Cache.*;

/**
 * TheMovieDatabase service implementation.
 */
public class TmdbServiceImpl extends CachingService<JsonObject> implements TmdbService {
  private static final Logger LOG = LoggerFactory.getLogger(TmdbServiceImpl.class);
  private static final int HTTPS = 443;
  private static final long DEFAULT_DELAY = SECONDS.toMillis(1);
  private static final String ENDPOINT = "api.themoviedb.org";
  private static final String RATE_RESET_HEADER = "X-RateLimit-Reset";
  private static final String APIKEY_PREFIX1 = "&api_key="; // for search
  private static final String APIKEY_PREFIX2 = "?api_key="; // with specific id
  private static final String APPEND_TO_RESPONSE = "&append_to_response=";
  private static final String APIKEY = "tmdb_key";

  private static final String MOVIE_NAME = "/3/search/movie?query=";
  private static final String MOVIE_ID = "/3/movie/";
  private static final String RECOMMENDATIONS = "/recommendations";
  private static final String SIMILAR = "/similar";

  private static final String TV_NAME = "/3/search/tv?query=";
  private static final String TV_ID = "/3/tv/";
  private static final String TV_SEASON = "/season/";

  private final Vertx vertx;
  private final JsonObject config;
  private final WebClient client;
  private final DatabaseService database;

  protected TmdbServiceImpl(Vertx vertx, JsonObject config, DatabaseService database) {
    super();
    this.vertx = vertx;
    this.config = config;
    this.database = database;
    this.client = new WebClient(io.vertx.ext.web.client.WebClient.create(vertx, new WebClientOptions()
        .setSsl(true)
        .setKeepAlive(false)));
  }

  @Override
  public TmdbService getMovieByName(String name, Handler<AsyncResult<JsonObject>> handler) { //pärib tmdb-st otsingu
    String uri = MOVIE_NAME + name.replaceAll(" ", "-") + APIKEY_PREFIX1;
    get(uri, getCached(SEARCH.get(name)), "").setHandler(handler);
    return this;
  }

  @Override
  public TmdbService getMovieById(String id, Handler<AsyncResult<JsonObject>> handler) { //pärib tmdb-st filmi
    String uri = MOVIE_ID + id + APIKEY_PREFIX2;
    Future.<JsonObject>future(fut -> get(uri, getCached(MOVIE.get(id)), "").setHandler(ar -> {
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
    })).setHandler(handler);
    return this;
  }

  @Override
  public TmdbService getTVByName(String name, Handler<AsyncResult<JsonObject>> handler) {
    String uri = TV_NAME + name.replaceAll(" ", "-") + APIKEY_PREFIX1;
    get(uri, getCached(TV_SEARCH.get(name)), "").setHandler(handler);
    return this;
  }

  @Override
  public TmdbService getTVById(String param, Handler<AsyncResult<JsonObject>> handler) {
    JsonObject jsonParam = new JsonObject(param);
    String id = Integer.toString(jsonParam.getInteger("seriesId"));
    int page = jsonParam.getInteger("page");
    StringBuilder seasons = new StringBuilder(APPEND_TO_RESPONSE);
    for (int i = ((page - 1) * 10); i < page * 10; i++) {
      if (i != page * 10 - 1) {
        seasons.append("season/").append(i).append(",");
      } else {
        seasons.append("season/").append(i);
      }
    }
    Future<JsonObject> fut = Future.future();
    fut.setHandler(handler);
    get(TV_ID + id + APIKEY_PREFIX2, getCached(TV.get(id + "_" + page)), seasons.toString()).setHandler(ar -> {
      if (ar.succeeded()) {
        JsonObject json = ar.result();
        //System.out.println(json.encodePrettily());
        fut.complete(json);
        database.insertSeries(json.getInteger("id"), json.getString("name"),
            json.getString("poster_path") == null ? "" : json.getString("poster_path"));
      }
    });
    return this;
  }

  @Override
  public TmdbService getTvSeason(String param, Handler<AsyncResult<JsonObject>> handler) {
    JsonObject jsonParam = new JsonObject(param);
    String seriesId = jsonParam.getString("seriesId");
    String seasonNr = jsonParam.getString("seasonNr");
    String uri = TV_ID + seriesId + TV_SEASON + seasonNr + APIKEY_PREFIX2;
    get(uri, getCached(SEASON.get(seriesId + "_" + seasonNr)), "").setHandler(handler);
    return this;
  }

  @Override
  public TmdbService insertSeasonViews(String username, String jsonParam, Handler<AsyncResult<JsonObject>> handler) {
    Future.<JsonObject>future(fut -> getTvSeason(jsonParam, ar -> {
      if (ar.succeeded()) {
        JsonObject result = ar.result();
        database.insertSeasonViews(username, result, new JsonObject(jsonParam).getString("seriesId"))
            .setHandler(ar2 -> {
              if (ar2.succeeded()) {
                fut.complete(ar2.result());
              }
            });
      }
    })).setHandler(handler);
    return this;
  }

  @Override
  public TmdbService getMovieRecommendation(String id, Handler<AsyncResult<JsonObject>> handler) {
    String uri = MOVIE_ID + id + RECOMMENDATIONS + APIKEY_PREFIX2;
    get(uri, getCached(Cache.RECOMMENDATIONS.get(id)), "").setHandler(handler);
    return this;
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
    SEARCH("search"),
    MOVIE("movie"),
    TV("tv"),
    TV_EPISODE("tv_episode"),
    TV_SEARCH("tv_search"),
    RECOMMENDATIONS("movie_recommended"),
    SEASON("season");

    private final String prefix;

    Cache(String prefix) {
      this.prefix = prefix;
    }

    public String get(String id) {
      return prefix + "_" + id;
    }
  }

  private long getTimeTillReset(HttpResponse res) {
    return Long.parseLong(res.getHeader(RATE_RESET_HEADER)) - System.currentTimeMillis() + 500L;
  }
}