package database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.rx.java.RxHelper;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import io.vertx.rxjava.ext.sql.SQLConnection;
import rx.Single;
import util.JsonUtils;
import util.RxUtils;

import static database.Sql.*;
import static database.Utils.formToDBDate;
import static database.Utils.movieDateToDBDate;
import static io.vertx.rx.java.RxHelper.toSubscriber;
import static io.vertx.rxjava.core.Future.future;
import static java.lang.System.currentTimeMillis;
import static util.ConditionUtils.*;
import static util.StringUtils.genString;
import static util.StringUtils.hash;

/**
 * Database service implementation.
 */
class DatabaseServiceImpl implements DatabaseService {
  private final JDBCClient client;

  protected DatabaseServiceImpl(io.vertx.ext.jdbc.JDBCClient dbClient) {
    this.client = new JDBCClient(dbClient);
  }

  private Single<SQLConnection> getConnection() {
    return client.rxGetConnection()
        .flatMap(conn -> Single.just(conn).doOnUnsubscribe(conn::close));
  }

  private Future<JsonObject> query(String sql, JsonArray params) {
    return future(fut -> getConnection()
        .flatMap(conn -> conn.rxQueryWithParams(sql, params))
        .map(ResultSet::toJson)
        .subscribe(fut::complete, fut::fail));
  }

  private Future<JsonObject> query(Sql sql, JsonArray params) {
    return query(sql.get(), params);
  }

  private Future<JsonObject> updateOrInsert(String sql, JsonArray params) {
    return future(fut -> getConnection()
        .flatMap(conn -> conn.rxUpdateWithParams(sql, params))
        .map(UpdateResult::toJson)
        .subscribe(fut::complete, fut::fail));
  }

  private Future<JsonObject> updateOrInsert(Sql sql, JsonArray params) {
    return updateOrInsert(sql.get(), params);
  }

  /**
   * Inserts a Facebook, Google or IdCard user into database.
   */
  @Override
  public DatabaseService insertOAuth2User(String username, String password, String firstname, String lastname,
                                          Handler<AsyncResult<JsonObject>> handler) {
    if (!nonNull(username, password, firstname, lastname) || contains("", username, firstname, lastname)) {
      Future.<JsonObject>failedFuture("Email, firstname and lastname must exist!").setHandler(handler);
      return this;
    }
    String salt = genString();
    RxUtils.<JsonObject>single(h -> insertUserSettings(username, null, h))
        .flatMap(json -> updateOrInsert(ADD_OAUTH2_USER, new JsonArray()
            .add(username)
            .add(firstname)
            .add(lastname)
            .add(hash(password, salt))
            .add(salt)).rxSetHandler())
        .subscribe(toSubscriber(handler));
    return this;
  }

  @Override
  public DatabaseService insertFormUser(String username, String password, String firstname, String lastname,
                                        String verified, Handler<AsyncResult<JsonObject>> handler) {
    String salt = genString();
    RxUtils.<JsonObject>single(h -> insertUserSettings(username, verified, h))
        .flatMap(json -> updateOrInsert(ADD_FORM_USER, new JsonArray()
            .add(firstname)
            .add(lastname)
            .add(username)
            .add(hash(password, salt))
            .add(salt)).rxSetHandler())
        .subscribe(RxHelper.toSubscriber(handler));
    return this;
  }

  /**
   * Inserts a view into views table.
   */
  @Override
  public DatabaseService insertView(String user, String jsonParam, Handler<AsyncResult<JsonObject>> handler) {
    JsonObject json = new JsonObject(jsonParam);
    updateOrInsert(ADD_VIEW, new JsonArray()
        .add(user)
        .add(json.getString("movieId"))
        .add(movieDateToDBDate(json.getString("start")))
        .add(movieDateToDBDate(json.getString("end")))
        .add(json.getBoolean("wasFirst"))
        .add(json.getBoolean("wasCinema"))
        .add(json.getString("comment")))
        .setHandler(handler);
    return this;
  }

  /**
   * Inserts a movie to movies table.
   */
  @Override
  public DatabaseService insertMovie(int id, String movieTitle, int year, String posterPath,
                                     Handler<AsyncResult<JsonObject>> handler) {
    updateOrInsert(ADD_MOVIE, new JsonArray()
        .add(id)
        .add(movieTitle)
        .add(year)
        .add(posterPath))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService insertSeries(int id, String seriesTitle, String posterPath,
                                      Handler<AsyncResult<JsonObject>> handler) {
    updateOrInsert(ADD_SERIES, new JsonArray()
        .add(id)
        .add(seriesTitle)
        .add(posterPath))
        .setHandler(handler);
    return this;
  }

  /**
   * Inserts an entry to wishlist table.
   */
  @Override
  public DatabaseService insertWishlist(String username, int movieId, Handler<AsyncResult<JsonObject>> handler) {
    updateOrInsert(ADD_WISHLIST, new JsonArray()
        .add(username)
        .add(movieId)
        .add(currentTimeMillis()))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService insertEpisodeView(String username, String jsonParam,
                                           Handler<AsyncResult<JsonObject>> handler) {
    JsonObject json = new JsonObject(jsonParam);
    updateOrInsert(ADD_EPISODE, new JsonArray()
        .add(username)
        .add(json.getInteger("seriesId"))
        .add(json.getInteger("episodeId"))
        .add(json.getString("seasonId"))
        .add(currentTimeMillis()))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService getSeenEpisodes(String username, int seriesId, Handler<AsyncResult<JsonObject>> handler) {
    query(GET_SEEN_EPISODES, new JsonArray()
        .add(username)
        .add(seriesId))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService isInWishlist(String username, int movieId, Handler<AsyncResult<JsonObject>> handler) {
    query(WISHLIST_CONTAINS, new JsonArray()
        .add(username)
        .add(movieId))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService getWishlist(String username, Handler<AsyncResult<JsonObject>> handler) {
    query(GET_WISHLIST, new JsonArray()
        .add(username))
        .setHandler(handler);
    return this;
  }

  /**
   * Gets settings for a user.
   */
  @Override
  public DatabaseService getSettings(String username, Handler<AsyncResult<JsonObject>> handler) {
    query(GET_SETTINGS, new JsonArray()
        .add(username))
        .setHandler(handler);
    return this;
  }

  /**
   * Gets data for a single user.
   */
  @Override
  public DatabaseService getUser(String username, Handler<AsyncResult<JsonObject>> handler) {
    query(GET_USER, new JsonArray()
        .add(username))
        .setHandler(handler);
    return this;
  }

  /**
   * Gets all users.
   */
  @Override
  public DatabaseService getAllUsers(Handler<AsyncResult<JsonObject>> handler) {
    query(GET_USERS, null).setHandler(handler);
    return this;
  }

  /**
   * Gets all movies views for user.
   */
  @Override
  public DatabaseService getViews(String username, String jsonParam, Handler<AsyncResult<JsonObject>> handler) {
    JsonObject json = new JsonObject(jsonParam);
    //System.out.println(json.encodePrettily());
    json.put("start", formToDBDate(json.getString("start"), false));
    json.put("end", formToDBDate(json.getString("end"), true));
    StringBuilder sb = new StringBuilder(GET_VIEWS.get());
    ifTrue(json.getBoolean("is-first"), () -> sb.append(" AND WasFirst"));
    ifTrue(json.getBoolean("is-cinema"), () -> sb.append(" AND WasCinema"));
    query(sb.append(" ORDER BY Start DESC LIMIT ?, ?").toString(), new JsonArray()
        .add(username)
        .add(json.getString("start"))
        .add(json.getString("end"))
        .add(json.getInteger("page") * 10)
        .add(10))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService getTopMoviesStat(String username, String jsonParam,
                                          Handler<AsyncResult<JsonObject>> handler) {
    JsonObject json = new JsonObject(jsonParam);
    json.put("start", formToDBDate(json.getString("start"), false));
    json.put("end", formToDBDate(json.getString("end"), true));
    StringBuilder sb = new StringBuilder(GET_TOP_MOVIES_STATISTICS.get());
    ifTrue(json.getBoolean("is-first"), () -> sb.append(" AND WasFirst"));
    ifTrue(json.getBoolean("is-cinema"), () -> sb.append(" AND WasCinema"));
    query(sb.append(" GROUP BY MovieId ORDER BY Count DESC LIMIT 3").toString(), new JsonArray()
        .add(username)
        .add(json.getString("start"))
        .add(json.getString("end")))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService getYearsDistribution(String username, String jsonParam,
                                              Handler<AsyncResult<JsonObject>> handler) {
    JsonObject json = new JsonObject(jsonParam);
    json.put("start", formToDBDate(json.getString("start"), false));
    json.put("end", formToDBDate(json.getString("end"), true));
    StringBuilder sb = new StringBuilder(GET_YEARS_DISTRIBUTION.get());
    ifTrue(json.getBoolean("is-first"), () -> sb.append(" AND WasFirst"));
    ifTrue(json.getBoolean("is-cinema"), () -> sb.append(" AND WasCinema"));
    query(sb.append(" GROUP BY Year ORDER BY Year DESC").toString(), new JsonArray()
        .add(username)
        .add(json.getString("start"))
        .add(json.getString("end")))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService getWeekdaysDistribution(String username, String jsonParam,
                                                 Handler<AsyncResult<JsonObject>> handler) {
    JsonObject json = new JsonObject(jsonParam);
    json.put("start", formToDBDate(json.getString("start"), false));
    json.put("end", formToDBDate(json.getString("end"), true));
    StringBuilder sb = new StringBuilder(GET_WEEKDAYS_DISTRIBUTION.get());
    ifTrue(json.getBoolean("is-first"), () -> sb.append(" AND WasFirst"));
    ifTrue(json.getBoolean("is-cinema"), () -> sb.append(" AND WasCinema"));
    query(sb.append(" GROUP BY Day ORDER BY Day").toString(), new JsonArray()
        .add(username)
        .add(json.getString("start"))
        .add(json.getString("end")))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService getTimeDistribution(String username, String jsonParam,
                                             Handler<AsyncResult<JsonObject>> handler) {
    JsonObject json = new JsonObject(jsonParam);
    json.put("start", formToDBDate(json.getString("start"), false));
    json.put("end", formToDBDate(json.getString("end"), true));
    StringBuilder sb = new StringBuilder(GET_TIME_DISTRIBUTION.get());
    ifTrue(json.getBoolean("is-first"), () -> sb.append(" AND WasFirst"));
    ifTrue(json.getBoolean("is-cinema"), () -> sb.append(" AND WasCinema"));
    query(sb.append(" GROUP BY Hour").toString(), new JsonArray()
        .add(username)
        .add(json.getString("start"))
        .add(json.getString("end")))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService getMonthYearDistribution(String username, String jsonParam,
                                                  Handler<AsyncResult<JsonObject>> handler) {
    JsonObject json = new JsonObject(jsonParam);
    json.put("start", formToDBDate(json.getString("start"), false));
    json.put("end", formToDBDate(json.getString("end"), true));
    StringBuilder sb = new StringBuilder(GET_MONTH_YEAR_DISTRIBUTION.get());
    ifTrue(json.getBoolean("is-first"), () -> sb.append(" AND WasFirst"));
    ifTrue(json.getBoolean("is-cinema"), () -> sb.append(" AND WasCinema"));
    query(sb.append(" GROUP BY Month, Year ORDER BY Year, Month").toString(), new JsonArray()
        .add(username)
        .add(json.getString("start"))
        .add(json.getString("end")))
        .setHandler(handler);
    return this;
  }

  /**
   * Gets a specific movie views for user.
   */
  @Override
  public DatabaseService getMovieViews(String username, String movieId, Handler<AsyncResult<JsonObject>> handler) {
    query(GET_MOVIE_VIEWS, new JsonArray()
        .add(username)
        .add(movieId))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService getAllTimeMeta(String username, String jsonParam, Handler<AsyncResult<JsonObject>> handler) {
    JsonObject json = new JsonObject(jsonParam);
    StringBuilder sb = new StringBuilder(GET_ALL_TIME_META.get());
    ifTrue(json.getBoolean("is-first"), () -> sb.append(" AND WasFirst"));
    ifTrue(json.getBoolean("is-cinema"), () -> sb.append(" AND WasCinema"));
    query(sb.toString(), new JsonArray()
        .add(username))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService getViewsMeta(String username, String jsonParam, Handler<AsyncResult<JsonObject>> handler) {
    JsonObject json = new JsonObject(jsonParam);
    System.out.println(json.encodePrettily());
    json.put("start", formToDBDate(json.getString("start"), false));
    json.put("end", formToDBDate(json.getString("end"), true));
    StringBuilder sb = new StringBuilder(GET_VIEWS_META.get());
    ifTrue(json.getBoolean("is-first"), () -> sb.append(" AND WasFirst"));
    ifTrue(json.getBoolean("is-cinema"), () -> sb.append(" AND WasCinema"));
    System.out.println("QUERY");
    System.out.println(sb.toString());
    query(sb.toString(), new JsonArray()
        .add(username)
        .add(json.getString("start"))
        .add(json.getString("end")))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService removeView(String username, String id, Handler<AsyncResult<JsonObject>> handler) {
    updateOrInsert(DELETE_VIEW, new JsonArray()
        .add(username)
        .add(id))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService removeEpisode(String username, String episodeId, Handler<AsyncResult<JsonObject>> handler) {
    updateOrInsert(DELETE_EPISODE, new JsonArray()
        .add(username)
        .add(episodeId))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService getWatchingSeries(String username, Handler<AsyncResult<JsonObject>> handler) {
    query(GET_WATCHING_SERIES, new JsonArray()
        .add(username))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService removeFromWishlist(String username, String movieId, Handler<AsyncResult<JsonObject>> handler) {
    updateOrInsert(DELETE_WISHLIST, new JsonArray()
        .add(username)
        .add(movieId))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService getLastMoviesHome(String username, Handler<AsyncResult<JsonObject>> handler) {
    query(GET_LAST_VIEWS, new JsonArray()
        .add(username))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService getLastWishlistHome(String username, Handler<AsyncResult<JsonObject>> handler) {
    query(GET_HOME_WISHLIST, new JsonArray()
        .add(username))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService getTopMoviesHome(String username, Handler<AsyncResult<JsonObject>> handler) {
    query(GET_TOP_MOVIES, new JsonArray()
        .add(username))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService getTotalMovieCount(String username, Handler<AsyncResult<JsonObject>> handler) {
    query(GET_TOTAL_MOVIE_COUNT, new JsonArray()
        .add(username))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService getNewMovieCount(String username, Handler<AsyncResult<JsonObject>> handler) {
    query(GET_NEW_MOVIE_COUNT, new JsonArray()
        .add(username))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService getTotalRuntime(String username, Handler<AsyncResult<JsonObject>> handler) {
    query(GET_TOTAL_RUNTIME, new JsonArray()
        .add(username))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService getTotalDistinctMoviesCount(String username, Handler<AsyncResult<JsonObject>> handler) {
    query(GET_TOTAL_DISTINCT_MOVIES, new JsonArray()
        .add(username))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService getTotalCinemaCount(String username, Handler<AsyncResult<JsonObject>> handler) {
    query(GET_TOTAL_CINEMA_COUNT, new JsonArray()
        .add(username))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService insertSeasonViews(String username, JsonObject seasonData, String seriesId,
                                           Handler<AsyncResult<JsonObject>> handler) { // TODO: 18/05/2017 test
    StringBuilder query = new StringBuilder(ADD_SEASON.get());
    JsonArray episodes = seasonData.getJsonArray("episodes");
    JsonArray values = new JsonArray();
    ifFalse(episodes.isEmpty(), () -> {
      episodes.stream()
          .map(obj -> (JsonObject) obj)
          .peek(json -> query.append(" (?, ?, ?, ?, ?),"))
          .forEach(json -> values
              .add(username)
              .add(seriesId)
              .add(json.getInteger("id"))
              .add(seasonData.getString("_id"))
              .add(currentTimeMillis()));
      query.deleteCharAt(query.length() - 1);
    });
    updateOrInsert(query.toString(), values).setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService removeSeasonViews(String username, String seasonId, Handler<AsyncResult<JsonObject>> handler) {
    updateOrInsert(DELETE_SEASON, new JsonArray()
        .add(username)
        .add(seasonId))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService updateUserVerifyStatus(String username, String value,
                                                Handler<AsyncResult<JsonObject>> handler) {
    updateOrInsert(UPDATE_USER_VERIFICATION_STATUS, new JsonArray().add(value).add(username)).setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService insertUserSettings(String username, String unique, Handler<AsyncResult<JsonObject>> handler) {
    updateOrInsert(ADD_USER_SETTINGS, new JsonArray()
        .add(username)
        .add(unique == null ? "0" : unique))
        .setHandler(handler);
    return this;
  }

  /**
   * Gets users count in database.
   */
  @Override
  public DatabaseService getUsersCount(Handler<AsyncResult<JsonObject>> handler) {
    query(GET_USER_COUNT, null).rxSetHandler()
        .map(JsonUtils::getRows)
        .map(array -> array.getJsonObject(0))
        .subscribe(toSubscriber(handler));
    return this;
  }
}
