package server.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.rx.java.RxHelper;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import io.vertx.rxjava.ext.sql.SQLConnection;
import rx.Single;
import server.util.CommonUtils;

import static io.vertx.rx.java.RxHelper.toSubscriber;
import static io.vertx.rxjava.core.Future.future;
import static java.lang.System.currentTimeMillis;
import static server.util.CommonUtils.*;
import static server.util.StringUtils.*;

/**
 * Database service implementation.
 */
public class DatabaseServiceImpl implements DatabaseService {
  private static final String SQL_INSERT_WISHLIST =
      "INSERT IGNORE INTO Wishlist (Username, MovieId, Time) VALUES (?, ?, ?)";
  private static final String SQL_INSERT_EPISODE =
      "INSERT IGNORE INTO Series (Username, SeriesId, EpisodeId, SeasonId, Time) VALUES (?, ?, ?, ?, ?)";
  private static final String SQL_IS_IN_WISHLIST =
      "SELECT MovieId FROM Wishlist WHERE Username = ? AND MovieId = ?";
  private static final String SQL_GET_WISHLIST =
      "SELECT Title, Time, Year, Image, MovieId FROM Wishlist " +
          "JOIN Movies ON Wishlist.MovieId = Movies.Id " +
          "WHERE Username =  ? ORDER BY Time DESC";
  private static final String SQL_GET_YEARS_DIST =
      "SELECT Year, COUNT(*) AS 'Count' FROM Views " +
          "JOIN Movies ON Movies.Id = Views.MovieId " +
          "WHERE Username = ? AND Start >= ? AND Start <= ?";
  private static final String SQL_GET_WEEKDAYS_DIST =
      "SELECT ((DAYOFWEEK(Start) + 5) % 7) AS Day, COUNT(*) AS 'Count' " +
          "FROM Views " +
          "WHERE Username = ? AND Start >= ? AND Start <= ?";
  private static final String SQL_GET_TIME_DIST =
      "SELECT HOUR(Start) AS Hour, COUNT(*) AS Count FROM Views " +
          "WHERE Username = ? AND Start >= ? AND Start <= ? ";
  private static final String SQL_GET_MONTH_YEAR_DIST =
      "SELECT MONTH(Start) AS Month, YEAR(Start) AS Year, COUNT(MONTHNAME(Start)) AS Count " +
          "FROM Views " +
          "WHERE Username = ? AND Start >= ? AND Start <= ? ";
  private static final String SQL_GET_ALL_TIME_META =
      "SELECT DATE(Min(Start)) AS Start, COUNT(*) AS Count, SUM(TIMESTAMPDIFF(MINUTE, Start, End)) AS Runtime FROM Views " +
          "WHERE Username = ?";
  private static final String SQL_INSERT_OAUTH2_USER =
      "INSERT INTO Users (Username, Firstname, Lastname, Password, Salt) VALUES (?, ?, ?, ?, ?)";
  private static final String SQL_QUERY_USERS = "SELECT * FROM Users " +
      "JOIN Settings ON Users.Username = Settings.Username";
  private static final String SQL_QUERY_USER = "SELECT * FROM Users " +
      "JOIN Settings ON Users.Username = Settings.Username " +
      "WHERE Users.Username = ?";
  private static final String SQL_INSERT_VIEW =
      "INSERT INTO Views (Username, MovieId, Start, End, WasFirst, WasCinema, Comment) " +
          "VALUES (?, ?, ?, ?, ?, ?, ?)";
  private static final String SQL_QUERY_VIEWS =
      "SELECT Views.Id, MovieId, Title, Start, WasFirst, WasCinema, Image, Comment, " +
          "TIMESTAMPDIFF(MINUTE, Start, End) AS Runtime " +
          "FROM Views " +
          "JOIN Movies ON Views.MovieId = Movies.Id " +
          "WHERE Username = ? AND Start >= ? AND Start <= ?";
  private static final String SQL_GET_TOP_MOVIES_STAT =
      "SELECT MovieId, Title, COUNT(*) AS Count, Image FROM Views " +
          "JOIN Movies ON Movies.Id = Views.MovieId " +
          "WHERE Username = ? AND Start >= ? AND Start <= ?";
  private static final String SQL_QUERY_SETTINGS = "SELECT * FROM Settings WHERE Username = ?";
  private static final String SQL_INSERT_MOVIE =
      "INSERT IGNORE INTO Movies VALUES (?, ?, ?, ?)";
  private static final String SQL_INSERT_SERIES =
      "INSERT IGNORE INTO SeriesInfo VALUES (?, ?, ?)";
  private static final String SQL_USERS_COUNT = "SELECT COUNT(*) AS Count FROM Users";
  private static final String SQL_GET_MOVIE_VIEWS =
      "SELECT Id, Start, WasCinema FROM Views" +
          " WHERE Username = ? AND MovieId = ?" +
          " ORDER BY Start DESC";
  private static final String SQL_QUERY_VIEWS_META =
      "SELECT Count(*) AS Count, SUM(TIMESTAMPDIFF(MINUTE, Start, End)) AS Runtime " +
          "FROM Views " +
          "JOIN Movies ON Views.MovieId = Movies.Id " +
          "WHERE Username = ? AND Start >= ? AND Start <= ?";
  private static final String SQL_REMOVE_VIEW =
      "DELETE FROM Views WHERE Username = ? AND Id = ?";
  private static final String SQL_REMOVE_EPISODE =
      "DELETE FROM Series WHERE Username = ? AND EpisodeId = ?";
  private static final String SQL_GET_SEEN_EPISODES = "SELECT EpisodeId FROM Series " +
      "WHERE Username = ? AND SeriesId = ?";
  private static final String SQL_GET_WATCHING_SERIES =
      "SELECT Title, Image, SeriesId, COUNT(SeriesId) AS Count FROM Series " +
          "JOIN SeriesInfo ON Series.SeriesId = SeriesInfo.Id " +
          "WHERE Username = ? " +
          "GROUP BY Title, Image, SeriesId " +
          "ORDER BY Title";
  private static final String SQL_REMOVE_WISHLIST =
      "DELETE FROM Wishlist WHERE Username = ? AND MovieId = ?";
  private static final String SQL_GET_LAST_VIEWS =
      "SELECT Title, Start, MovieId, WEEKDAY(Start) AS 'week_day', WasCinema FROM Views " +
          "JOIN Movies ON Movies.Id = Views.MovieId " +
          "WHERE Username = ? " +
          "ORDER BY Start DESC LIMIT 5";
  private static final String SQL_GET_HOME_WISHLIST =
      "SELECT Title, Time, Year, MovieId FROM Wishlist " +
          "JOIN Movies ON Movies.Id = Wishlist.MovieId " +
          "WHERE Username = ? " +
          "ORDER BY Time DESC LIMIT 5";
  private static final String SQL_GET_TOP_MOVIES =
      "SELECT MovieId, Title, COUNT(*) AS Count, Image FROM Views " +
          "JOIN Movies ON Movies.Id = Views.MovieId " +
          "WHERE Username = ? " +
          "GROUP BY MovieId ORDER BY Count DESC LIMIT 5";
  private static final String SQL_GET_TOTAL_MOVIE_COUNT =
      "SELECT COUNT(*) AS 'total_movies', SUM(TIMESTAMPDIFF(MINUTE, Start, End)) AS Runtime FROM Views " +
          "WHERE Username = ?";
  private static final String SQL_GET_TOTAL_CINEMA_COUNT =
      "SELECT COUNT(*) AS 'total_cinema' FROM Views " +
          "WHERE Username = ? AND WasCinema = 1";
  private static final String SQL_GET_NEW_MOVIE_COUNT =
      "SELECT COUNT(WasFirst) AS 'new_movies' FROM Views " +
          "WHERE Username = ? " +
          "AND WasFirst = 1";
  private static final String SQL_GET_TOTAL_RUNTIME =
      "SELECT SUM(TIMESTAMPDIFF(MINUTE, Start, End)) AS Runtime FROM Views " +
          "WHERE Username = ?";
  private static final String SQL_GET_TOTAL_DISTINCT_MOVIES =
      "SELECT COUNT(DISTINCT MovieId) AS 'unique_movies' FROM Views " +
          "WHERE Username = ?";
  private static final String SQL_INSERT_SEASON =
      "INSERT IGNORE INTO Series (Username, SeriesId, EpisodeId, SeasonId, Time) VALUES";
  private static final String SQL_REMOVE_SEASON =
      "DELETE FROM Series WHERE Username = ? AND SeasonId = ?;";
  private static final String SQL_UPDATE_USER_VERIFY_STATUS = "UPDATE Settings SET Verified = ? WHERE Username = ?";
  private static final String SQL_INSERT_USER_SETTINGS = "INSERT INTO Settings (Username, Verified) VALUES (?, ?)";
  private static final String SQL_INSERT_FORM_USER = "INSERT INTO Users " +
      "(Firstname, Lastname, Username, Password, Salt) VALUES (?, ?, ?, ?, ?)";

  private final JDBCClient client;

  protected DatabaseServiceImpl(Vertx vertx, JsonObject config) {
    this.client = new JDBCClient(io.vertx.ext.jdbc.JDBCClient.createShared(vertx, config.getJsonObject("mysql")));
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

  private Future<JsonObject> updateOrInsert(String sql, JsonArray params) {
    return future(fut -> getConnection()
        .flatMap(conn -> conn.rxUpdateWithParams(sql, params))
        .map(UpdateResult::toJson)
        .subscribe(fut::complete, fut::fail));
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
    CommonUtils.<JsonObject>single(h -> insertUserSettings(username, null, h))
        .flatMap(json -> updateOrInsert(SQL_INSERT_OAUTH2_USER, new JsonArray()
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
    CommonUtils.<JsonObject>single(h -> insertUserSettings(username, verified, h))
        .flatMap(json -> updateOrInsert(SQL_INSERT_FORM_USER, new JsonArray()
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
    updateOrInsert(SQL_INSERT_VIEW, new JsonArray()
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
    updateOrInsert(SQL_INSERT_MOVIE, new JsonArray()
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
    updateOrInsert(SQL_INSERT_SERIES, new JsonArray()
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
    updateOrInsert(SQL_INSERT_WISHLIST, new JsonArray()
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
    updateOrInsert(SQL_INSERT_EPISODE, new JsonArray()
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
    query(SQL_GET_SEEN_EPISODES, new JsonArray()
        .add(username)
        .add(seriesId))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService isInWishlist(String username, int movieId, Handler<AsyncResult<JsonObject>> handler) {
    query(SQL_IS_IN_WISHLIST, new JsonArray()
        .add(username)
        .add(movieId))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService getWishlist(String username, Handler<AsyncResult<JsonObject>> handler) {
    query(SQL_GET_WISHLIST, new JsonArray()
        .add(username))
        .setHandler(handler);
    return this;
  }

  /**
   * Gets settings for a user.
   */
  @Override
  public DatabaseService getSettings(String username, Handler<AsyncResult<JsonObject>> handler) {
    query(SQL_QUERY_SETTINGS, new JsonArray()
        .add(username))
        .setHandler(handler);
    return this;
  }

  /**
   * Gets data for a single user.
   */
  @Override
  public DatabaseService getUser(String username, Handler<AsyncResult<JsonObject>> handler) {
    query(SQL_QUERY_USER, new JsonArray()
        .add(username))
        .setHandler(handler);
    return this;
  }

  /**
   * Gets all users.
   */
  @Override
  public DatabaseService getAllUsers(Handler<AsyncResult<JsonObject>> handler) {
    query(SQL_QUERY_USERS, null).setHandler(handler);
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
    StringBuilder sb = new StringBuilder(SQL_QUERY_VIEWS);
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
    StringBuilder sb = new StringBuilder(SQL_GET_TOP_MOVIES_STAT);
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
    StringBuilder sb = new StringBuilder(SQL_GET_YEARS_DIST);
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
    StringBuilder sb = new StringBuilder(SQL_GET_WEEKDAYS_DIST);
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
    StringBuilder sb = new StringBuilder(SQL_GET_TIME_DIST);
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
    StringBuilder sb = new StringBuilder(SQL_GET_MONTH_YEAR_DIST);
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
    query(SQL_GET_MOVIE_VIEWS, new JsonArray()
        .add(username)
        .add(movieId))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService getAllTimeMeta(String username, String jsonParam, Handler<AsyncResult<JsonObject>> handler) {
    JsonObject json = new JsonObject(jsonParam);
    StringBuilder sb = new StringBuilder(SQL_GET_ALL_TIME_META);
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
    StringBuilder sb = new StringBuilder(SQL_QUERY_VIEWS_META);
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
    updateOrInsert(SQL_REMOVE_VIEW, new JsonArray()
        .add(username)
        .add(id))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService removeEpisode(String username, String episodeId, Handler<AsyncResult<JsonObject>> handler) {
    updateOrInsert(SQL_REMOVE_EPISODE, new JsonArray()
        .add(username)
        .add(episodeId))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService getWatchingSeries(String username, Handler<AsyncResult<JsonObject>> handler) {
    query(SQL_GET_WATCHING_SERIES, new JsonArray()
        .add(username))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService removeFromWishlist(String username, String movieId, Handler<AsyncResult<JsonObject>> handler) {
    updateOrInsert(SQL_REMOVE_WISHLIST, new JsonArray()
        .add(username)
        .add(movieId))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService getLastMoviesHome(String username, Handler<AsyncResult<JsonObject>> handler) {
    query(SQL_GET_LAST_VIEWS, new JsonArray()
        .add(username))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService getLastWishlistHome(String username, Handler<AsyncResult<JsonObject>> handler) {
    query(SQL_GET_HOME_WISHLIST, new JsonArray()
        .add(username))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService getTopMoviesHome(String username, Handler<AsyncResult<JsonObject>> handler) {
    query(SQL_GET_TOP_MOVIES, new JsonArray()
        .add(username))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService getTotalMovieCount(String username, Handler<AsyncResult<JsonObject>> handler) {
    query(SQL_GET_TOTAL_MOVIE_COUNT, new JsonArray()
        .add(username))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService getNewMovieCount(String username, Handler<AsyncResult<JsonObject>> handler) {
    query(SQL_GET_NEW_MOVIE_COUNT, new JsonArray()
        .add(username))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService getTotalRuntime(String username, Handler<AsyncResult<JsonObject>> handler) {
    query(SQL_GET_TOTAL_RUNTIME, new JsonArray()
        .add(username))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService getTotalDistinctMoviesCount(String username, Handler<AsyncResult<JsonObject>> handler) {
    query(SQL_GET_TOTAL_DISTINCT_MOVIES, new JsonArray()
        .add(username))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService getTotalCinemaCount(String username, Handler<AsyncResult<JsonObject>> handler) {
    query(SQL_GET_TOTAL_CINEMA_COUNT, new JsonArray()
        .add(username))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService insertSeasonViews(String username, JsonObject seasonData, String seriesId,
                                           Handler<AsyncResult<JsonObject>> handler) { // TODO: 18/05/2017 test
    StringBuilder query = new StringBuilder(SQL_INSERT_SEASON);
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
    updateOrInsert(SQL_REMOVE_SEASON, new JsonArray()
        .add(username)
        .add(seasonId))
        .setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService updateUserVerifyStatus(String username, String value,
                                                Handler<AsyncResult<JsonObject>> handler) {
    updateOrInsert(SQL_UPDATE_USER_VERIFY_STATUS, new JsonArray().add(value).add(username)).setHandler(handler);
    return this;
  }

  @Override
  public DatabaseService insertUserSettings(String username, String unique, Handler<AsyncResult<JsonObject>> handler) {
    updateOrInsert(SQL_INSERT_USER_SETTINGS, new JsonArray()
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
    query(SQL_USERS_COUNT, null).rxSetHandler()
        .map(CommonUtils::getRows)
        .map(array -> array.getJsonObject(0))
        .subscribe(toSubscriber(handler));
    return this;
  }
}
