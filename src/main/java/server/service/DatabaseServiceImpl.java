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

import static server.util.CommonUtils.contains;
import static server.util.CommonUtils.nonNull;

//näited -> https://github.com/vert-x3/vertx-examples/tree/master/jdbc-examples

public class DatabaseServiceImpl extends CachingServiceImpl<JsonObject> implements DatabaseService {
    private static final Logger log = LoggerFactory.getLogger(DatabaseServiceImpl.class);

    private static final String MYSQL = "mysql";

    private static final String SQL_INSERT_USER =
            "INSERT INTO Users (Email, Password, Serialnumber, Firstname, Lastname) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_QUERY_USERS = "SELECT * FROM Users";
    private static final String SQL_QUERY_USER = "SELECT * FROM Users WHERE Email = ?";
    private static final String SQL_QUERY_VIEWS =
            "SELECT Firstname, Lastname, Title, Start, End, WasFirst, WasCinema " +
                    "FROM Views " +
                    "JOIN Movies ON Views.MovieId = Movies.Id " +
                    "JOIN Users ON Views.UserId = Users.Id " +
                    "WHERE Email = ?";

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
    public Future<JsonObject> insertUser(JsonArray userData) {
        // TODO: 19.02.2017 create userData array here, check for invalid values
        Future<JsonObject> future = Future.future();
        client.getConnection(connHandler(future,
                conn -> conn.updateWithParams(SQL_INSERT_USER, userData, ar -> {
                    if (ar.succeeded()) {
                        future.complete(ar.result().toJson());
                    } else {
                        future.fail(ar.cause());
                    }
                    conn.close();
                })));
        return future;
    }

    @Override
    public Future<JsonObject> insertOAuth2User(String email, String firstname, String lastname) {
        Future<JsonObject> future = Future.future();
        if (!nonNull(email, firstname, lastname) || contains("", email, firstname, lastname)) {
            future.fail(new Throwable("Email, firstname and lastname must exist!"));
            return future;
        }
        client.getConnection(connHandler(future, conn -> conn.updateWithParams(SQL_INSERT_USER, new JsonArray()
                .add(email).add("")
                .add("")
                .add(firstname).add(lastname), updateResultHandler(conn, future))));
        return future;
    }

    @Override
    public Future<JsonObject> insertIdCardUser(String serial, String firstname, String lastname) {
        Future<JsonObject> future = Future.future();
        if (!nonNull(serial, firstname, lastname) || contains("", serial, firstname, lastname)) {
            future.fail(new Throwable("SerialCode, firstname and lastname must exist!"));
            return future;
        }
        client.getConnection(connHandler(future, conn -> conn.updateWithParams(SQL_INSERT_USER, new JsonArray()
                .add("").add("")
                .add(serial)
                .add(firstname).add(lastname), updateResultHandler(conn, future))));
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
