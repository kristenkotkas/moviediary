package server.service;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.jdbc.JDBCClient;

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
    private static final String SQL_INSERT_EPISODE =
            "INSERT IGNORE INTO Series (Username, SeriesId, EpisodeId, SeasonId, Time) VALUES (?, ?, ?, ?, ?)";
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
            "SELECT DATE(Min(Start)) AS Start, COUNT(*) AS Count, " +
                    "SUM(TIMESTAMPDIFF(MINUTE, Start, End)) AS Runtime FROM Views " +
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
    private static final String SQL_GET_LAST_VIEWS =
            "SELECT Title, Start, MovieId, WEEKDAY(Start) AS 'week_day', WasCinema FROM Views " +
                    "JOIN Movies ON Movies.Id = Views.MovieId " +
                    "WHERE Username = ? " +
                    "ORDER BY Start DESC LIMIT 5";
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
    private static final String SQL_REMOVE_SEASON =
            "DELETE FROM Series WHERE Username = ? AND SeasonId = ?;";
    private static final String SQL_INSERT_NEW_LIST =
            "INSERT INTO ListsInfo (Username, ListName, TimeCreated) VALUES (?, ?, ?);";
    private static final String SQL_GET_LISTS =
            "SELECT Id, ListName, TimeCreated, COUNT(ListId) AS Size FROM ListsInfo " +
                    "JOIN ListEntries ON ListsInfo.Id = ListEntries.ListId " +
                    "WHERE ListsInfo.Username = ? AND ListEntries.Username = ? " +
                    "AND Active GROUP BY ListId ORDER BY TimeCreated DESC;";
    private static final String SQL_INSERT_INTO_LIST =
            "INSERT IGNORE INTO ListEntries VALUES (?, ?, ?, ?);";
    private static final String SQL_REMOVE_FROM_LIST =
            "DELETE FROM ListEntries WHERE Username = ? AND ListId = ? && MovieId = ?;";
    private static final String SQL_GET_IN_LISTS =
            "SELECT ListId FROM ListEntries WHERE Username = ? AND MovieId = ?;";
    private static final String SQL_GET_LIST_ENTRIES =
            "SELECT MovieId, Title, ListName, Year, Image, Time FROM ListEntries " +
                    "JOIN Movies On ListEntries.MovieId = Movies.Id " +
                    "JOIN ListsInfo On ListsInfo.Id = ListEntries.ListId " +
                    "WHERE ListEntries.Username = ? AND ListId = ? ORDER BY Time DESC;";
    private static final String SQL_CHANGE_LIST_NAME =
            "UPDATE ListsInfo SET ListName = ? WHERE Username = ? AND Id = ?;";
    private static final String SQL_DELETE_LIST =
            "UPDATE ListsInfo SET Active = 0 WHERE Username = ? AND Id = ?;";
    private static final String SQL_GET_LIST_SEEN_MOVIES =
            "SELECT DISTINCT Views.MovieId FROM ListEntries " +
                    "JOIN Views ON ListEntries.MovieId = Views.MovieId " +
                    "WHERE " +
                    "Views.Username = ? AND " +
                    "ListEntries.Username = ? AND " +
                    "ListId = ?;";
    private static final String SQL_GET_LIST_NAME =
            "SELECT ListName FROM ListsInfo WHERE Username = ? AND Id = ?;";
    private static final String SQL_GET_LAST_LISTS_HOME =
            "SELECT ListId, ListName, MovieId, Title, Year FROM ListEntries " +
                    "JOIN Movies ON Movies.Id = ListEntries.MovieId " +
                    "JOIN ListsInfo ON ListsInfo.Id = ListEntries.ListId " +
                    "WHERE ListEntries.Username = ? AND ACTIVE " +
                    "ORDER BY Time DESC LIMIT 5";
    private static final String SQL_GET_DELETED_LISTS =
            "SELECT Id, ListName, TimeCreated FROM ListsInfo WHERE Username = ? AND NOT Active ORDER BY TimeCreated DESC;";
    private static final String SQL_RESTORE_DELETED_LIST =
            "UPDATE ListsInfo SET Active = 1 WHERE Username = ? AND Id = ?;";

    private final JDBCClient client;

    protected DatabaseServiceImpl(Vertx vertx, JsonObject config) {
        this.client = JDBCClient.createShared(vertx, config.getJsonObject("mysql"));
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
        //System.out.println(json.encodePrettily());
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
    public Future<JsonObject> getLastMoviesHome(String username) {
        return query(SQL_GET_LAST_VIEWS, new JsonArray().add(username));
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
    public Future<JsonObject> insertSeasonViews(String username, JsonObject seasonData, String seriesId) { // TODO: 18/05/2017 test
        StringBuilder query = new StringBuilder(SQL_INSERT_SEASON);
        JsonArray episodes = seasonData.getJsonArray("episodes");
        JsonArray values = new JsonArray();
        ifFalse(episodes.isEmpty(), () -> {
            episodes.stream()
                    .map(obj -> (JsonObject) obj)
                    .peek(json -> query.append(" (?, ?, ?, ?, ?),"))
                    .forEach(json -> values
                            .add(username)
                            .add(seriesId)
                            .add(json.getInteger("id"))
                            .add(seasonData.getString("_id"))
                            .add(currentTimeMillis()));
            query.deleteCharAt(query.length() - 1);
        });
        return updateOrInsert(query.toString(), values);
    }

    @Override
    public Future<JsonObject> insertList(String username, String listName) {
        return updateOrInsert(SQL_INSERT_NEW_LIST, new JsonArray()
                .add(username)
                .add(listName)
                .add(currentTimeMillis()));
    }

    @Override
    public Future<JsonObject> removeSeasonViews(String username, String seasonId) {
        return updateOrInsert(SQL_REMOVE_SEASON, new JsonArray().add(username).add(seasonId));
    }

    @Override
    public Future<JsonObject> getLists(String username) {
        return query(SQL_GET_LISTS, new JsonArray()
                .add(username)
                .add(username));
    }

    @Override
    public Future<JsonObject> getDeletedLists(String username) {
        return query(SQL_GET_DELETED_LISTS, new JsonArray().add(username));
    }

    @Override
    public Future<JsonObject> insertIntoList(String username, String jsonParam) {
        return updateOrInsert(SQL_INSERT_INTO_LIST, new JsonArray()
                .add(username)
                .add(new JsonObject(jsonParam).getString("listId"))
                .add(new JsonObject(jsonParam).getString("movieId"))
                .add(currentTimeMillis()));
    }

    @Override
    public Future<JsonObject> removeFromList(String username, String jsonParam) {
        return updateOrInsert(SQL_REMOVE_FROM_LIST, new JsonArray()
                .add(username)
                .add(new JsonObject(jsonParam).getString("listId"))
                .add(new JsonObject(jsonParam).getString("movieId")));
    }

    @Override
    public Future<JsonObject> getInLists(String username, String movieId) {
        return query(SQL_GET_IN_LISTS, new JsonArray()
                .add(username)
                .add(movieId));
    }

    @Override
    public Future<JsonObject> getListEntries(String username, String listId) {
        return query(SQL_GET_LIST_ENTRIES, new JsonArray()
                .add(username)
                .add(listId));
    }

    @Override
    public Future<JsonObject> changeListName(String username, String param) {
        return updateOrInsert(SQL_CHANGE_LIST_NAME, new JsonArray()
                .add(new JsonObject(param).getString("listName"))
                .add(username)
                .add(new JsonObject(param).getString("listId")));

    }

    @Override
    public Future<JsonObject> deleteList(String username, String listId) {
        return updateOrInsert(SQL_DELETE_LIST, new JsonArray()
                .add(username)
                .add(listId));
    }

    @Override
    public Future<JsonObject> getListSeenMovies(String username, String listId) {
        return query(SQL_GET_LIST_SEEN_MOVIES, new JsonArray()
                .add(username)
                .add(username)
                .add(listId));
    }

    @Override
    public Future<JsonObject> getListName(String username, String listId) {
        return query(SQL_GET_LIST_NAME, new JsonArray()
                .add(username)
                .add(listId));
    }

    @Override
    public Future<JsonObject> getLastListsHome(String username) {
        return query(SQL_GET_LAST_LISTS_HOME, new JsonArray()
                .add(username));
    }

    @Override
    public Future<JsonObject> restoreDeletedList(String username, String listId) {
        return updateOrInsert(SQL_RESTORE_DELETED_LIST, new JsonArray()
                .add(username)
                .add(listId));
    }

    /**
     * Gets users count in database.
     */
    @Override
    public Future<JsonObject> getUsersCount() {
        return future(fut -> query(SQL_USERS_COUNT, null).rxSetHandler()
                .map(DatabaseService::getRows)
                .map(array -> array.getJsonObject(0))
                .subscribe(fut::complete, fut::fail));
    }
}
