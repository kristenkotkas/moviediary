package server.service;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import rx.Observable;
import rx.Single;

import java.util.Map;

import static io.vertx.core.logging.LoggerFactory.getLogger;
import static io.vertx.rxjava.core.Future.future;
import static java.lang.System.currentTimeMillis;
import static rx.Statement.ifThen;
import static server.service.DatabaseService.Column.*;
import static server.service.DatabaseService.SQLCommand.INSERT;
import static server.service.DatabaseService.SQLCommand.UPDATE;
import static server.service.DatabaseService.createDataMap;
import static server.util.CommonUtils.*;
import static server.util.StringUtils.*;

/**
 * Database service implementation.
 */
public class DatabaseServiceImpl implements DatabaseService {
    private static final Logger LOG = getLogger(DatabaseServiceImpl.class);
    private static final String SQL_INSERT_WISHLIST =
            "INSERT IGNORE INTO Wishlist (Username, MovieId, Time) VALUES (?, ?, ?)";
    private static final String SQL_INSERT_EPISODE =
            "INSERT IGNORE INTO Series (Username, SeriesId, EpisodeId, SeasonId, Time) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_IS_IN_WISHLIST =
            "SELECT MovieId FROM Wishlist WHERE Username = ? AND MovieId = ?";
    private static final String SQL_GET_WISHLIST =
            "SELECT Title, Time, Image, MovieId FROM Wishlist " +
                    "JOIN Movies ON Wishlist.MovieId = Movies.Id " +
                    "WHERE Username =  ? ORDER BY Time DESC";
    private static final String SQL_GET_YEARS_DIST =
            "SELECT Year, COUNT(*) AS 'Count' FROM Views " +
                    "JOIN Movies ON Movies.Id = Views.MovieId " +
                    "WHERE Username = ? AND Start >= ? AND Start <= ?";
    private static final String SQL_GET_WEEKDAYS_DIST =
            "SELECT ((DAYOFWEEK(Start) + 5) % 7) AS Day, COUNT(*) AS 'Count' " +
                    "FROM Views " +
                    "WHERE Username = ? AND Start >= ? AND Start <= ?";
    private static final String SQL_GET_TIME_DIST =
            "SELECT HOUR(Start) AS Hour, COUNT(*) AS Count FROM Views " +
                    "WHERE Username = ? AND Start >= ? AND Start <= ? ";
    private static final String SQL_GET_ALL_TIME_META =
            "Select DATE(Min(Start)) AS Start, COUNT(*) AS Count FROM Views " +
                    "WHERE Username = ?";
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
    private static final String SQL_USERS_COUNT = "SELECT COUNT(*) AS Count FROM Users";
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

    private static final String SQL_GET_SEEN_EPISODES =
            "SELECT EpisodeId FROM Series " +
                    "WHERE Username = ? " +
                    "AND SeriesId = ?";

    private final JDBCClient client;

    protected DatabaseServiceImpl(Vertx vertx, JsonObject config) {
        this.client = JDBCClient.createShared(vertx, config.getJsonObject(MYSQL));
    }

    /**
     * Inserts a Facebook, Google or IdCard user into database.
     */
    @Override
    public Future<JsonObject> insertUser(String username, String password, String firstname, String lastname) {
        return future(fut -> ifThen(() -> !nonNull(username, password, firstname, lastname) ||
                        contains("", username, firstname, lastname),
                Observable.just(genString()),
                Observable.error(new Throwable("Email, firstname and lastname must exist!")))
                .toSingle()
                .flatMap(salt -> client.rxGetConnection()
                        .flatMap(conn -> conn
                                .rxUpdateWithParams(SQL_INSERT_USER, new JsonArray()
                                        .add(username)
                                        .add(firstname)
                                        .add(lastname)
                                        .add(hash(password, salt))
                                        .add(salt))
                                .doAfterTerminate(conn::close)))
                .map(UpdateResult::toJson)
                .doOnError(fut::fail)
                .subscribe(res -> insert(Table.SETTINGS, createDataMap(username))
                        .rxSetHandler()
                        .doOnError(fut::fail)
                        .subscribe(result -> fut.complete(res))));




 /*       return future(fut -> check(!nonNull(username, password, firstname, lastname) ||
                        contains("", username, firstname, lastname),
                () -> fut.fail(new Throwable("Email, firstname and lastname must exist!")),
                () -> ifPresent(genString(), salt -> client.rxGetConnection()
                        .flatMap(conn -> conn
                                .rxUpdateWithParams(SQL_INSERT_USER, new JsonArray()
                                        .add(username)
                                        .add(firstname)
                                        .add(lastname)
                                        .add(hash(password, salt))
                                        .add(salt))
                                .doAfterTerminate(conn::close))
                        .map(UpdateResult::toJson)
                        .subscribe(res -> insert(Table.SETTINGS, createDataMap(username))
                                .rxSetHandler()
                                .subscribe(result -> fut.complete(res), fut::fail), fut::fail))));*/
    }

    /**
     * Inserts a view into views table.
     */
    @Override
    public Future<JsonObject> insertView(String user, String param) {
        return future(fut -> ifPresent(new JsonObject(param), json -> client.rxGetConnection()
                .flatMap(conn -> conn
                        .rxUpdateWithParams(SQL_INSERT_VIEW, new JsonArray()
                                .add(user)
                                .add(json.getString("movieId"))
                                .add(movieDateToDBDate(json.getString("start")))
                                .add(movieDateToDBDate(json.getString("end")))
                                .add(json.getBoolean("wasFirst"))
                                .add(json.getBoolean("wasCinema"))
                                .add(json.getString("comment")))
                        .doAfterTerminate(conn::close))
                .map(UpdateResult::toJson)
                .subscribe(fut::complete, fut::fail)));
    }

    /**
     * Inserts a movie to movies table.
     */
    @Override
    public Future<JsonObject> insertMovie(int id, String movieTitle, int year, String posterPath) {
        return future(fut -> client.rxGetConnection()
                .flatMap(conn -> conn
                        .rxUpdateWithParams(SQL_INSERT_MOVIE, new JsonArray()
                                .add(id)
                                .add(movieTitle)
                                .add(posterPath)
                                .add(posterPath))
                        .doAfterTerminate(conn::close))
                .map(UpdateResult::toJson)
                .subscribe(fut::complete, fut::fail));
    }

    @Override
    public Future<JsonObject> insertSeries(int id, String seriesTitle, String posterPath) {
        return future(fut -> client.rxGetConnection()
                .flatMap(conn -> conn
                        .rxUpdateWithParams(SQL_INSERT_SERIES, new JsonArray()
                                .add(id)
                                .add(seriesTitle)
                                .add(posterPath))
                        .doAfterTerminate(conn::close))
                .map(UpdateResult::toJson)
                .subscribe(fut::complete, fut::fail));
    }

    /**
     * Inserts an entry to wishlist table.
     */
    @Override
    public Future<JsonObject> insertWishlist(String username, int movieId) {
        return future(fut -> client.rxGetConnection()
                .flatMap(conn -> conn
                        .rxUpdateWithParams(SQL_INSERT_WISHLIST, new JsonArray()
                                .add(username)
                                .add(movieId)
                                .add(currentTimeMillis()))
                        .doAfterTerminate(conn::close))
                .map(UpdateResult::toJson)
                .subscribe(fut::complete, fut::fail));
    }

    @Override
    public Future<JsonObject> insertEpisodeView(String username, String param) {
        return future(fut -> ifPresent(new JsonObject(param), json -> client.rxGetConnection()
                .flatMap(conn -> conn
                        .rxUpdateWithParams(SQL_INSERT_EPISODE, new JsonArray()
                                .add(username)
                                .add(json.getInteger("seriesId"))
                                .add(json.getInteger("episodeId"))
                                .add(json.getString("seasonId"))
                                .add(currentTimeMillis()))
                        .doAfterTerminate(conn::close))
                .map(UpdateResult::toJson)
                .subscribe(fut::complete, fut::fail)));
    }

    @Override
    public Future<JsonObject> getSeenEpisodes(String username, int seriesId) {
        return future(fut -> client.rxGetConnection()
                .flatMap(conn -> conn
                        .rxQueryWithParams(SQL_GET_SEEN_EPISODES, new JsonArray()
                                .add(username)
                                .add(seriesId))
                        .doAfterTerminate(conn::close))
                .map(ResultSet::toJson)
                .subscribe(fut::complete, fut::fail));
    }

    @Override
    public Future<JsonObject> isInWishlist(String username, int movieId) {
        return future(fut -> client.rxGetConnection()
                .flatMap(conn -> conn
                        .rxQueryWithParams(SQL_IS_IN_WISHLIST, new JsonArray()
                                .add(username)
                                .add(movieId))
                        .doAfterTerminate(conn::close))
                .map(ResultSet::toJson)
                .subscribe(fut::complete, fut::fail));
    }

    @Override
    public Future<JsonObject> getWishlist(String username) {
        return future(fut -> client.rxGetConnection()
                .flatMap(conn -> conn
                        .rxQueryWithParams(SQL_GET_WISHLIST, new JsonArray()
                                .add(username))
                        .doAfterTerminate(conn::close))
                .map(ResultSet::toJson)
                .subscribe(fut::complete, fut::fail));
    }

    /**
     * Gets settings for a user.
     */
    @Override
    public Future<JsonObject> getSettings(String username) {
        return future(fut -> client.rxGetConnection()
                .flatMap(conn -> conn
                        .rxQueryWithParams(SQL_QUERY_SETTINGS, new JsonArray()
                                .add(username))
                        .doAfterTerminate(conn::close))
                .map(ResultSet::toJson)
                .subscribe(fut::complete, fut::fail));
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
        return future(fut -> check(data.get(USERNAME) == null,
                () -> fut.fail("Username required."),
                () -> check(data.size() == 1,
                        () -> fut.fail("No columns specified."),
                        () -> getSortedColumns(data, columns -> client.rxGetConnection()
                                .flatMap(conn -> conn
                                        .rxUpdateWithParams(UPDATE
                                                .create(table, columns), getSortedValues(columns, data))
                                        .doAfterTerminate(conn::close))
                                .map(UpdateResult::toJson)
                                .subscribe(fut::complete, fut::fail)))));
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
        return future(fut -> check(data.get(USERNAME) == null,
                () -> fut.fail("Username required."),
                () -> getSortedColumns(data, columns -> client.rxGetConnection()
                        .flatMap(conn -> conn
                                .rxUpdateWithParams(INSERT.create(table, columns), getSortedValues(columns, data))
                                .doAfterTerminate(conn::close))
                        .map(UpdateResult::toJson)
                        .subscribe(fut::complete, fut::fail))));
    }

    /**
     * Gets data for a single user.
     */
    @Override
    public Future<JsonObject> getUser(String username) {
        return future(fut -> client.rxGetConnection()
                .flatMap(conn -> conn
                        .rxQueryWithParams(SQL_QUERY_USER, new JsonArray()
                                .add(username))
                        .doAfterTerminate(conn::close))
                .map(ResultSet::toJson)
                .subscribe(fut::complete, fut::fail));
    }

    /**
     * Gets all users.
     */
    @Override
    public Future<JsonObject> getAllUsers() {
        return future(fut -> client.rxGetConnection()
                .flatMap(conn -> conn
                        .rxQuery(SQL_QUERY_USERS)
                        .doAfterTerminate(conn::close))
                .map(ResultSet::toJson)
                .subscribe(fut::complete, fut::fail));
    }

    /**
     * Gets all movies views for user.
     */
    @Override
    public Future<JsonObject> getViews(String username, String param, int page) {
        JsonObject json = new JsonObject(param);
        json.put("start", formToDBDate(json.getString("start"), false));
        json.put("end", formToDBDate(json.getString("end"), true));
        StringBuilder sb = new StringBuilder(SQL_QUERY_VIEWS);
        if (json.getBoolean("is-first")) {
            sb.append(" AND WasFirst");
        }
        if (json.getBoolean("is-cinema")) {
            sb.append(" AND WasCinema");
        }
        return future(fut -> client.rxGetConnection()
                .flatMap(conn -> conn
                        .rxQueryWithParams(sb.append(" ORDER BY Start DESC LIMIT ?, ?").toString(), new JsonArray()
                                .add(username)
                                .add(json.getString("start"))
                                .add(json.getString("end"))
                                .add(page * 10)
                                .add(10))
                        .doAfterTerminate(conn::close))
                .map(ResultSet::toJson)
                .subscribe(fut::complete, fut::fail));
    }

    @Override
    public Future<JsonObject> getYearsDist(String username, String param) {
        //GROUP BY Year ORDER BY Year DESC
        JsonObject json = new JsonObject(param);
        json.put("start", formToDBDate(json.getString("start"), false));
        json.put("end", formToDBDate(json.getString("end"), true));
        StringBuilder sb = new StringBuilder(SQL_GET_YEARS_DIST);
        if (json.getBoolean("is-first")) {
            sb.append(" AND WasFirst");
        }
        if (json.getBoolean("is-cinema")) {
            sb.append(" AND WasCinema");
        }
        return future(fut -> client.rxGetConnection()
                .flatMap(conn -> conn
                        .rxQueryWithParams(sb.append(" GROUP BY Year ORDER BY Year DESC").toString(), new JsonArray()
                                .add(username)
                                .add(json.getString("start"))
                                .add(json.getString("end")))
                        .doAfterTerminate(conn::close))
                .map(ResultSet::toJson)
                .subscribe(fut::complete, fut::fail));
    }

    @Override
    public Future<JsonObject> getWeekdaysDist(String username, String param) {
        //"GROUP BY Day ORDER BY Day"
        JsonObject json = new JsonObject(param);
        json.put("start", formToDBDate(json.getString("start"), false));
        json.put("end", formToDBDate(json.getString("end"), true));
        StringBuilder sb = new StringBuilder(SQL_GET_WEEKDAYS_DIST);
        if (json.getBoolean("is-first")) {
            sb.append(" AND WasFirst");
        }
        if (json.getBoolean("is-cinema")) {
            sb.append(" AND WasCinema");
        }
        return future(fut -> client.rxGetConnection()
                .flatMap(conn -> conn
                        .rxQueryWithParams(sb.append(" GROUP BY Day ORDER BY Day").toString(), new JsonArray()
                                .add(username)
                                .add(json.getString("start"))
                                .add(json.getString("end")))
                        .doAfterTerminate(conn::close))
                .map(ResultSet::toJson)
                .subscribe(fut::complete, fut::fail));
    }

    @Override
    public Future<JsonObject> getTimeDist(String username, String param) {
        //"GROUP BY Hour";
        JsonObject json = new JsonObject(param);
        json.put("start", formToDBDate(json.getString("start"), false));
        json.put("end", formToDBDate(json.getString("end"), true));
        StringBuilder sb = new StringBuilder(SQL_GET_TIME_DIST);
        if (json.getBoolean("is-first")) {
            sb.append(" AND WasFirst");
        }
        if (json.getBoolean("is-cinema")) {
            sb.append(" AND WasCinema");
        }
        return future(fut -> client.rxGetConnection()
                .flatMap(conn -> conn
                        .rxQueryWithParams(sb.append(" GROUP BY Hour").toString(), new JsonArray()
                                .add(username)
                                .add(json.getString("start"))
                                .add(json.getString("end")))
                        .doAfterTerminate(conn::close))
                .map(ResultSet::toJson)
                .subscribe(fut::complete, fut::fail));
    }

    /**
     * Gets a specific movie views for user.
     */
    @Override
    public Future<JsonObject> getMovieViews(String username, String param) {
        return future(fut -> client.rxGetConnection()
                .flatMap(conn -> conn
                        .rxQueryWithParams(SQL_GET_MOVIE_VIEWS, new JsonArray()
                                .add(username)
                                .add(param))
                        .doAfterTerminate(conn::close))
                .map(ResultSet::toJson)
                .subscribe(fut::complete, fut::fail));
    }

    @Override
    public Future<JsonObject> getAllTimeMeta(String username, String param) {
        JsonObject json = new JsonObject(param);
        StringBuilder sb = new StringBuilder(SQL_GET_ALL_TIME_META);
        if (json.getBoolean("is-first")) {
            sb.append(" AND WasFirst");
        }
        if (json.getBoolean("is-cinema")) {
            sb.append(" AND WasCinema");
        }
        return future(fut -> client.rxGetConnection()
                .flatMap(conn -> conn
                        .rxQueryWithParams(sb.toString(), new JsonArray()
                                .add(username))
                        .doAfterTerminate(conn::close))
                .map(ResultSet::toJson)
                .subscribe(fut::complete, fut::fail));
    }

    @Override
    public Future<JsonObject> getViewsMeta(String username, String param) {
        return future(fut -> Single.just(new JsonObject(param))
                .map(json -> json.put("start", formToDBDate(json.getString("start"), false)))
                .map(json -> json.put("end", formToDBDate(json.getString("end"), true)))
                .subscribe(json -> Single.just(new StringBuilder(SQL_QUERY_VIEWS_META))
                        .doOnEach(sb -> ifTrue(json.getBoolean("is-first"),
                                () -> sb.getValue().append(" AND WasFirst")))
                        .doOnEach(sb -> ifTrue(json.getBoolean("is-cinema"),
                                () -> sb.getValue().append(" AND WasCinema")))
                        .subscribe(sb -> client.rxGetConnection()
                                .flatMap(conn -> conn
                                        .rxQueryWithParams(sb.toString(), new JsonArray()
                                                .add(username)
                                                .add(json.getString("start"))
                                                .add(json.getString("end")))
                                        .doAfterTerminate(conn::close))
                                .map(ResultSet::toJson)
                                .subscribe(fut::complete, fut::fail))));
/*        JsonObject json = new JsonObject(param);
        json.put("start", formToDBDate(json.getString("start"), false));
        json.put("end", formToDBDate(json.getString("end"), true));
        StringBuilder sb = new StringBuilder(SQL_QUERY_VIEWS_META);
        if (json.getBoolean("is-first")) {
            sb.append(" AND WasFirst");
        }
        if (json.getBoolean("is-cinema")) {
            sb.append(" AND WasCinema");
        }
        return future(fut -> client
                .rxGetConnection()
                .flatMap(conn -> conn
                        .rxQueryWithParams(sb.toString(), new JsonArray()
                                .add(username)
                                .add(json.getString("start"))
                                .add(json.getString("end")))
                        .doAfterTerminate(conn::close))
                .map(ResultSet::toJson)
                .subscribe(fut::complete, fut::fail));*/
    }

    @Override
    public Future<JsonObject> removeView(String username, String param) {
        return future(fut -> client.rxGetConnection()
                .flatMap(conn -> conn
                        .rxUpdateWithParams(SQL_REMOVE_VIEW, new JsonArray()
                                .add(username)
                                .add(param))
                        .doAfterTerminate(conn::close))
                .map(UpdateResult::toJson)
                .subscribe(fut::complete, fut::fail));
    }

    /**
     * Gets users count in database.
     */
    @Override
    public Future<String> getUsersCount() {
        return future(fut -> client.rxGetConnection()
                .flatMap(conn -> conn
                        .rxQuery(SQL_USERS_COUNT)
                        .doAfterTerminate(conn::close))
                .map(resultSet -> resultSet.getRows().get(0).getLong("Count").toString())
                .subscribe(fut::complete, fut::fail));
    }
}
