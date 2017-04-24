package server.util;

import com.mysql.cj.jdbc.MysqlDataSource;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import rx.Single;
import server.service.DatabaseService;
import server.service.DatabaseServiceImpl;

import java.sql.Connection;
import java.sql.SQLException;

import static io.vertx.rxjava.core.Future.future;
import static java.lang.System.currentTimeMillis;
import static server.util.CommonUtils.ifPresent;

public class LocalDatabase {
    public static final String SQL_TRUNCATE_TABLES = "TRUNCATE TABLE Users;" +
            "TRUNCATE TABLE Settings;" +
            "TRUNCATE TABLE Movies;" +
            "TRUNCATE TABLE Views;" +
            "TRUNCATE TABLE Wishlist;";
    public static final String SQL_CREATE_USERS = "CREATE TABLE Users (" +
            "    Id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,\n" +
            "    Firstname VARCHAR(100) NOT NULL,\n" +
            "    Lastname VARCHAR(100) NOT NULL,\n" +
            "    Username VARCHAR(100) NOT NULL,\n" +
            "    Password VARCHAR(64) DEFAULT 'NULL',\n" +
            "    Salt VARCHAR(16) DEFAULT 'default' NOT NULL)";
    public static final String SQL_CREATE_SETTINGS = "CREATE TABLE Settings (\n" +
            "  Id          INT NOT NULL AUTO_INCREMENT PRIMARY KEY,\n" +
            "  RuntimeType VARCHAR(100) DEFAULT 'default' NOT NULL,\n" +
            "  Username    VARCHAR(100)                   NOT NULL,\n" +
            "  Verified    VARCHAR(64) DEFAULT '0'        NOT NULL);";
    public static final String SQL_CREATE_MOVIES = "CREATE TABLE Movies (\n" +
            "  Id    INT NOT NULL AUTO_INCREMENT PRIMARY KEY,\n" +
            "  Title VARCHAR(100) NOT NULL,\n" +
            "  Year  SMALLINT NOT NULL,\n" +
            "  Image VARCHAR(64)  NOT NULL);";
    public static final String SQL_CREATE_VIEWS = "CREATE TABLE Views (\n" +
            "  Id        INT NOT NULL AUTO_INCREMENT PRIMARY KEY,\n" +
            "  MovieId   INT                     NOT NULL,\n" +
            "  Start     DATETIME                NULL,\n" +
            "  End       DATETIME                NULL,\n" +
            "  WasFirst  TINYINT                 NOT NULL,\n" +
            "  WasCinema TINYINT                 NOT NULL,\n" +
            "  Username  VARCHAR(100)            NOT NULL,\n" +
            "  Comment   VARCHAR(500) DEFAULT '' NOT NULL\n);";
    public static final String SQL_CREATE_WISHLIST = "CREATE TABLE Wishlist (" +
            "  MovieId  INT                     NOT NULL,\n" +
            "  Username VARCHAR(100)            NOT NULL,\n" +
            "  Time     BIGINT NOT NULL,\n" +
            "  PRIMARY KEY (Username, MovieId));";
    public static final String SQL_INSERT_FORM_USER = "INSERT INTO Users " +
            "(Username, Firstname, Lastname, Password, Salt) " +
            "VALUES ('unittest@kyngas.eu', 'Form', 'Tester', " +
            "'d7d1b328a56a8c8bfd6dd4e3d9006365d3496b523ac4ff37bc68679b5433b486', 'f44a65de25274188')";
    public static final String SQL_INSERT_FORM_SETTING = "INSERT INTO Settings (Username, RuntimeType, Verified) " +
            "VALUES ('unittest@kyngas.eu', 'default', '1')";
    public static final String SQL_INSERT_FB_USER = "INSERT INTO Users " +
            "(Username, Firstname, Lastname, Password, Salt) " +
            "VALUES ('facebook_ekubamn_tester@tfbnw.net', 'Facebook', 'Tester', " +
            "'38427f3f11e535e128a5e12f962070eb5c72bdfb324ba6b55d9caed2223ffaf8', 'a8eb1f3ed5882b83')";
    public static final String SQL_INSERT_FB_SETTING = "INSERT INTO Settings (Username, RuntimeType, Verified) " +
            "VALUES ('facebook_ekubamn_tester@tfbnw.net', 'default', '0')";
    public static final String SQL_INSERT_MOVIES_HOBBIT = "INSERT INTO Movies (Id, Title, Year, Image) " +
            "VALUES ('49051', 'The Hobbit: An Unexpected Journey', '2012', '/w29Guo6FX6fxzH86f8iAbEhQEFC.jpg')";
    public static final String SQL_INSERT_MOVIES_GHOST = "INSERT INTO Movies (Id, Title, Year, Image) " +
            "VALUES ('315837', 'Ghost in the Shell', '2017', '/si1ZyELNHdPUZw4pXR5KjMIIsBF.jpg')";
    public static final String SQL_INSERT_WISHLIST = "INSERT INTO Wishlist (Username, MovieId, Time) " +
            "VALUES ('unittest@kyngas.eu', '49051', '" + currentTimeMillis() + "')";
    public static final String SQL_INSERT_VIEW = "INSERT INTO Views " +
            "(Username, MovieId, Start, End, WasFirst, WasCinema, Comment) " +
            "VALUES ('unittest@kyngas.eu', '49051', '2017-04-23 17:58:00', '2017-04-23 19:44:00', '1', '0', 'random')";

    private final JDBCClient client;

    private LocalDatabase(Vertx vertx, JsonObject config) {
        DatabaseServiceImpl.setTesting();
        this.client = JDBCClient.createShared(vertx, config);
    }

    public static Future<LocalDatabase> initializeDatabase(Vertx vertx, JsonObject config) {
        return future(fut -> ifPresent(new LocalDatabase(vertx, config
                .put("url", "jdbc:h2:mem:test;DATABASE_TO_UPPER=false")
                .put("driver_class", "org.h2.Driver")
                .put("max_pool_size", 30)), db -> db.initTables()
                .doOnError(fut::fail)
                .map(res -> db.initData().rxSetHandler())
                .subscribe(r -> fut.complete(db), fut::fail)));
    }

    public JDBCClient getClient() {
        return client;
    }

    private Single<UpdateResult> initTables() {
        return client.rxGetConnection().flatMap(conn -> conn.rxUpdate(SQL_CREATE_USERS)
                .flatMap(res -> conn.rxUpdate(SQL_CREATE_SETTINGS))
                .flatMap(res -> conn.rxUpdate(SQL_CREATE_MOVIES))
                .flatMap(res -> conn.rxUpdate(SQL_CREATE_VIEWS))
                .flatMap(res -> conn.rxUpdate(SQL_CREATE_WISHLIST))
                .doAfterTerminate(conn::close));
    }

    public Future<Void> initData() {
        return future(fut -> client.rxGetConnection()
                .flatMap(conn -> conn.rxUpdate(SQL_INSERT_FORM_USER)
                        .flatMap(res -> conn.rxUpdate(SQL_INSERT_FORM_SETTING))
                        .flatMap(res -> conn.rxUpdate(SQL_INSERT_FB_USER))
                        .flatMap(res -> conn.rxUpdate(SQL_INSERT_FB_SETTING))
                        //.flatMap(res -> conn.rxUpdate(SQL_INSERT_MOVIES_HOBBIT))
                        //.flatMap(res -> conn.rxUpdate(SQL_INSERT_MOVIES_GHOST))
                        .flatMap(res -> conn.rxUpdate(SQL_INSERT_WISHLIST))
                        .flatMap(res -> conn.rxUpdate(SQL_INSERT_VIEW))
                        .doAfterTerminate(conn::close))
                .subscribe(res -> fut.complete(), fut::fail));
    }

    public Future<Void> resetCleanState() {
        return future(fut -> client.rxGetConnection()
                .flatMap(conn -> conn.rxExecute(SQL_TRUNCATE_TABLES).doAfterTerminate(conn::close))
                .doOnError(fut::fail)
                .flatMap(res -> initData().rxSetHandler())
                .subscribe(res -> fut.complete(), fut::fail));
    }

    public void resetCleanStateBlocking() {
        resetCleanState().rxSetHandler().toBlocking().value();
    }

    public Future<Void> dropAll() {
        return future(fut -> client.rxGetConnection()
                .flatMap(conn -> conn.rxExecute("DROP ALL OBJECTS").doAfterTerminate(conn::close))
                .subscribe(fut::complete, fut::fail));
    }

    public Future<JsonArray> query(String sql, JsonArray params) {
        return future(fut -> client.rxGetConnection()
                .flatMap(conn -> conn.rxQueryWithParams(sql, params)
                        .doAfterTerminate(conn::close))
                .map(ResultSet::toJson)
                .map(DatabaseService::getRows)
                .subscribe(fut::complete, fut::fail));
    }

    public JsonArray queryBlocking(String sql, JsonArray params) {
        return query(sql, params).rxSetHandler().toBlocking().value();
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

    @SuppressWarnings("unused")
    private void test() throws SQLException {
        Connection conn = new MysqlDataSource().getConnection();
        conn.prepareStatement(SQL_CREATE_MOVIES);
    }
}