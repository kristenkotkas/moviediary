package database;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;

/**
 * Service which interacts with database.
 */
@VertxGen
@ProxyGen
public interface DatabaseService {
  String SERVICE_NAME = "database-eventbus-service";
  String SERVICE_ADDRESS = "service.database";

  static DatabaseService create(JDBCClient dbClient) {
    return new DatabaseServiceImpl(dbClient);
  }

  static DatabaseService createProxy(Vertx vertx, String address) {
    return new DatabaseServiceVertxEBProxy(vertx, address);
  }

  @Fluent
  DatabaseService insertOAuth2User(String username, String password, String firstname, String lastname,
                                   Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService insertFormUser(String username, String password, String firstname, String lastname, String verified,
                                 Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService insertMovie(int id, String movieTitle, int year, String posterPath,
                              Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService insertWishlist(String username, int movieId, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService insertView(String user, String jsonParam, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService isInWishlist(String username, int movieId, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService getWishlist(String username, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService getSettings(String username, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService getUser(String username, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService getAllUsers(Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService getViews(String username, String jsonParam, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService getMovieViews(String username, String movieId, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService getUsersCount(Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService getYearsDistribution(String username, String jsonParam, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService getWeekdaysDistribution(String username, String jsonParam, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService getTimeDistribution(String username, String jsonParam, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService getAllTimeMeta(String username, String jsonParam, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService getViewsMeta(String username, String jsonParam, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService removeView(String username, String id, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService removeEpisode(String username, String episodeId, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService insertEpisodeView(String username, String data, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService getSeenEpisodes(String username, int seriesId, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService insertSeries(int id, String seriesTitle, String posterPath, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService getWatchingSeries(String username, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService removeFromWishlist(String username, String movieId, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService getLastMoviesHome(String username, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService getLastWishlistHome(String username, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService getTopMoviesHome(String username, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService getTotalMovieCount(String username, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService getNewMovieCount(String username, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService getTotalRuntime(String username, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService getTotalDistinctMoviesCount(String username, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService getTotalCinemaCount(String username, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService getTopMoviesStat(String username, String jsonParam, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService getMonthYearDistribution(String username, String jsonParam, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService insertSeasonViews(String username, JsonObject seasonData, String seriesId,
                                    Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService removeSeasonViews(String username, String seasonId, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService updateUserVerifyStatus(String username, String value, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService insertUserSettings(String username, String unique, Handler<AsyncResult<JsonObject>> handler);
}
