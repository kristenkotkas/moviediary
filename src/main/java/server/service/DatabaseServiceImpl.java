package server.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import server.router.UiRouter;

import java.time.LocalDateTime;

import static server.util.CommonUtils.contains;
import static server.util.CommonUtils.nonNull;

//näited -> https://github.com/vert-x3/vertx-examples/tree/master/jdbc-examples

public class DatabaseServiceImpl extends CachingServiceImpl<JsonObject> implements DatabaseService {
    private static final Logger log = LoggerFactory.getLogger(DatabaseServiceImpl.class);

    private static final String MYSQL = "mysql";

    private static final String SQL_INSERT_USER =
            "INSERT INTO Users (Username, Password, Firstname, Lastname) VALUES (?, ?, ?, ?)";
    private static final String SQL_QUERY_USERS = "SELECT * FROM Users";
    private static final String SQL_QUERY_USER = "SELECT * FROM Users WHERE Username = ?";
    private static final String SQL_INSERT_DEMO_VIEWS = "INSERT INTO Views (Username, MovieId, Start, End, WasFirst, WasCinema) " +
            "VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SQL_QUERY_VIEWS =
                    "SELECT Title, Start, WasFirst, WasCinema " +
                    "FROM Views " +
                    "JOIN Movies ON Views.MovieId = Movies.Id " +
                    "WHERE Username = ?";

    private final Vertx vertx;
    private final JsonObject config;
    private final JDBCClient client;

    protected DatabaseServiceImpl(Vertx vertx, JsonObject config) {
        super(CachingServiceImpl.DEFAULT_MAX_CACHE_SIZE);
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
        client.getConnection(connHandler(future, conn -> conn.updateWithParams(SQL_INSERT_USER, new JsonArray()
                .add(username)
                .add(password)
                .add(firstname)
                .add(lastname), updateResultHandler(conn, future))));
        insertDemoViews(username, 157336, 1,0);
        insertDemoViews(username, 334541, 1,1);
        insertDemoViews(username, 334543, 0,1);
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
    public Future<JsonObject> getAllViews() {
        Future<JsonObject> future = Future.future();
        CacheItem<JsonObject> cache = getCached(CACHE_ALL);
        if (!tryCachedResult(false, cache, future)) {
            client.getConnection(connHandler(future,
                    conn -> conn.queryWithParams(SQL_QUERY_VIEWS, new JsonArray().add(UiRouter.unique),
                            resultSetHandler(conn, CACHE_ALL, future))));
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
