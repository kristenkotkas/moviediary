package tmdb;

import database.DatabaseService;
import entity.CacheItem;
import entity.CachingService;
import entity.Retryable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;
import util.RxUtils;

import static entity.Status.OK;
import static entity.Status.RATE_LIMIT;
import static io.vertx.rx.java.RxHelper.toSubscriber;
import static io.vertx.rxjava.ext.web.codec.BodyCodec.jsonObject;
import static java.util.concurrent.TimeUnit.SECONDS;
import static tmdb.TmdbServiceImpl.Cache.*;

/**
 * TheMovieDatabase service implementation.
 */
class TmdbServiceImpl extends CachingService<JsonObject> implements TmdbService {
  private static final Logger LOG = LoggerFactory.getLogger(TmdbServiceImpl.class);
  private static final int HTTPS = 443;
  private static final long DEFAULT_DELAY = SECONDS.toMillis(1);
  private static final String ENDPOINT = "api.themoviedb.org";
  private static final String RATE_RESET_HEADER = "X-RateLimit-Reset";
  private static final String APIKEY = "tmdb_key";

  private static final String MOVIE_NAME = "/3/search/movie?query=";
  private static final String MOVIE_ID = "/3/movie/";
  private static final String SIMILAR = "/similar";

  private static final String TV_NAME = "/3/search/tv?query=";
  private static final String TV_ID = "/3/tv/";
  private static final String TV_SEASON = "/season/";

  private final Vertx vertx;
  private final JsonObject config;
  private final WebClient client;
  private final database.rxjava.DatabaseService database;

  protected TmdbServiceImpl(Vertx vertx, JsonObject config,
                            io.vertx.ext.web.client.WebClient webClient, DatabaseService database) {
    super();
    this.vertx = vertx;
    this.config = config;
    this.database = new database.rxjava.DatabaseService(database);
    this.client = new WebClient(webClient);
  }

  @Override
  public TmdbService getMovieByName(String name, Handler<AsyncResult<JsonObject>> handler) { //pärib tmdb-st otsingu
    String uri = MOVIE_NAME + name.replaceAll(" ", "-");
    get(uri, getCached(SEARCH.get(name))).setHandler(handler);
    return this;
  }

  @Override
  public TmdbService getMovieById(String id, Handler<AsyncResult<JsonObject>> handler) { //pärib tmdb-st filmi
    String uri = MOVIE_ID + id;
    Future.<JsonObject>future(fut -> get(uri, getCached(MOVIE.get(id))).setHandler(ar -> {
      if (ar.succeeded()) {
        JsonObject json = ar.result();
        fut.complete(json);
        if (!json.getString("release_date").equals("")) {
          // TODO: 19.06.2017 correct?
          int year = Integer.parseInt(json.getString("release_date").split("-")[0]);
          String poster = json.getString("poster_path") == null ? "" : json.getString("poster_path");
          // TODO: 18.04.2017 release date null check
          database.rxInsertMovie(json.getInteger("id"), json.getString("title"), year, poster).subscribe();
        }
      } else {
        LOG.error("TMDB getMovieByID failed, could not add movie to DB: " + ar.cause());
      }
    })).setHandler(handler);
    return this;
  }

  @Override
  public TmdbService getTVByName(String name, Handler<AsyncResult<JsonObject>> handler) {
    String uri = TV_NAME + name.replaceAll(" ", "-");
    get(uri, getCached(TV_SEARCH.get(name))).setHandler(handler);
    return this;
  }

  @Override
  public TmdbService getTVById(String param, Handler<AsyncResult<JsonObject>> handler) {
    JsonObject jsonParam = new JsonObject(param);
    String id = Integer.toString(jsonParam.getInteger("seriesId"));
    int page = jsonParam.getInteger("page");
    StringBuilder sb = new StringBuilder(TV_ID + id + "?append_to_response=");
    for (int i = (page - 1) * 10; i < page * 10; i++) {
      if (i != page * 10 - 1) {
        sb.append("season/").append(i).append(",");
      } else {
        sb.append("season/").append(i);
      }
    }
    Future.<JsonObject>future(fut -> get(sb.toString(), getCached(TV.get(id + "_" + page))).setHandler(ar -> {
      if (ar.succeeded()) {
        JsonObject json = ar.result();
        fut.complete(json);
        String poster = json.getString("poster_path") == null ? "" : json.getString("poster_path");
        database.rxInsertSeries(json.getInteger("id"), json.getString("name"), poster).subscribe();
      } else {
        fut.fail(ar.cause());
      }
    })).setHandler(handler);
    return this;
  }

  @Override
  public TmdbService getTvSeason(String param, Handler<AsyncResult<JsonObject>> handler) {
    JsonObject jsonParam = new JsonObject(param);
    String seriesId = jsonParam.getString("seriesId");
    String seasonNr = jsonParam.getString("seasonNr");
    String uri = TV_ID + seriesId + TV_SEASON + seasonNr;
    get(uri, getCached(SEASON.get(seriesId + "_" + seasonNr))).setHandler(handler);
    return this;
  }

  @Override
  public TmdbService insertSeasonViews(String username, String jsonParam, Handler<AsyncResult<JsonObject>> handler) { // TODO: 19/06/2017 check
    String seriesId = new JsonObject(jsonParam).getString("seriesId");
    RxUtils.<JsonObject>single(h -> getTvSeason(jsonParam, h))
        .flatMap(json -> database.rxInsertSeasonViews(username, json, seriesId))
        .subscribe(toSubscriber(handler));
    return this;
  }

  @Override
  public TmdbService getMovieRecommendation(String id, Handler<AsyncResult<JsonObject>> handler) {
    get(MOVIE_ID + id + "/recommendations", getCached(Cache.RECOMMENDATIONS.get(id))).setHandler(handler);
    return this;
  }

  private Future<JsonObject> get(String uri, CacheItem<JsonObject> cacheItem) {
    return cacheItem.get(true, (fut, cache) -> get(appendApiKey(uri), cache, fut, Retryable.create(5)));
  }

  private void get(String uri, CacheItem<JsonObject> cache, Future<JsonObject> future, Retryable retryable) {
    client.get(HTTPS, ENDPOINT, uri)
        .timeout(5000L)
        .as(jsonObject())
        .send(ar -> {
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
            LOG.info("TMDB API request failed: " + ar.cause());
            retryable.retry(
                () -> vertx.setTimer(DEFAULT_DELAY, timer -> get(uri, cache, future, retryable)),
                () -> future.fail("Too many failures."));
          }
        });
  }

  private String appendApiKey(String uri) {
    return uri + (uri.contains("?") ? "&" : "?") + "api_key=" + config.getString(APIKEY);
  }

  private long getTimeTillReset(HttpResponse res) {
    return Long.parseLong(res.getHeader(RATE_RESET_HEADER)) - System.currentTimeMillis() + 500L;
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
}