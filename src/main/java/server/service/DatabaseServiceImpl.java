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

import static io.vertx.core.Future.future;
import static server.service.DatabaseService.Column.*;
import static server.service.DatabaseService.SQLCommand.INSERT;
import static server.service.DatabaseService.SQLCommand.UPDATE;
import static server.service.DatabaseService.*;
import static server.util.CommonUtils.*;
import static server.util.StringUtils.*;

/**
 * Database service implementation.
 */
public class DatabaseServiceImpl implements DatabaseService {
    public static final String SQL_INSERT_WISHLIST =
            "INSERT IGNORE INTO Wishlist (Username, MovieId, Time) VALUES (?, ?, ?)";
    public static final String SQL_INSERT_EPISODE =
            "INSERT IGNORE INTO Series (Username, SeriesId, EpisodeId, SeasonId, Time) VALUES (?, ?, ?, ?, ?)";
    public static final String SQL_IS_IN_WISHLIST =
            "SELECT MovieId FROM Wishlist WHERE Username = ? AND MovieId = ?";
    public static final String SQL_GET_WISHLIST =
            "SELECT Title, Time, Image, MovieId FROM Wishlist " +
                    "JOIN Movies ON Wishlist.MovieId = Movies.Id " +
                    "WHERE Username =  ? ORDER BY Time DESC";
    public static final String SQL_GET_YEARS_DIST =
            "SELECT Year, COUNT(*) AS 'Count' FROM Views " +
                    "JOIN Movies ON Movies.Id = Views.MovieId " +
                    "WHERE Username = ? AND Start >= ? AND Start <= ?";
    public static final String SQL_GET_WEEKDAYS_DIST =
            "SELECT ((DAYOFWEEK(Start) + 5) % 7) AS Day, COUNT(*) AS 'Count' " +
                    "FROM Views " +
                    "WHERE Username = ? AND Start >= ? AND Start <= ?";
    public static final String SQL_GET_TIME_DIST =
            "SELECT HOUR(Start) AS Hour, COUNT(*) AS Count FROM Views " +
                    "WHERE Username = ? AND Start >= ? AND Start <= ? ";
    public static final String SQL_GET_ALL_TIME_META =
            "Select DATE(Min(Start)) AS Start, COUNT(*) AS Count FROM Views " +
                    "WHERE Username = ?";
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseServiceImpl.class);
    private static final String MYSQL = "mysql";
    private static final String SQL_INSERT_USER =
            "INSERT INTO Users (Username, Firstname, Lastname, Password, Salt) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_QUERY_USERS = "SELECT * FROM Users " +
            "JOIN Settings ON Users.Username = Settings.Username";
    private static final String SQL_QUERY_USER = "SELECT * FROM Users " +
            "JOIN Settings ON Users.Username = Settings.Username " +
            "WHERE Users.Username = ?";
    private static final String SQL_INSERT_VIEW =
            "INSERT INTO Views (Username, MovieId, Start, End, WasFirst, WasCinema, Comment) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_QUERY_VIEWS =
            "SELECT Views.Id, MovieId, Title, Start, WasFirst, WasCinema, Image, Comment, TIMESTAMPDIFF(MINUTE, Start, End) AS Runtime " +
                    "FROM Views " +
                    "JOIN Movies ON Views.MovieId = Movies.Id " +
                    "WHERE Username = ? AND Start >= ? AND Start <= ?";
    private static final String SQL_QUERY_SETTINGS = "SELECT * FROM Settings WHERE Username = ?";
    private static final String SQL_INSERT_MOVIE =
            "INSERT IGNORE INTO Movies VALUES (?, ?, ?, ?)";
    private static final String SQL_INSERT_SERIES =
            "INSERT IGNORE INTO SeriesInfo VALUES (?, ?, ?)";
    private static final String SQL_VIEWS_COUNT = "SELECT COUNT(*) AS Count FROM Users";
    private static final String SQL_GET_MOVIE_VIEWS =
            "SELECT Start, WasCinema From Views" +
                    " WHERE Username = ? AND MovieId = ?" +
                    " ORDER BY Start DESC";
    private static final String SQL_QUERY_VIEWS_META =
            "SELECT Count(*) AS Count " +
                    "FROM Views " +
                    "JOIN Movies ON Views.MovieId = Movies.Id " +
                    "WHERE Username = ? AND Start >= ? AND Start <= ?";

    private static final String SQL_REMOVE_VIEW =
            "DELETE FROM Views WHERE Username = ? AND Id = ?";

    private static final String SQL_REMOVE_EPISODE =
            "DELETE FROM Series WHERE Username = ? AND EpisodeId = ?";

    private static final String SQL_GET_SEEN_EPISODES =
            "SELECT EpisodeId FROM Series " +
                    "WHERE Username = ? " +
                    "AND SeriesId = ?";

    private static final String SQL_GET_WATCHING_SERIES =
            "SELECT Title, Image, SeriesId, COUNT(SeriesId) AS Count FROM Series " +
                    "JOIN SeriesInfo ON Series.SeriesId = SeriesInfo.Id " +
                    "WHERE Username = ? " +
                    "GROUP BY Title, Image, SeriesId " +
                    "ORDER BY Title";

    private final JDBCClient client;

    protected DatabaseServiceImpl(Vertx vertx, JsonObject config) {
        this.client = JDBCClient.createShared(vertx, config.getJsonObject(MYSQL));
    }

    /**
     * Inserts a Facebook, Google or IdCard user into database.
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
                check(ar.succeeded(),
                        () -> insert(Table.SETTINGS, createDataMap(username)).setHandler(result ->
                                check(result.succeeded(),
                                        () -> fut.complete(ar.result().toJson()),
                                        () -> fut.fail(result.cause()))),
                        () -> fut.fail(ar.cause()));
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
                        .add(json.getString("comment")), resultHandler(conn, fut)))));
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
                        .add(posterPath), resultHandler(conn, fut)))));
    }

    @Override
    public Future<JsonObject> insertSeries(int id, String seriesTitle, String posterPath) {
        return future(fut -> client.getConnection(connHandler(fut,
                conn -> conn.updateWithParams(SQL_INSERT_SERIES, new JsonArray()
                        .add(id)
                        .add(seriesTitle)
                        .add(posterPath), resultHandler(conn, fut)))));
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
                        .add(System.currentTimeMillis()), resultHandler(conn, fut)))));
    }

    @Override
    public Future<JsonObject> insertEpisodeView(String username, String param) {
        JsonObject json = new JsonObject(param);
        return future(fut -> client.getConnection(connHandler(fut,
                conn -> conn.updateWithParams(SQL_INSERT_EPISODE, new JsonArray()
                        .add(username)
                        .add(json.getInteger("seriesId"))
                        .add(json.getInteger("episodeId"))
                        .add(json.getString("seasonId"))
                        .add(System.currentTimeMillis()), resultHandler(conn, fut)))));

    }

    @Override
    public Future<JsonObject> getSeenEpisodes(String username, int seriesId) {
        return future(fut -> client.getConnection(connHandler(fut,
                conn -> conn.queryWithParams(SQL_GET_SEEN_EPISODES, new JsonArray()
                        .add(username)
                        .add(seriesId), resultHandler(conn, fut)))));
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
        return future(fut -> client.getConnection(connHandler(fut,
                conn -> conn.queryWithParams(SQL_GET_WISHLIST, new JsonArray().add(username),
                        resultHandler(conn, fut)))));
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
                            resultHandler(conn, fut))));
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
                            resultHandler(conn, fut))));
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

    @Override
    public Future<JsonObject> getYearsDist(String username, String param) {
        //GROUP BY Year ORDER BY Year DESC
        JsonObject json = new JsonObject(param);
        json.put("start", formToDBDate(json.getString("start"), false));
        json.put("end", formToDBDate(json.getString("end"), true));
        String SQL_QUERY_VIEWS_TEMP = SQL_GET_YEARS_DIST;
        if (json.getBoolean("is-first")) {
            SQL_QUERY_VIEWS_TEMP += " AND WasFirst";
        }
        if (json.getBoolean("is-cinema")) {
            SQL_QUERY_VIEWS_TEMP += " AND WasCinema";
        }

        SQL_QUERY_VIEWS_TEMP += " GROUP BY Year ORDER BY Year DESC";
        String SQL_QUERY_VIEWS_TEMP_FINAL = SQL_QUERY_VIEWS_TEMP;
        return future(fut -> client.getConnection(connHandler(fut,
                conn -> conn.queryWithParams(SQL_QUERY_VIEWS_TEMP_FINAL, new JsonArray()
                        .add(username)
                        .add(json.getString("start"))
                        .add(json.getString("end")), resultHandler(conn, fut)))));
    }

    @Override
    public Future<JsonObject> getWeekdaysDist(String username, String param) {
        //"GROUP BY Day ORDER BY Day"
        JsonObject json = new JsonObject(param);
        json.put("start", formToDBDate(json.getString("start"), false));
        json.put("end", formToDBDate(json.getString("end"), true));
        String SQL_QUERY_VIEWS_TEMP = SQL_GET_WEEKDAYS_DIST;
        if (json.getBoolean("is-first")) {
            SQL_QUERY_VIEWS_TEMP += " AND WasFirst";
        }
        if (json.getBoolean("is-cinema")) {
            SQL_QUERY_VIEWS_TEMP += " AND WasCinema";
        }
        SQL_QUERY_VIEWS_TEMP += " GROUP BY Day ORDER BY Day";
        String SQL_QUERY_VIEWS_TEMP_FINAL = SQL_QUERY_VIEWS_TEMP;
        return future(fut -> client.getConnection(connHandler(fut,
                conn -> conn.queryWithParams(SQL_QUERY_VIEWS_TEMP_FINAL, new JsonArray()
                        .add(username)
                        .add(json.getString("start"))
                        .add(json.getString("end")), resultHandler(conn, fut)))));
    }

    @Override
    public Future<JsonObject> getTimeDist(String username, String param) {
        //"GROUP BY Hour";
        JsonObject json = new JsonObject(param);
        json.put("start", formToDBDate(json.getString("start"), false));
        json.put("end", formToDBDate(json.getString("end"), true));
        String SQL_GET_TIME_DIST_TEMP = SQL_GET_TIME_DIST;
        if (json.getBoolean("is-first")) {
            SQL_GET_TIME_DIST_TEMP += " AND WasFirst";
        }
        if (json.getBoolean("is-cinema")) {
            SQL_GET_TIME_DIST_TEMP += " AND WasCinema";
        }
        SQL_GET_TIME_DIST_TEMP += " GROUP BY Hour";
        String SQL_GET_TIME_DIST_TEMP_FINAL = SQL_GET_TIME_DIST_TEMP;
        return future(fut -> client.getConnection(connHandler(fut,
                conn -> conn.queryWithParams(SQL_GET_TIME_DIST_TEMP_FINAL, new JsonArray()
                        .add(username)
                        .add(json.getString("start"))
                        .add(json.getString("end")), resultHandler(conn, fut)))));
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

    @Override
    public Future<JsonObject> getAllTimeMeta(String username, String param) {
        JsonObject json = new JsonObject(param);

        String SQL_GET_ALL_TIME_META_TEMP = SQL_GET_ALL_TIME_META;
        if (json.getBoolean("is-first")) {
            SQL_GET_ALL_TIME_META_TEMP += " AND WasFirst";
        }
        if (json.getBoolean("is-cinema")) {
            SQL_GET_ALL_TIME_META_TEMP += " AND WasCinema";
        }

        String SQL_GET_ALL_TIME_META_TEMP_FINAL = SQL_GET_ALL_TIME_META_TEMP;
        return future(fut -> client.getConnection(connHandler(fut,
                conn -> conn.queryWithParams(SQL_GET_ALL_TIME_META_TEMP_FINAL, new JsonArray()
                        .add(username), resultHandler(conn, fut)))));
    }

    @Override
    public Future<JsonObject> getViewsMeta(String username, String param) {
        JsonObject json = new JsonObject(param);
        json.put("start", formToDBDate(json.getString("start"), false));
        json.put("end", formToDBDate(json.getString("end"), true));
        String SQL_QUERY_VIEWS_TEMP = SQL_QUERY_VIEWS_META;
        if (json.getBoolean("is-first")) {
            SQL_QUERY_VIEWS_TEMP += " AND WasFirst";
        }
        if (json.getBoolean("is-cinema")) {
            SQL_QUERY_VIEWS_TEMP += " AND WasCinema";
        }

        String finalSQL_QUERY_VIEWS_TEMP = SQL_QUERY_VIEWS_TEMP;
        return future(fut -> client.getConnection(connHandler(fut,
                conn -> conn.queryWithParams(finalSQL_QUERY_VIEWS_TEMP, new JsonArray()
                        .add(username)
                        .add(json.getString("start"))
                        .add(json.getString("end")), resultHandler(conn, fut)))));
    }

    @Override
    public Future<JsonObject> removeView(String username, String param) {
        return future(fut -> client.getConnection(connHandler(fut,
                conn -> conn.updateWithParams(SQL_REMOVE_VIEW, new JsonArray()
                        .add(username)
                        .add(param), resultHandler(conn, fut)))));
    }

    @Override
    public Future<JsonObject> removeEpisode(String username, String param) {
        return future(fut -> client.getConnection(connHandler(fut,
                conn -> conn.updateWithParams(SQL_REMOVE_EPISODE, new JsonArray()
                        .add(username)
                        .add(param), resultHandler(conn, fut)))));
    }

    @Override
    public Future<JsonObject> getWatchingSeries(String username) {
        return future(fut -> client.getConnection(connHandler(fut,
                conn -> conn.queryWithParams(SQL_GET_WATCHING_SERIES, new JsonArray()
                        .add(username), resultHandler(conn, fut)))));
    }

    /**
     * Gets users count in database.
     */
    @Override
    public Future<String> getUsersCount() {
        return future(fut -> client.getConnection(connHandler(fut, conn -> conn.query(SQL_VIEWS_COUNT, ar -> {
            check(ar.succeeded(),
                    () -> fut.complete(ar.result().getRows().get(0).getLong("Count").toString()),
                    () -> fut.fail(ar.cause()));
            conn.close();
        }))));
    }
}
