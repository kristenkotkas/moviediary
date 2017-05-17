package server.service;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import server.verticle.ServerVerticle;

import java.util.List;
import java.util.Map;

import static io.vertx.rxjava.core.Future.failedFuture;
import static io.vertx.rxjava.core.Future.future;
import static java.lang.System.currentTimeMillis;
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
    private static final String SQL_INSERT_WISHLIST =
            "INSERT IGNORE INTO Wishlist (Username, MovieId, Time) VALUES (?, ?, ?)";
    private static final String SQL_INSERT_EPISODE =
            "INSERT IGNORE INTO Series (Username, SeriesId, EpisodeId, SeasonId, Time) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_IS_IN_WISHLIST =
            "SELECT MovieId FROM Wishlist WHERE Username = ? AND MovieId = ?";
    private static final String SQL_GET_WISHLIST =
            "SELECT Title, Time, Year, Image, MovieId FROM Wishlist " +
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
    private static final String SQL_GET_MONTH_YEAR_DIST =
            "SELECT MONTH(Start) AS Month, YEAR(Start) AS Year, COUNT(MONTHNAME(Start)) AS Count " +
                    "FROM Views " +
                    "WHERE Username = ? AND Start >= ? AND Start <= ? ";
    private static final String SQL_GET_ALL_TIME_META =
            "SELECT DATE(Min(Start)) AS Start, COUNT(*) AS Count, SUM(TIMESTAMPDIFF(MINUTE, Start, End)) AS Runtime FROM Views " +
                    "WHERE Username = ?";
    private static final String SQL_INSERT_USER =
            "INSERT INTO Users (Username, Firstname, Lastname, Password, Salt) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_QUERY_USERS = "SELECT * FROM Users " +
            "JOIN Settings ON Users.Username = Settings.Username";
    private static final String SQL_QUERY_USER = "SELECT * FROM Users " +
            "JOIN Settings ON Users.Username = Settings.Username " +
            "WHERE Users.Username = ?";
    private static final String SQL_INSERT_VIEW =
            "INSERT INTO Views (Username, MovieId, Start, End, WasFirst, WasCinema, Comment) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_QUERY_VIEWS =
            "SELECT Views.Id, MovieId, Title, Start, WasFirst, WasCinema, Image, Comment, " +
                    "TIMESTAMPDIFF(MINUTE, Start, End) AS Runtime " +
                    "FROM Views " +
                    "JOIN Movies ON Views.MovieId = Movies.Id " +
                    "WHERE Username = ? AND Start >= ? AND Start <= ?";
    private static final String SQL_GET_TOP_MOVIES_STAT =
            "SELECT MovieId, Title, COUNT(*) AS Count, Image FROM Views " +
                    "JOIN Movies ON Movies.Id = Views.MovieId " +
                    "WHERE Username = ? AND Start >= ? AND Start <= ?";
    private static final String SQL_QUERY_SETTINGS = "SELECT * FROM Settings WHERE Username = ?";
    private static final String SQL_INSERT_MOVIE =
            "INSERT IGNORE INTO Movies VALUES (?, ?, ?, ?)";
    private static final String SQL_INSERT_SERIES =
            "INSERT IGNORE INTO SeriesInfo VALUES (?, ?, ?)";
    private static final String SQL_USERS_COUNT = "SELECT COUNT(*) AS Count FROM Users";
    private static final String SQL_GET_MOVIE_VIEWS =
            "SELECT Id, Start, WasCinema FROM Views" +
                    " WHERE Username = ? AND MovieId = ?" +
                    " ORDER BY Start DESC";
    private static final String SQL_QUERY_VIEWS_META =
            "SELECT Count(*) AS Count, SUM(TIMESTAMPDIFF(MINUTE, Start, End)) AS Runtime " +
                    "FROM Views " +
                    "JOIN Movies ON Views.MovieId = Movies.Id " +
                    "WHERE Username = ? AND Start >= ? AND Start <= ?";
    private static final String SQL_REMOVE_VIEW =
            "DELETE FROM Views WHERE Username = ? AND Id = ?";
    private static final String SQL_REMOVE_EPISODE =
            "DELETE FROM Series WHERE Username = ? AND EpisodeId = ?";
    private static final String SQL_GET_SEEN_EPISODES = "SELECT EpisodeId FROM Series " +
            "WHERE Username = ? AND SeriesId = ?";
    private static final String SQL_GET_WATCHING_SERIES =
            "SELECT Title, Image, SeriesId, COUNT(SeriesId) AS Count FROM Series " +
                    "JOIN SeriesInfo ON Series.SeriesId = SeriesInfo.Id " +
                    "WHERE Username = ? " +
                    "GROUP BY Title, Image, SeriesId " +
                    "ORDER BY Title";
    private static final String SQL_REMOVE_WISHLIST =
            "DELETE FROM Wishlist WHERE Username = ? AND MovieId = ?";
    private static final String SQL_GET_LAST_VIEWS =
            "SELECT Title, Start, MovieId, WEEKDAY(Start) AS 'week_day', WasCinema FROM Views " +
                    "JOIN Movies ON Movies.Id = Views.MovieId " +
                    "WHERE Username = ? " +
                    "ORDER BY Start DESC LIMIT 5";
    private static final String SQL_GET_HOME_WISHLIST =
            "SELECT Title, Time, Year, MovieId FROM Wishlist " +
                    "JOIN Movies ON Movies.Id = Wishlist.MovieId " +
                    "WHERE Username = ? " +
                    "ORDER BY Time DESC LIMIT 5";
    private static final String SQL_GET_TOP_MOVIES =
            "SELECT MovieId, Title, COUNT(*) AS Count, Image FROM Views " +
                    "JOIN Movies ON Movies.Id = Views.MovieId " +
                    "WHERE Username = ? " +
                    "GROUP BY MovieId ORDER BY Count DESC LIMIT 5";
    private static final String SQL_GET_TOTAL_MOVIE_COUNT =
            "SELECT COUNT(*) AS 'total_movies', SUM(TIMESTAMPDIFF(MINUTE, Start, End)) AS Runtime FROM Views " +
                    "WHERE Username = ?";
    private static final String SQL_GET_TOTAL_CINEMA_COUNT =
            "SELECT COUNT(*) AS 'total_cinema' FROM Views " +
                    "WHERE Username = ? AND WasCinema = 1";
    private static final String SQL_GET_NEW_MOVIE_COUNT =
            "SELECT COUNT(WasFirst) AS 'new_movies' FROM Views " +
                    "WHERE Username = ? " +
                    "AND WasFirst = 1";
    private static final String SQL_GET_TOTAL_RUNTIME =
            "SELECT SUM(TIMESTAMPDIFF(MINUTE, Start, End)) AS Runtime FROM Views " +
                    "WHERE Username = ?";
    private static final String SQL_GET_TOTAL_DISTINCT_MOVIES =
            "SELECT COUNT(DISTINCT MovieId) AS 'unique_movies' FROM Views " +
                    "WHERE Username = ?";
    private static final String SQL_INSERT_SEASON =
            "INSERT IGNORE INTO Series (Username, SeriesId, EpisodeId, SeasonId, Time) VALUES";
    private static final String  SQL_REMOVE_SEASON =
            "DELETE FROM Series WHERE Username = ? AND SeasonId = ?;";

    private final JDBCClient client;
    private final ServerVerticle verticle;

    protected DatabaseServiceImpl(Vertx vertx, JsonObject config, ServerVerticle verticle) {
        this.client = JDBCClient.createShared(vertx, config.getJsonObject("mysql"));
        this.verticle = verticle;
    }

    private Future<JsonObject> query(String sql, JsonArray params) {
        return future(fut -> client.rxGetConnection()
                .flatMap(conn -> conn.rxQueryWithParams(sql, params).doAfterTerminate(conn::close))
                .map(ResultSet::toJson)
                .subscribe(fut::complete, fut::fail));
    }

    private Future<JsonObject> updateOrInsert(String sql, JsonArray params) {
        return future(fut -> client.rxGetConnection()
                .flatMap(conn -> conn.rxUpdateWithParams(sql, params).doAfterTerminate(conn::close))
                .map(UpdateResult::toJson)
                .subscribe(fut::complete, fut::fail));
    }

    /**
     * Inserts a Facebook, Google or IdCard user into database.
     */
    @Override
    public Future<JsonObject> insertUser(String username, String password, String firstname, String lastname) {
        if (!nonNull(username, password, firstname, lastname) || contains("", username, firstname, lastname)) {
            return failedFuture("Email, firstname and lastname must exist!");
        }
        return future(fut -> ifPresent(genString(), salt -> updateOrInsert(SQL_INSERT_USER, new JsonArray()
                .add(username)
                .add(firstname)
                .add(lastname)
                .add(hash(password, salt))
                .add(salt)).rxSetHandler()
                .doOnError(fut::fail)
                .subscribe(res -> insert(Table.SETTINGS, createDataMap(username)).rxSetHandler()
                        .subscribe(result -> fut.complete(res), fut::fail))));
    }

    /**
     * Inserts a view into views table.
     */
    @Override
    public Future<JsonObject> insertView(String user, String jsonParam) {
        JsonObject json = new JsonObject(jsonParam);
        return updateOrInsert(SQL_INSERT_VIEW, new JsonArray()
                .add(user)
                .add(json.getString("movieId"))
                .add(movieDateToDBDate(json.getString("start")))
                .add(movieDateToDBDate(json.getString("end")))
                .add(json.getBoolean("wasFirst"))
                .add(json.getBoolean("wasCinema"))
                .add(json.getString("comment")));
    }

    /**
     * Inserts a movie to movies table.
     */
    @Override
    public Future<JsonObject> insertMovie(int id, String movieTitle, int year, String posterPath) {
        return updateOrInsert(SQL_INSERT_MOVIE, new JsonArray()
                .add(id)
                .add(movieTitle)
                .add(year)
                .add(posterPath));
    }

    @Override
    public Future<JsonObject> insertSeries(int id, String seriesTitle, String posterPath) {
        return updateOrInsert(SQL_INSERT_SERIES, new JsonArray()
                .add(id)
                .add(seriesTitle)
                .add(posterPath));
    }

    /**
     * Inserts an entry to wishlist table.
     */
    @Override
    public Future<JsonObject> insertWishlist(String username, int movieId) {
        return updateOrInsert(SQL_INSERT_WISHLIST, new JsonArray()
                .add(username)
                .add(movieId)
                .add(currentTimeMillis()));
    }

    @Override
    public Future<JsonObject> insertEpisodeView(String username, String jsonParam) {
        JsonObject json = new JsonObject(jsonParam);
        return updateOrInsert(SQL_INSERT_EPISODE, new JsonArray()
                .add(username)
                .add(json.getInteger("seriesId"))
                .add(json.getInteger("episodeId"))
                .add(json.getString("seasonId"))
                .add(currentTimeMillis()));
    }

    @Override
    public Future<JsonObject> getSeenEpisodes(String username, int seriesId) {
        return query(SQL_GET_SEEN_EPISODES, new JsonArray().add(username).add(seriesId));
    }

    @Override
    public Future<JsonObject> isInWishlist(String username, int movieId) {
        return query(SQL_IS_IN_WISHLIST, new JsonArray().add(username).add(movieId));
    }

    @Override
    public Future<JsonObject> getWishlist(String username) {
        return query(SQL_GET_WISHLIST, new JsonArray().add(username));
    }

    /**
     * Gets settings for a user.
     */
    @Override
    public Future<JsonObject> getSettings(String username) {
        return query(SQL_QUERY_SETTINGS, new JsonArray().add(username));
    }

    /**
     * Updates data in a table.
     *
     * @param table to update data in
     * @param data  map of columns to update and data to be updated
     * @return future of JsonObject containing update results
     */
    @Override
    public Future<JsonObject> update(Table table, Map<Column, String> data) { // TODO: 25.04.2017 test
        if (data.get(USERNAME) == null) {
            return failedFuture("Username required.");
        } else if (data.size() == 1) {
            return failedFuture("No columns specified.");
        }
        List<Column> columns = getSortedColumns(data);
        return updateOrInsert(UPDATE.create(table, columns), getSortedValues(columns, data));
    }

    /**
     * Inserts data to a table.
     *
     * @param table to insert data to
     * @param data  map of columns to insert to and data to be inserted
     * @return future of JsonObject containing insertion results
     */
    @Override
    public Future<JsonObject> insert(Table table, Map<Column, String> data) { // TODO: 25.04.2017 test
        if (data.get(USERNAME) == null) {
            return failedFuture("Username required.");
        }
        List<Column> columns = getSortedColumns(data);
        return updateOrInsert(INSERT.create(table, columns), getSortedValues(columns, data));
    }

    /**
     * Gets data for a single user.
     */
    @Override
    public Future<JsonObject> getUser(String username) {
        return query(SQL_QUERY_USER, new JsonArray().add(username));
    }

    /**
     * Gets all users.
     */
    @Override
    public Future<JsonObject> getAllUsers() {
        return query(SQL_QUERY_USERS, null);
    }

    /**
     * Gets all movies views for user.
     */
    @Override
    public Future<JsonObject> getViews(String username, String jsonParam) {
        JsonObject json = new JsonObject(jsonParam);
        json.put("start", formToDBDate(json.getString("start"), false));
        json.put("end", formToDBDate(json.getString("end"), true));
        StringBuilder sb = new StringBuilder(SQL_QUERY_VIEWS);
        ifTrue(json.getBoolean("is-first"), () -> sb.append(" AND WasFirst"));
        ifTrue(json.getBoolean("is-cinema"), () -> sb.append(" AND WasCinema"));
        return query(sb.append(" ORDER BY Start DESC LIMIT ?, ?").toString(), new JsonArray()
                .add(username)
                .add(json.getString("start"))
                .add(json.getString("end"))
                .add(json.getInteger("page") * 10)
                .add(10));
    }

    @Override
    public Future<JsonObject> getTopMoviesStat(String username, String jsonParam) {
        JsonObject json = new JsonObject(jsonParam);
        json.put("start", formToDBDate(json.getString("start"), false));
        json.put("end", formToDBDate(json.getString("end"), true));
        StringBuilder sb = new StringBuilder(SQL_GET_TOP_MOVIES_STAT);
        ifTrue(json.getBoolean("is-first"), () -> sb.append(" AND WasFirst"));
        ifTrue(json.getBoolean("is-cinema"), () -> sb.append(" AND WasCinema"));
        return query(sb.append(" GROUP BY MovieId ORDER BY Count DESC LIMIT 3").toString(), new JsonArray()
                .add(username)
                .add(json.getString("start"))
                .add(json.getString("end")));
    }

    @Override
    public Future<JsonObject> getYearsDistribution(String username, String jsonParam) {
        JsonObject json = new JsonObject(jsonParam);
        json.put("start", formToDBDate(json.getString("start"), false));
        json.put("end", formToDBDate(json.getString("end"), true));
        StringBuilder sb = new StringBuilder(SQL_GET_YEARS_DIST);
        ifTrue(json.getBoolean("is-first"), () -> sb.append(" AND WasFirst"));
        ifTrue(json.getBoolean("is-cinema"), () -> sb.append(" AND WasCinema"));
        return query(sb.append(" GROUP BY Year ORDER BY Year DESC").toString(), new JsonArray()
                .add(username)
                .add(json.getString("start"))
                .add(json.getString("end")));
    }

    @Override
    public Future<JsonObject> getWeekdaysDistribution(String username, String jsonParam) {
        JsonObject json = new JsonObject(jsonParam);
        json.put("start", formToDBDate(json.getString("start"), false));
        json.put("end", formToDBDate(json.getString("end"), true));
        StringBuilder sb = new StringBuilder(SQL_GET_WEEKDAYS_DIST);
        ifTrue(json.getBoolean("is-first"), () -> sb.append(" AND WasFirst"));
        ifTrue(json.getBoolean("is-cinema"), () -> sb.append(" AND WasCinema"));
        return query(sb.append(" GROUP BY Day ORDER BY Day").toString(), new JsonArray()
                .add(username)
                .add(json.getString("start"))
                .add(json.getString("end")));
    }

    @Override
    public Future<JsonObject> getTimeDistribution(String username, String jsonParam) {
        JsonObject json = new JsonObject(jsonParam);
        json.put("start", formToDBDate(json.getString("start"), false));
        json.put("end", formToDBDate(json.getString("end"), true));
        StringBuilder sb = new StringBuilder(SQL_GET_TIME_DIST);
        ifTrue(json.getBoolean("is-first"), () -> sb.append(" AND WasFirst"));
        ifTrue(json.getBoolean("is-cinema"), () -> sb.append(" AND WasCinema"));
        return query(sb.append(" GROUP BY Hour").toString(), new JsonArray()
                .add(username)
                .add(json.getString("start"))
                .add(json.getString("end")));
    }

    @Override
    public Future<JsonObject> getMonthYearDistribution(String username, String jsonParam) {
        JsonObject json = new JsonObject(jsonParam);
        json.put("start", formToDBDate(json.getString("start"), false));
        json.put("end", formToDBDate(json.getString("end"), true));
        StringBuilder sb = new StringBuilder(SQL_GET_MONTH_YEAR_DIST);
        ifTrue(json.getBoolean("is-first"), () -> sb.append(" AND WasFirst"));
        ifTrue(json.getBoolean("is-cinema"), () -> sb.append(" AND WasCinema"));
        return query(sb.append(" GROUP BY Month, Year ORDER BY Year, Month").toString(), new JsonArray()
                .add(username)
                .add(json.getString("start"))
                .add(json.getString("end")));
    }

    /**
     * Gets a specific movie views for user.
     */
    @Override
    public Future<JsonObject> getMovieViews(String username, String movieId) {
        return query(SQL_GET_MOVIE_VIEWS, new JsonArray().add(username).add(movieId));
    }

    @Override
    public Future<JsonObject> getAllTimeMeta(String username, String jsonParam) {
        JsonObject json = new JsonObject(jsonParam);
        StringBuilder sb = new StringBuilder(SQL_GET_ALL_TIME_META);
        ifTrue(json.getBoolean("is-first"), () -> sb.append(" AND WasFirst"));
        ifTrue(json.getBoolean("is-cinema"), () -> sb.append(" AND WasCinema"));
        return query(sb.toString(), new JsonArray().add(username));
    }

    @Override
    public Future<JsonObject> getViewsMeta(String username, String jsonParam) {
        JsonObject json = new JsonObject(jsonParam);
        System.out.println(json.encodePrettily());
        json.put("start", formToDBDate(json.getString("start"), false));
        json.put("end", formToDBDate(json.getString("end"), true));
        StringBuilder sb = new StringBuilder(SQL_QUERY_VIEWS_META);
        ifTrue(json.getBoolean("is-first"), () -> sb.append(" AND WasFirst"));
        ifTrue(json.getBoolean("is-cinema"), () -> sb.append(" AND WasCinema"));
        System.out.println("QUERY");
        System.out.println(sb.toString());
        return query(sb.toString(), new JsonArray()
                .add(username)
                .add(json.getString("start"))
                .add(json.getString("end")));
    }

    @Override
    public Future<JsonObject> removeView(String username, String id) {
        return updateOrInsert(SQL_REMOVE_VIEW, new JsonArray().add(username).add(id));
    }

    @Override
    public Future<JsonObject> removeEpisode(String username, String episodeId) {
        return updateOrInsert(SQL_REMOVE_EPISODE, new JsonArray().add(username).add(episodeId));
    }

    @Override
    public Future<JsonObject> getWatchingSeries(String username) {
        return query(SQL_GET_WATCHING_SERIES, new JsonArray().add(username));
    }

    @Override
    public Future<JsonObject> removeFromWishlist(String username, String movieId) {
        return updateOrInsert(SQL_REMOVE_WISHLIST, new JsonArray().add(username).add(movieId));
    }

    @Override
    public Future<JsonObject> getLastMoviesHome(String username) {
        return query(SQL_GET_LAST_VIEWS, new JsonArray().add(username));
    }

    @Override
    public Future<JsonObject> getLastWishlistHome(String username) {
        return query(SQL_GET_HOME_WISHLIST, new JsonArray().add(username));
    }

    @Override
    public Future<JsonObject> getTopMoviesHome(String username) {
        return query(SQL_GET_TOP_MOVIES, new JsonArray().add(username));
    }

    @Override
    public Future<JsonObject> getTotalMovieCount(String username) {
        return query(SQL_GET_TOTAL_MOVIE_COUNT, new JsonArray().add(username));
    }

    @Override
    public Future<JsonObject> getNewMovieCount(String username) {
        return query(SQL_GET_NEW_MOVIE_COUNT, new JsonArray().add(username));
    }

    @Override
    public Future<JsonObject> getTotalRuntime(String username) {
        return query(SQL_GET_TOTAL_RUNTIME, new JsonArray().add(username));
    }

    @Override
    public Future<JsonObject> getTotalDistinctMoviesCount(String username) {
        return query(SQL_GET_TOTAL_DISTINCT_MOVIES, new JsonArray().add(username));
    }

    @Override
    public Future<JsonObject> getTotalCinemaCount(String username) {
        return query(SQL_GET_TOTAL_CINEMA_COUNT, new JsonArray().add(username));
    }

    @Override
    public Future<JsonObject> insertSeasonViews(String username, JsonObject seasonData, String seriesId) {
        StringBuilder query = new StringBuilder(SQL_INSERT_SEASON);
        JsonArray episodes = seasonData.getJsonArray("episodes");
        JsonArray valuesArray = new JsonArray();
        //currentTimeMillis()
        if (!episodes.isEmpty()) {
            for (Object jsonObject : episodes) {
                query.append(" (?, ?, ?, ?, ?),");
                valuesArray
                        .add(username)
                        .add(seriesId)
                        .add(((JsonObject) jsonObject).getInteger("id"))
                        .add(seasonData.getString("_id"))
                        .add(currentTimeMillis());
            }
            query.deleteCharAt(query.length() - 1);
        }
        /*
        INSERT INTO tbl_name
            (a,b,c)
        VALUES
            (1,2,3),
            (4,5,6),
            (7,8,9);
         */
        return updateOrInsert(query.toString(), valuesArray);
    }

    @Override
    public Future<JsonObject> removeSeasonViews(String username, String seasonId) {
        return updateOrInsert(SQL_REMOVE_SEASON, new JsonArray().add(username).add(seasonId));
    }

    /**
     * Gets users count in database.
     */
    @Override
    public Future<String> getUsersCount() {
        return future(fut -> query(SQL_USERS_COUNT, null).rxSetHandler()
                .map(DatabaseService::getRows)
                .map(array -> array.getJsonObject(0).getLong("Count").toString())
                .subscribe(fut::complete, fut::fail));
    }
}
