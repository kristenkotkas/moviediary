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
import java.util.List;
import java.util.Map;

import static java.util.concurrent.TimeUnit.MINUTES;
import static server.service.DatabaseService.Column.*;
import static server.service.DatabaseService.SQLCommand.INSERT;
import static server.service.DatabaseService.SQLCommand.UPDATE;
import static server.service.DatabaseService.createDataMap;
import static server.util.CommonUtils.contains;
import static server.util.CommonUtils.nonNull;
import static server.util.HandlerUtils.futureHandler;
import static server.util.StringUtils.*;

/**
 * Database service implementation.
 */
public class DatabaseServiceImpl extends CachingServiceImpl<JsonObject> implements DatabaseService {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseServiceImpl.class);
    private static final String MYSQL = "mysql";

    private static final String SQL_INSERT_USER =
            "INSERT INTO Users (Username, Firstname, Lastname, Password, Salt) VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_QUERY_USERS = "SELECT * FROM Users " +
            "JOIN Settings ON Users.Username = Settings.Username";
    private static final String SQL_QUERY_USER = "SELECT * FROM Users WHERE Username = ?";

    private static final String SQL_INSERT_DEMO_VIEWS =
            "INSERT INTO Views (Username, MovieId, Start, End, WasFirst, WasCinema) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SQL_QUERY_VIEWS =
            "SELECT Title, Start, WasFirst, WasCinema " +
                    "FROM Views " +
                    "JOIN Movies ON Views.MovieId = Movies.Id " +
                    "WHERE Username = ? AND Start >= ? AND End <= ?";

    private static final String SQL_QUERY_SETTINGS = "SELECT * FROM Settings WHERE Username = ?";

    private static final String SQL_INSERT_MOVIE =
            "INSERT IGNORE INTO Movies VALUES (?, ?, ?)";

    private static final String SQL_VIEWS_COUNT = "SELECT COUNT(*) AS Count FROM Users";

    private static final String SQL_GET_MOVIE_VIEWS =
            "SELECT Start, WasCinema From Views" +
            " WHERE Username = ? AND MovieId = ?";

    private final JDBCClient client;

    protected DatabaseServiceImpl(Vertx vertx, JsonObject config) {
        super(DEFAULT_MAX_CACHE_SIZE);
        this.client = JDBCClient.createShared(vertx, config.getJsonObject(MYSQL));
        vertx.setPeriodic(MINUTES.toMillis(15), connectionHeartbeat());
    }

    @Override
    public Future<JsonObject> insertUser(String username, String password, String firstname, String lastname) {
        Future<JsonObject> future = Future.future();
        if (!nonNull(username, password, firstname, lastname) || contains("", username, firstname, lastname)) {
            future.fail(new Throwable("Email, firstname and lastname must exist!"));
            return future;
        }
        String salt = genString();
        client.getConnection(connHandler(future,
                conn -> conn.updateWithParams(SQL_INSERT_USER, new JsonArray()
                        .add(username)
                        .add(firstname)
                        .add(lastname)
                        .add(hash(password, salt))
                        .add(salt), ar -> {
                    if (ar.succeeded()) {
                        Future<JsonObject> future1 = insertDemoViews(username, 157336, 1, 0);
                        Future<JsonObject> future2 = insertDemoViews(username, 334541, 1, 1);
                        Future<JsonObject> future3 = insertDemoViews(username, 334543, 0, 1);
                        Future<JsonObject> future4 = insert(Table.SETTINGS, createDataMap(username));
                        CompositeFuture.all(future1, future2, future3, future4).setHandler(result -> {
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
    public Future<JsonObject> insertMovie(int id, String movieTitle, int year) {
        Future<JsonObject> future = Future.future();
        client.getConnection(connHandler(future, conn -> conn.updateWithParams(SQL_INSERT_MOVIE, new JsonArray()
                .add(id)
                .add(movieTitle)
                .add(year), updateResultHandler(conn, future))));
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

    @Override
    public Future<JsonObject> update(Table table, Map<Column, String> data) {
        Future<JsonObject> future = Future.future();
        if (data.get(USERNAME) == null) {
            future.fail("Username required.");
            return future;
        }
        if (data.size() == 1) {
            future.fail("No columns specified.");
            return future;
        }
        List<Column> columns = getSortedColumns(data);
        client.getConnection(connHandler(future,
                conn -> conn.updateWithParams(UPDATE.create(table, columns), getSortedValues(columns, data),
                        updateResultHandler(conn, future))));
        return future;
    }

    @Override
    public Future<JsonObject> insert(Table table, Map<Column, String> data) {
        Future<JsonObject> future = Future.future();
        if (data.get(USERNAME) == null) {
            future.fail("Username required.");
            return future;
        }
        List<Column> columns = getSortedColumns(data);
        client.getConnection(connHandler(future,
                conn -> conn.updateWithParams(INSERT.create(table, columns), getSortedValues(columns, data),
                        updateResultHandler(conn, future))));
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
        if (!tryCachedResult(false, cache, future)) {
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

    @Override
    public Future<JsonObject> getMovieViews(String username, String param) {
        System.out.println("---------------------------------------------");
        System.out.println("USERNAME: " + username);
        System.out.println("PARAM: " + param);
        System.out.println("---------------------------------------------");
        Future<JsonObject> future = Future.future();
        CacheItem<JsonObject> cache = getCached(CACHE_VIEWS + username + param);
        if (!tryCachedResult(false, cache, future)) {
            client.getConnection(connHandler(future,
                    conn -> conn.queryWithParams(SQL_GET_MOVIE_VIEWS, new JsonArray()
                    .add(username)
                    .add(param),
                            resultSetHandler(conn, CACHE_VIEWS + username + param, future))));
        }
        return future;
    }

    @Override
    public Future<String> getUsersCount() {
        Future<String> future = Future.future();
        client.getConnection(connHandler(future,
                conn -> conn.query(SQL_VIEWS_COUNT, ar -> {
                    if (ar.succeeded()) {
                        future.complete(ar.result().getRows().get(0).getLong("Count").toString());
                    } else {
                        future.fail(ar.cause());
                    }
                    conn.close();
                })));
        return future;
    }

    private Handler<AsyncResult<SQLConnection>> connHandler(Future future, Handler<SQLConnection> handler) {
        return conn -> {
            if (conn.succeeded()) {
                handler.handle(conn.result());
            } else {
                future.fail(conn.cause());
                // TODO: 4.03.2017 failed connection still needs to be closed?
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

    /**
     * Makes a connetion to database and logs if it fails.
     */
    private Handler<Long> connectionHeartbeat() {
        return timer -> {
            Future<Void> future = Future.future();
            client.getConnection(connHandler(future, conn -> conn.query("SELECT version()", futureHandler(future))));
            future.setHandler(ar -> {
                if (ar.failed()) {
                    LOG.error("Connection heartbeat failed: ", ar.cause());
                }
            });
        };
    }
}
