package server.util;

import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import rx.Single;
import server.service.DatabaseService;

import static io.vertx.rxjava.core.Future.future;
import static java.lang.System.currentTimeMillis;
import static server.util.CommonUtils.ifPresent;

public class LocalDatabase {
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
            "  Id    INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,\n" +
            "  Title VARCHAR(100) NOT NULL,\n" +
            "  Year  SMALLINT     NOT NULL,\n" +
            "  Image VARCHAR(64)  NOT NULL\n" +
            ");";
    public static final String SQL_CREATE_SERIES = "CREATE TABLE Series (\n" +
            "  Username  VARCHAR(100) NOT NULL,\n" +
            "  SeriesId  INT          NOT NULL,\n" +
            "  EpisodeId INT          NOT NULL,\n" +
            "  SeasonId  VARCHAR(100) NOT NULL,\n" +
            "  Time      BIGINT       NOT NULL,\n" +
            "  PRIMARY KEY (Username, EpisodeId)\n" +
            ");";
    public static final String SQL_CREATE_SERIES_INFO = "CREATE TABLE SeriesInfo (\n" +
            "  Id    INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,\n" +
            "  Title VARCHAR(100) NOT NULL,\n" +
            "  Image VARCHAR(100) NOT NULL\n" +
            ");";
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
            "'967a097e667b8ebcbab27a5327c504dbfefc3fac3ca9eb696e00de16b4005e60', '1ffa4de675252a4d')";
    public static final String SQL_INSERT_FORM_SETTING = "INSERT INTO Settings (Username, RuntimeType, Verified) " +
            "VALUES ('unittest@kyngas.eu', 'default', '1')";
    public static final String SQL_INSERT_FB_USER = "INSERT INTO Users " +
            "(Username, Firstname, Lastname, Password, Salt) " +
            "VALUES ('facebook_ekubamn_tester@tfbnw.net', 'Facebook', 'Tester', " +
            "'1f27217a682d9631b4a839c72ef9bdc4dd5aed7319cc5b0df016ea8ddb81aa1f', '67d2ba0a146054b3')";
    public static final String SQL_INSERT_FB_SETTING = "INSERT INTO Settings (Username, RuntimeType, Verified) " +
            "VALUES ('facebook_ekubamn_tester@tfbnw.net', 'default', '0')";
    public static final String SQL_INSERT_MOVIES_HOBBIT = "INSERT INTO Movies (Id, Title, Year, Image) " +
            "VALUES ('49051', 'The Hobbit: An Unexpected Journey', '2012', '/w29Guo6FX6fxzH86f8iAbEhQEFC.jpg')";
    public static final String SQL_INSERT_MOVIES_GHOST = "INSERT INTO Movies (Id, Title, Year, Image) " +
            "VALUES ('315837', 'Ghost in the Shell', '2017', '/si1ZyELNHdPUZw4pXR5KjMIIsBF.jpg')";
    public static final String SQL_INSERT_SERIES_EPISODE = "INSERT INTO Series " +
            "(Username, SeriesId, EpisodeId, SeasonId, Time) VALUES " +
            "('unittest@kyngas.eu', '42009', '1188308', '571bb2e29251414e97005342', '" + currentTimeMillis() + "')";
    public static final String SQL_INSERT_SERIES_INFO = "INSERT INTO SeriesInfo " +
            "(Id, Title, Image) VALUES " +
            "('42009', 'Black Mirror', '/djUxgzSIdfS5vNP2EHIBDIz9I8A.jpg')";
    public static final String SQL_INSERT_WISHLIST_HOBBIT = "INSERT INTO Wishlist (Username, MovieId, Time) " +
            "VALUES ('unittest@kyngas.eu', '49051', '" + currentTimeMillis() + "')";
    public static final String SQL_INSERT_VIEW_HOBBIT = "INSERT INTO Views " +
            "(Username, MovieId, Start, End, WasFirst, WasCinema, Comment) " +
            "VALUES ('unittest@kyngas.eu', '49051', '2017-04-23 17:58:00', '2017-04-23 19:44:00', '1', '0', 'random')";
    public static final String SQL_INSERT_VIEW_GHOST = "INSERT INTO Views " +
            "(Username, MovieId, Start, End, WasFirst, WasCinema, Comment) " +
            "VALUES ('unittest@kyngas.eu', '315837', '2017-03-17 17:58:00', '2017-03-17 19:44:00', '0', '1', 'lamp')";

    private final JDBCClient client;
    private final DB database;

    private LocalDatabase(Vertx vertx, JsonObject config) {
        database = createDatabase(config);
        this.client = JDBCClient.createShared(vertx, config);
    }

    private static DB createDatabase(JsonObject config) {
        config.put("user", "root");
        config.remove("password");
        try {
            DB db = DB.newEmbeddedDB(3366);
            db.start();
            return db;
        } catch (ManagedProcessException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    public static Future<LocalDatabase> initializeDatabase(Vertx vertx, JsonObject config) {
        return future(fut -> ifPresent(new LocalDatabase(vertx, config
                .put("url", "jdbc:mysql://localhost:3366/test")
                .put("driver_class", "com.mysql.cj.jdbc.Driver")
                .put("max_pool_size", 30)), db -> db.initTables()
                .doOnError(fut::fail)
                .map(res -> db.initData().rxSetHandler())
                .subscribe(r -> fut.complete(db), fut::fail)));
    }

    public void close() {
        client.close();
        try {
            database.stop();
        } catch (ManagedProcessException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    private Single<UpdateResult> initTables() {
        return client.rxGetConnection().flatMap(conn -> conn.rxUpdate(SQL_CREATE_USERS)
                .flatMap(res -> conn.rxUpdate(SQL_CREATE_SETTINGS))
                .flatMap(res -> conn.rxUpdate(SQL_CREATE_MOVIES))
                .flatMap(res -> conn.rxUpdate(SQL_CREATE_SERIES))
                .flatMap(res -> conn.rxUpdate(SQL_CREATE_SERIES_INFO))
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
                        //.flatMap(res -> conn.rxUpdate(SQL_INSERT_WISHLIST_HOBBIT))
                        //.flatMap(res -> conn.rxUpdate(SQL_INSERT_VIEW_HOBBIT))
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
}
