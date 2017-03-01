package server.service;

import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;

import java.time.LocalDateTime;

import static server.util.CommonUtils.contains;
import static server.util.CommonUtils.nonNull;
import static server.util.StringUtils.*;

//näited -> https://github.com/vert-x3/vertx-examples/tree/master/jdbc-examples

public class DatabaseServiceImpl extends CachingServiceImpl<JsonObject> implements DatabaseService {
    private static final Logger log = LoggerFactory.getLogger(DatabaseServiceImpl.class);
    private static final String MYSQL = "mysql";

    private static final String SQL_INSERT_USER =
            "INSERT INTO Users (Username, Firstname, Lastname, Password, Salt) VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_QUERY_USERS = "SELECT * FROM Users";
    private static final String SQL_QUERY_USER = "SELECT * FROM Users WHERE Username = ?";

    private static final String SQL_INSERT_DEMO_VIEWS =
            "INSERT INTO Views (Username, MovieId, Start, End, WasFirst, WasCinema) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SQL_QUERY_VIEWS =
            "SELECT Title, Start, WasFirst, WasCinema " +
                    "FROM Views " +
                    "JOIN Movies ON Views.MovieId = Movies.Id " +
                    "WHERE Username = ? AND Start >= ? AND End <= ?";

    private static final String SQL_QUERY_SETTINGS = "SELECT * FROM Settings WHERE Username = ?";
    private static final String SQL_UPDATE_SETTINGS =
            "UPDATE Settings SET RuntimeType = ?, Language = ? WHERE Username = ?";
    private static final String SQL_INSERT_SETTINGS =
            "INSERT INTO Settings (Username, RuntimeType, Language) VALUES (?, ?, ?)";

    private final Vertx vertx;
    private final JsonObject config;
    private final JDBCClient client;

    protected DatabaseServiceImpl(Vertx vertx, JsonObject config) {
        super(DEFAULT_MAX_CACHE_SIZE);
        this.vertx = vertx;
        this.config = config;
        this.client = JDBCClient.createShared(vertx, config.getJsonObject(MYSQL));
    }

    @Override
    public Future<JsonObject> insertUser(String username, String password, String firstname, String lastname) {
        Future<JsonObject> future = Future.future();
        if (!nonNull(username, password, firstname, lastname) || contains("", username, firstname, lastname)) {
            future.fail(new Throwable("Email, firstname and lastname must exist!"));
            return future;
        }
        String salt = genString();
        client.getConnection(connHandler(future, conn -> conn.updateWithParams(SQL_INSERT_USER, new JsonArray()
                .add(username)
                .add(firstname)
                .add(lastname)
                .add(hash(password, salt))
                .add(salt), ar -> {
            if (ar.succeeded()) {
                Future<JsonObject> future1 = insertDemoViews(username, 157336, 1, 0);
                Future<JsonObject> future2 = insertDemoViews(username, 334541, 1, 1);
                Future<JsonObject> future3 = insertDemoViews(username, 334543, 0, 1);
                //teeme 3 sisestust korraga ja saame teada kas õnnestus
                CompositeFuture.all(future1, future2, future3).setHandler(result -> {
                    if (result.succeeded()) {
                        future.complete(ar.result().toJson());
                    } else {
                        future.fail(result.cause());
                    }
                });
            } else {
                future.fail(ar.cause());
            }
            conn.close();
        })));
        return future;
    }

    private Future<JsonObject> insertDemoViews(String username, int movieId, int wasFirst, int wasCinema) {
        Future<JsonObject> future = Future.future();
        client.getConnection(connHandler(future,
                conn -> conn.updateWithParams(SQL_INSERT_DEMO_VIEWS, new JsonArray()
                        .add(username)
                        .add(movieId)
                        .add(LocalDateTime.now().toString())
                        .add(LocalDateTime.now().plusHours(2).toString())
                        .add(wasFirst)
                        .add(wasCinema), updateResultHandler(conn, future))));
        return future;
    }

    @Override
    public Future<JsonObject> getSettings(String username) {
        Future<JsonObject> future = Future.future();
        client.getConnection(connHandler(future,
                conn -> conn.queryWithParams(SQL_QUERY_SETTINGS, new JsonArray().add(username),
                        resultSetHandler(conn, CACHE_SETTINGS + username, future))));
        return future;
    }

    // TODO: 2.03.2017 only update what needed
    @Override
    public Future<JsonObject> updateSettings(String username, String runtimeType, String language) {
        Future<JsonObject> future = Future.future();
        // TODO: 01/03/2017 get previous settings -> if param null, replace
        client.getConnection(connHandler(future,
                conn -> conn.updateWithParams(SQL_UPDATE_SETTINGS, new JsonArray()
                        .add(runtimeType != null ? runtimeType : "default")
                        .add(language)
                        .add(username), updateResultHandler(conn, future))));
        return future;
    }

    @Override
    public Future<JsonObject> insertSettings(String username, String runtimeType, String language) {
        Future<JsonObject> future = Future.future();
        // TODO: 01/03/2017 get previous settings -> if param null, replace
        client.getConnection(connHandler(future,
                conn -> conn.updateWithParams(SQL_INSERT_SETTINGS, new JsonArray()
                        .add(username)
                        .add(runtimeType != null ? runtimeType : "default")
                        .add(language), updateResultHandler(conn, future))));
        return future;
    }

    @Override
    public Future<JsonObject> getUser(String username) {
        Future<JsonObject> future = Future.future();
        CacheItem<JsonObject> cache = getCached(CACHE_USER + username);
        if (!tryCachedResult(false, cache, future)) {
            client.getConnection(connHandler(future,
                    conn -> conn.queryWithParams(SQL_QUERY_USER, new JsonArray().add(username),
                            resultSetHandler(conn, CACHE_USER + username, future))));
        }
        return future;
    }

    @Override
    public Future<JsonObject> getAllUsers() {
        Future<JsonObject> future = Future.future();
        CacheItem<JsonObject> cache = getCached(CACHE_ALL);
        if (!tryCachedResult(false, cache, future)) { //cache timeout peaks väikseks panema või mitte kasutama
            client.getConnection(connHandler(future,
                    conn -> conn.query(SQL_QUERY_USERS, resultSetHandler(conn, CACHE_ALL, future))));
        }
        return future;
    }

    @Override
    public Future<JsonObject> getViews(String username, String param) {
        JsonObject json = new JsonObject(param);
        json.put("start", formToDBDate(json.getString("start"), false));
        json.put("end", formToDBDate(json.getString("end"), true));
        System.out.println(json.encodePrettily());
        //"WHERE Username = ? AND Start >= ? AND End <= ? AND " + "WasFirst = ? AND WasCinema = ?"
        // AND ? AND ?
        String SQL_QUERY_VIEWS_TEMP = SQL_QUERY_VIEWS;
        if (json.getBoolean("is-first")) {
            SQL_QUERY_VIEWS_TEMP += " AND WasFirst";
        }
        if (json.getBoolean("is-cinema")) {
            SQL_QUERY_VIEWS_TEMP += " AND WasCinema";
        }
        SQL_QUERY_VIEWS_TEMP += " ORDER BY Start DESC";

        System.out.println("QUERY:" + SQL_QUERY_VIEWS);

        Future<JsonObject> future = Future.future();
        CacheItem<JsonObject> cache = getCached(CACHE_VIEWS + username);
        if (!tryCachedResult(false, cache, future)) {
            String finalSQL_QUERY_VIEWS_TEMP = SQL_QUERY_VIEWS_TEMP;
            client.getConnection(connHandler(future,
                    conn -> conn.queryWithParams(finalSQL_QUERY_VIEWS_TEMP, new JsonArray()
                                    .add(username)
                                    .add(json.getString("start"))
                                    .add(json.getString("end")),
                            resultSetHandler(conn, CACHE_VIEWS + username, future))));
        }
        return future;
    }

    private Handler<AsyncResult<SQLConnection>> connHandler(Future future, Handler<SQLConnection> handler) {
        return conn -> {
            if (conn.succeeded()) {
                handler.handle(conn.result());
            } else {
                future.fail(conn.cause());
            }
        };
    }

    private Handler<AsyncResult<ResultSet>> resultSetHandler(SQLConnection conn, String key,
                                                             Future<JsonObject> future) {
        return ar -> {
            if (ar.succeeded()) {
                future.complete(getCached(key).set(ar.result().toJson()));
            } else {
                future.fail(ar.cause());
            }
            conn.close();
        };
    }

    private Handler<AsyncResult<UpdateResult>> updateResultHandler(SQLConnection conn, Future<JsonObject> future) {
        return ar -> {
            if (ar.succeeded()) {
                future.complete(ar.result().toJson());
            } else {
                future.fail(ar.cause());
            }
            conn.close();
        };
    }
}
