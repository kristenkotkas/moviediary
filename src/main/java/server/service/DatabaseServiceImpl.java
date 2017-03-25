package server.service;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;

import java.util.List;
import java.util.Map;

import static server.service.DatabaseService.Column.*;
import static server.service.DatabaseService.SQLCommand.INSERT;
import static server.service.DatabaseService.SQLCommand.UPDATE;
import static server.service.DatabaseService.*;
import static server.util.CommonUtils.*;
import static server.util.StringUtils.*;
import static server.util.StringUtils.formToDBDate;

/**
 * Database service implementation.
 */
public class DatabaseServiceImpl implements DatabaseService {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseServiceImpl.class);
    private static final String MYSQL = "mysql";

    private static final String SQL_INSERT_USER =
            "INSERT INTO Users (Username, Firstname, Lastname, Password, Salt) VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_QUERY_USERS = "SELECT * FROM Users " +
            "JOIN Settings ON Users.Username = Settings.Username";
    private static final String SQL_QUERY_USER = "SELECT * FROM Users WHERE Username = ?";

    private static final String SQL_INSERT_VIEW =
            "INSERT INTO Views (Username, MovieId, Start, End, WasFirst, WasCinema, Comment) VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_QUERY_VIEWS =
            "SELECT MovieId, Title, Start, WasFirst, WasCinema, Image, Comment " +
                    "FROM Views " +
                    "JOIN Movies ON Views.MovieId = Movies.Id " +
                    "WHERE Username = ? AND Start >= ? AND Start <= ?";

    private static final String SQL_QUERY_SETTINGS = "SELECT * FROM Settings WHERE Username = ?";

    private static final String SQL_INSERT_MOVIE =
            "INSERT IGNORE INTO Movies VALUES (?, ?, ?, ?)";

    private static final String SQL_VIEWS_COUNT = "SELECT COUNT(*) AS Count FROM Users";

    private static final String SQL_GET_MOVIE_VIEWS =
            "SELECT Start, WasCinema From Views" +
                    " WHERE Username = ? AND MovieId = ?" +
                    " ORDER BY Start DESC";

    public static final String SQL_INSERT_WISHLIST =
            "INSERT IGNORE INTO Wishlist (Username, MovieId, Time) VALUES (?, ?, ?)";

    public static final String SQL_IS_IN_WISHLIST =
            "SELECT MovieId FROM Wishlist WHERE Username = ? AND MovieId = ?";

    public static final String SQL_GET_WISHLIST =
            "SELECT Title, Time, Image, MovieId FROM Wishlist " +
                    "JOIN Movies ON Wishlist.MovieId = Movies.Id " +
                    "WHERE Username =  ? ORDER BY Time DESC";

    private final JDBCClient client;

    protected DatabaseServiceImpl(Vertx vertx, JsonObject config) {
        this.client = JDBCClient.createShared(vertx, config.getJsonObject(MYSQL));
    }

    /**
     * Inserts a Facebook, Google or IdCard user into database.
     * Also inserts 3 movie views for demonstration purposes.
     */
    @Override
    public Future<JsonObject> insertUser(String username, String password, String firstname, String lastname) {
        return future(fut -> {
            if (!nonNull(username, password, firstname, lastname) || contains("", username, firstname, lastname)) {
                fut.fail(new Throwable("Email, firstname and lastname must exist!"));
                return;
            }
            String salt = genString();
            client.getConnection(connHandler(fut, conn -> conn.updateWithParams(SQL_INSERT_USER, new JsonArray()
                    .add(username)
                    .add(firstname)
                    .add(lastname)
                    .add(hash(password, salt))
                    .add(salt), ar -> {
                if (ar.succeeded()) {
                    Future<JsonObject> f4 = insert(Table.SETTINGS, createDataMap(username));
                    f4.setHandler(result -> {
                        if (result.succeeded()) {
                            fut.complete(ar.result().toJson());
                        } else {
                            fut.fail(result.cause());
                        }
                    });
                } else {
                    fut.fail(ar.cause());
                }
                conn.close();
            })));
        });
    }

    /**
     * Inserts a view into views table.
     */
    @Override
    public Future<JsonObject> insertView(String user, String param) {
        JsonObject json = new JsonObject(param);
        System.out.println("-------------------------------------");
        System.out.println(json.encodePrettily());
        System.out.println("-------------------------------------");

        String movieId = json.getString("movieId");
        return future(fut -> client.getConnection(connHandler(fut,
                conn -> conn.updateWithParams(SQL_INSERT_VIEW, new JsonArray()
                        .add(user)
                        .add(movieId)
                        .add(movieDateToDBDate(json.getString("start")))
                        .add(movieDateToDBDate(json.getString("end")))
                        .add(json.getBoolean("wasFirst"))
                        .add(json.getBoolean("wasCinema"))
                        .add(json.getString("comment")), updateHandler(conn, fut)))));
    }

    /**
     * Inserts a movie to movies table.
     */
    @Override
    public Future<JsonObject> insertMovie(int id, String movieTitle, int year, String posterPath) {
        return future(fut -> client.getConnection(connHandler(fut,
                conn -> conn.updateWithParams(SQL_INSERT_MOVIE, new JsonArray()
                        .add(id)
                        .add(movieTitle)
                        .add(year)
                        .add(posterPath), updateHandler(conn, fut)))));
    }

    /**
     * Inserts an entry to wishlist table.
     */
    @Override
    public Future<JsonObject> insertWishlist(String username, int movieId) {
        return future(fut -> client.getConnection(connHandler(fut,
                conn -> conn.updateWithParams(SQL_INSERT_WISHLIST, new JsonArray()
                        .add(username)
                        .add(movieId)
                        .add(System.currentTimeMillis()), updateHandler(conn, fut)))));
    }

    @Override
    public Future<JsonObject> isInWishlist(String username, int movieId) {
        System.out.println("WISHLIST: " + username + ": " + movieId);
        return future(fut -> client.getConnection(connHandler(fut,
                conn -> conn.queryWithParams(SQL_IS_IN_WISHLIST, new JsonArray()
                        .add(username)
                        .add(movieId), resultHandler(conn, fut)))));
    }

    @Override
    public Future<JsonObject> getWishlist(String username) {
        System.out.println("USERNAME: " + username);
        return future(fut -> client.getConnection(DatabaseService.connHandler(fut,
                conn -> conn.queryWithParams(SQL_GET_WISHLIST, new JsonArray().add(username),
                        DatabaseService.resultHandler(conn, fut)))));
    }

    /**
     * Gets settings for a user.
     */
    @Override
    public Future<JsonObject> getSettings(String username) {
        return future(fut -> client.getConnection(connHandler(fut,
                conn -> conn.queryWithParams(SQL_QUERY_SETTINGS, new JsonArray().add(username),
                        resultHandler(conn, fut)))));
    }

    /**
     * Updates data in a table.
     *
     * @param table to update data in
     * @param data  map of columns to update and data to be updated
     * @return future of JsonObject containing update results
     */
    @Override
    public Future<JsonObject> update(Table table, Map<Column, String> data) {
        return future(fut -> {
            if (data.get(USERNAME) == null) {
                fut.fail("Username required.");
                return;
            }
            if (data.size() == 1) {
                fut.fail("No columns specified.");
                return;
            }
            List<Column> columns = getSortedColumns(data);
            client.getConnection(connHandler(fut,
                    conn -> conn.updateWithParams(UPDATE.create(table, columns), getSortedValues(columns, data),
                            updateHandler(conn, fut))));
        });
    }

    /**
     * Inserts data to a table.
     *
     * @param table to insert data to
     * @param data  map of columns to insert to and data to be inserted
     * @return future of JsonObject containing insertion results
     */
    @Override
    public Future<JsonObject> insert(Table table, Map<Column, String> data) {
        return future(fut -> {
            if (data.get(USERNAME) == null) {
                fut.fail("Username required.");
                return;
            }
            List<Column> columns = getSortedColumns(data);
            client.getConnection(connHandler(fut,
                    conn -> conn.updateWithParams(INSERT.create(table, columns), getSortedValues(columns, data),
                            updateHandler(conn, fut))));
        });
    }

    /**
     * Gets data for a single user.
     */
    @Override
    public Future<JsonObject> getUser(String username) {
        return future(fut -> client.getConnection(connHandler(fut,
                conn -> conn.queryWithParams(SQL_QUERY_USER, new JsonArray().add(username),
                        resultHandler(conn, fut)))));
    }

    /**
     * Gets all users.
     */
    @Override
    public Future<JsonObject> getAllUsers() {
        return future(fut -> client.getConnection(connHandler(fut,
                conn -> conn.query(SQL_QUERY_USERS, resultHandler(conn, fut)))));
    }

    /**
     * Gets all movies views for user.
     */
    @Override
    public Future<JsonObject> getViews(String username, String param, int page) {
        JsonObject json = new JsonObject(param);
        json.put("start", formToDBDate(json.getString("start"), false));
        json.put("end", formToDBDate(json.getString("end"), true));
        System.out.println(json.encodePrettily());
        String SQL_QUERY_VIEWS_TEMP = SQL_QUERY_VIEWS;
        if (json.getBoolean("is-first")) {
            SQL_QUERY_VIEWS_TEMP += " AND WasFirst";
        }
        if (json.getBoolean("is-cinema")) {
            SQL_QUERY_VIEWS_TEMP += " AND WasCinema";
        }
        SQL_QUERY_VIEWS_TEMP += " ORDER BY Start DESC LIMIT ?, ?";

        System.out.println("QUERY:" + SQL_QUERY_VIEWS);

        String finalSQL_QUERY_VIEWS_TEMP = SQL_QUERY_VIEWS_TEMP;
        return future(fut -> client.getConnection(connHandler(fut,
                conn -> conn.queryWithParams(finalSQL_QUERY_VIEWS_TEMP, new JsonArray()
                        .add(username)
                        .add(json.getString("start"))
                        .add(json.getString("end"))
                        .add(page * 10)
                        .add(10), resultHandler(conn, fut)))));
    }

    /**
     * Gets a specific movie views for user.
     */
    @Override
    public Future<JsonObject> getMovieViews(String username, String param) {
        return future(fut -> client.getConnection(connHandler(fut,
                conn -> conn.queryWithParams(SQL_GET_MOVIE_VIEWS, new JsonArray()
                        .add(username)
                        .add(param), resultHandler(conn, fut)))));
    }

    /**
     * Gets users count in database.
     */
    @Override
    public Future<String> getUsersCount() {
        return future(fut -> client.getConnection(connHandler(fut, conn -> conn.query(SQL_VIEWS_COUNT, ar -> {
            if (ar.succeeded()) {
                fut.complete(ar.result().getRows().get(0).getLong("Count").toString());
            } else {
                fut.fail(ar.cause());
            }
            conn.close();
        }))));
    }
}
