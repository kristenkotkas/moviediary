package entity;

import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import rx.Single;
import util.JsonUtils;

import static entity.LocalSql.*;
import static io.vertx.rxjava.core.Future.failedFuture;
import static io.vertx.rxjava.core.Future.future;
import static util.ConditionUtils.ifPresent;

public class LocalDatabase {
  private static final Logger LOG = LoggerFactory.getLogger(LocalDatabase.class);
  private final JDBCClient client;
  private final DB database;

  private LocalDatabase(Vertx vertx, JsonObject config) {
    this.database = createDatabase(config);
    this.client = JDBCClient.createShared(vertx, config);
  }

  public static JsonObject toTestingConfig(JsonObject config) {
    config.getJsonObject("mysql")
        .put("url", "jdbc:mysql://localhost:3366/test")
        .put("driver_class", "com.mysql.cj.jdbc.Driver")
        .put("max_pool_size", 30);
    return config;
  }

  private static DB createDatabase(JsonObject config) {
    config.put("user", "root");
    config.remove("password");
    try {
      DB db = DB.newEmbeddedDB(3366);
      db.start();
      return db;
    } catch (ManagedProcessException e) {
      LOG.debug("DB startup failed", e);
      throw new RuntimeException(e.getCause());
    }
  }

  public void close() {
    client.close();
    try {
      database.stop();
    } catch (ManagedProcessException e) {
      LOG.debug("DB close failed", e);
      throw new RuntimeException(e.getCause());
    }
  }

  public static Future<LocalDatabase> initializeDatabase(Vertx vertx, JsonObject config) {
    JsonObject dbConfig = config.getJsonObject("mysql");
    if (!dbConfig.getString("url").contains("localhost")) {
      return failedFuture("Configuration is not set to testing environment.");
    }
    return future(fut -> ifPresent(new LocalDatabase(vertx, dbConfig), db -> db.initTables()
        .doOnError(fut::fail)
        .map(res -> db.initData().rxSetHandler())
        .subscribe(r -> fut.complete(db), fut::fail)));
  }

  private Single<UpdateResult> initTables() {
    return client.rxGetConnection().flatMap(conn -> conn.rxUpdate(CREATE_USERS_TABLE.get())
        .flatMap(res -> conn.rxUpdate(CREATE_SETTINGS_TABLE.get()))
        .flatMap(res -> conn.rxUpdate(CREATE_MOVIES_TABLE.get()))
        .flatMap(res -> conn.rxUpdate(CREATE_SERIES_TABLE.get()))
        .flatMap(res -> conn.rxUpdate(CREATE_SERIES_INFO_TABLE.get()))
        .flatMap(res -> conn.rxUpdate(CREATE_VIEWS_TABLE.get()))
        .flatMap(res -> conn.rxUpdate(CREATE_WISHLIST_TABLE.get()))
        .doAfterTerminate(conn::close));
  }

  public Future<Void> initData() {
    return future(fut -> client.rxGetConnection()
        .flatMap(conn -> conn.rxUpdate(INSERT_FORM_USER.get())
            .flatMap(res -> conn.rxUpdate(INSERT_FORM_SETTING.get()))
            .flatMap(res -> conn.rxUpdate(INSERT_FB_USER.get()))
            .flatMap(res -> conn.rxUpdate(INSERT_FB_SETTING.get()))
            //.flatMap(res -> conn.rxUpdate(INSERT_MOVIES_HOBBIT.get()))
            //.flatMap(res -> conn.rxUpdate(INSERT_MOVIES_GHOST.get()))
            //.flatMap(res -> conn.rxUpdate(INSERT_WISHLIST_HOBBIT.get()))
            //.flatMap(res -> conn.rxUpdate(INSERT_VIEW_HOBBIT.get()))
            .doAfterTerminate(conn::close))
        .subscribe(res -> fut.complete(), fut::fail));
  }

  public Future<Void> resetCleanState() {
    return future(fut -> client.rxGetConnection()
        .flatMap(conn -> conn.rxExecute("TRUNCATE TABLE test.users")
            .flatMap(v -> conn.rxExecute("TRUNCATE TABLE test.settings"))
            .flatMap(v -> conn.rxExecute("TRUNCATE TABLE test.movies"))
            .flatMap(v -> conn.rxExecute("TRUNCATE TABLE test.series"))
            .flatMap(v -> conn.rxExecute("TRUNCATE TABLE test.seriesInfo"))
            .flatMap(v -> conn.rxExecute("TRUNCATE TABLE test.views"))
            .flatMap(v -> conn.rxExecute("TRUNCATE TABLE test.wishlist"))
            .doAfterTerminate(conn::close))
        .doOnError(fut::fail)
        .flatMap(res -> initData().rxSetHandler())
        .subscribe(res -> fut.complete(), fut::fail));
  }

  public void resetCleanStateBlocking() {
    resetCleanState().rxSetHandler().toBlocking().value();
  }

  public Future<JsonArray> query(String sql, JsonArray params) {
    return future(fut -> client.rxGetConnection()
        .flatMap(conn -> conn.rxQueryWithParams(sql, params)
            .doAfterTerminate(conn::close))
        .map(ResultSet::toJson)
        .map(JsonUtils::getRows)
        .subscribe(fut::complete, fut::fail));
  }

  public JsonArray queryBlocking(String sql, JsonArray params) {
    return query(sql, params).rxSetHandler().toBlocking().value();
  }

  public JsonArray queryBlocking(LocalSql sql, JsonArray params) {
    return query(sql.get(), params).rxSetHandler().toBlocking().value();
  }

  public Future<JsonObject> updateOrInsert(String sql, JsonArray params) {
    return future(fut -> client.rxGetConnection()
        .flatMap(conn -> conn.rxUpdateWithParams(sql, params)
            .doAfterTerminate(conn::close))
        .map(UpdateResult::toJson)
        .subscribe(fut::complete, fut::fail));
  }

  public JsonObject updateOrInsertBlocking(String sql, JsonArray params) {
    return updateOrInsert(sql, params).rxSetHandler().toBlocking().value();
  }

  public JsonObject updateOrInsertBlocking(LocalSql sql, JsonArray params) {
    return updateOrInsert(sql.get(), params).rxSetHandler().toBlocking().value();
  }
}
