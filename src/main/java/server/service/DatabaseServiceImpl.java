package server.service;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import server.entity.Event;
import server.entity.Privilege;
import server.entity.admin.AdminCountParams;
import server.entity.admin.AdminSessionsParams;

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
    private static final String SQL_UPDATE_MOVIE_POSTER =
            "UPDATE Movies SET Image = ? WHERE Movies.Id = ?";
    private static final String SQL_INSERT_SERIES =
            "INSERT IGNORE INTO SeriesInfo VALUES (?, ?, ?)";
    private static final String SQL_UPDATE_SERIES_POSTER =
            "UPDATE SeriesInfo SET Image = ? WHERE SeriesInfo.Id = ?";
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
            "SELECT Title, Image, Series.SeriesId, COUNT(Series.SeriesId) AS Count, Active FROM Series " +
                    "JOIN SeriesInfo ON Series.SeriesId = SeriesInfo.Id " +
                    "JOIN UserSeriesInfo ON Series.SeriesId = UserSeriesInfo.SeriesId " +
                    "WHERE Series.Username = ? AND UserSeriesInfo.Username = ? AND Active " +
                    "GROUP BY Series.SeriesId, Active " +
                    "ORDER BY Title;";
    private static final String SQL_GET_LAST_VIEWS =
            "SELECT Title, Start, MovieId, WEEKDAY(Start) AS 'week_day', WasCinema FROM Views " +
                    "JOIN Movies ON Movies.Id = Views.MovieId " +
                    "WHERE Username = ? " +
                    "ORDER BY Start DESC LIMIT 5";
    private static final String SQL_GET_TOP_MOVIES =
            "SELECT MovieId, Title, COUNT(*) AS Count, Image FROM Views " +
                    "JOIN Movies ON Movies.Id = Views.MovieId " +
                    "WHERE Username = ? " +
                    "GROUP BY MovieId ORDER BY Count DESC, AVG(Start) DESC LIMIT 5";
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
            "SELECT Id, ListName FROM ListsInfo WHERE Username = ? AND Active ORDER BY TimeCreated ASC;";
    private static final String SQL_GET_LISTS_SIZE =
            "SELECT ListsInfo.Id as id, IFNULL((SELECT COUNT(ListId) FROM ListEntry " +
                    "WHERE Username = ? AND ListEntry.ListId = ListsInfo.Id GROUP BY ListId), 0) AS count, " +
                    "IFNULL((SELECT SUM(MovieId IN (SELECT DISTINCT MovieId FROM Views WHERE Username = ?)) AS seen  " +
                    "FROM ListEntry WHERE Username = ? AND ListEntry.ListId = ListsInfo.Id " +
                    "GROUP BY ListId), 0) AS seen FROM ListsInfo WHERE Username = ? AND Active ORDER BY TimeCreated ASC;";
    private static final String SQL_INSERT_INTO_LIST =
            "INSERT IGNORE INTO ListEntry VALUES (?, ?, ?, ?);";
    private static final String SQL_REMOVE_FROM_LIST =
            "DELETE FROM ListEntry WHERE Username = ? AND ListId = ? && MovieId = ?;";
    private static final String SQL_GET_IN_LISTS =
            "SELECT ListId FROM ListEntry WHERE Username = ? AND MovieId = ?;";
    private static final String SQL_GET_LIST_ENTRIES =
            "SELECT MovieId, Title, ListName, Year, Image, Time, ListId, " +
                    "MovieId IN (SELECT DISTINCT Views.MovieId " +
                    "FROM ListEntry " +
                    "JOIN Views ON ListEntry.MovieId = Views.MovieId " +
                    "WHERE Views.Username = ? AND ListEntry.Username = ? AND " +
                    "ListId = ?) AS Seen FROM ListEntry " +
                    "JOIN Movies ON ListEntry.MovieId = Movies.Id " +
                    "JOIN ListsInfo ON ListsInfo.Id = ListEntry.ListId " +
                    "WHERE ListEntry.Username = ? AND ListId = ? " +
                    "ORDER BY Time DESC;";
    private static final String SQL_CHANGE_LIST_NAME =
            "UPDATE ListsInfo SET ListName = ? WHERE Username = ? AND Id = ?;";
    private static final String SQL_DELETE_LIST =
            "UPDATE ListsInfo SET Active = 0 WHERE Username = ? AND Id = ?;";
    private static final String SQL_GET_LIST_SEEN_MOVIES =
            "SELECT DISTINCT Views.MovieId FROM ListEntry " +
                    "JOIN Views ON ListEntry.MovieId = Views.MovieId " +
                    "WHERE Views.Username = ? AND " +
                    "ListEntry.Username = ? AND " +
                    "ListId = ?;";
    private static final String SQL_GET_LIST_NAME =
            "SELECT ListName FROM ListsInfo WHERE Username = ? AND Id = ?;";
    private static final String SQL_GET_LAST_LISTS_HOME =
            "SELECT ListId, ListName, MovieId, Title, Year FROM ListEntry " +
                    "JOIN Movies ON Movies.Id = ListEntry.MovieId " +
                    "JOIN ListsInfo ON ListsInfo.Id = ListEntry.ListId " +
                    "WHERE ListEntry.Username = ? AND ACTIVE " +
                    "ORDER BY Time DESC LIMIT 5";
    private static final String SQL_GET_DELETED_LISTS =
            "SELECT Id, ListName, TimeCreated FROM ListsInfo WHERE Username = ? AND NOT Active ORDER BY TimeCreated ASC;";
    private static final String SQL_RESTORE_DELETED_LIST =
            "UPDATE ListsInfo SET Active = 1 WHERE Username = ? AND Id = ?;";
    private static final String SQL_INSERT_USER_SERIES_INFO =
            "INSERT IGNORE INTO UserSeriesInfo (Username, SeriesId) VALUES (?, ?);";
    private static final String SQL_SET_SERIES_INACTIVE =
            "UPDATE UserSeriesInfo SET Active = 0 WHERE Username = ? AND SeriesId = ?;";
    private static final String SQL_GET_INACTIVE_SERIES =
            "SELECT Title, Image, Series.SeriesId, COUNT(Series.SeriesId) AS Count, Active FROM Series " +
                    "JOIN SeriesInfo ON Series.SeriesId = SeriesInfo.Id " +
                    "JOIN UserSeriesInfo ON Series.SeriesId = UserSeriesInfo.SeriesId " +
                    "WHERE Series.Username = ? AND UserSeriesInfo.Username = ? AND NOT Active " +
                    "GROUP BY Series.SeriesId, Active " +
                    "ORDER BY Title;";
    private static final String SQL_SET_SERIES_ACTIVE =
            "UPDATE UserSeriesInfo SET Active = 1 WHERE Username = ? AND SeriesId = ?;";
    private static final String SQL_GET_TODAY_IN_HISTORY =
            "SELECT MovieId, Title, Year(Start) AS Year, WasCinema FROM Views " +
                    "JOIN Movies ON Movies.Id = Views.MovieId " +
                    "WHERE Username = ? AND " +
                    "DAYOFMONTH(Start) = DAYOFMONTH(DATE(NOW())) AND " +
                    "MONTH(Start) = MONTH(DATE(NOW())) ORDER BY YEAR DESC;";
    private static final String SQL_GET_HOME_STATISTICS =
            "SELECT " +
                    "(SELECT COUNT(*) FROM Views WHERE Username = ?) " +
                    "AS 'total_views', " +
                    "(SELECT COUNT(WasFirst) FROM Views WHERE Username = ? AND WasFirst = 1) " +
                    "AS 'first_view', " +
                    "(SELECT COUNT(DISTINCT MovieId) FROM Views WHERE Username = ?) " +
                    "AS 'unique_movies', " +
                    "(SELECT COUNT(*) FROM Views WHERE Username = ? AND WasCinema = 1) " +
                    "AS 'total_cinema', " +
                    "(SELECT SUM(TIMESTAMPDIFF(MINUTE, Start, End)) FROM Views WHERE Username = ?) " +
                    "AS 'total_runtime';";
    private static final String SQL_GET_OSCAR_AWARDS = "" +
            "SELECT cate.Name, awar.Status, cate.DisplayValue " +
            "FROM Awards awar " +
            "       JOIN Category cate ON cate.Id = awar.CategoryId " +
            "WHERE awar.MovieId = ?" +
            "ORDER BY cate.Order, cate.Id asc;";
    private static final String SQL_INSERT_EVENT = "INSERT INTO Event (Username, Event) VALUE (?, ?);";
    private static final String SQL_INSERT_API_KEY_EVENT = "CALL insert_api_key_event(?, ?, ?);";
    private static final String SQL_INSERT_LOGIN_EVENT =
            "INSERT INTO LoginEvent (Username, Client, Server) VALUES (?, ?, ?);";
    private static final String SQL_IS_PRIVILEGE_GRANTED = "" +
            "SELECT EXISTS(SELECT * " +
            "FROM ApiKey " +
            "WHERE ApiKey = ? " +
            "AND Privilege = ?) as PrivilegeExists;";

    private JDBCClient client;

    protected DatabaseServiceImpl(Vertx vertx, JsonObject config) {
        if (config.containsKey("mysql")) {
            this.client = JDBCClient.createShared(vertx, config.getJsonObject("mysql"));
        }
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
        if (!nonNull(username, password) || contains("", username)) {
            return failedFuture("Email must exist!");
        }
        return future(fut -> ifPresent(genString(), salt -> updateOrInsert(SQL_INSERT_USER, new JsonArray()
                .add(username)
                .add(firstname == null ? "" : firstname)
                .add(lastname == null ? "" : lastname)
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
    public Future<JsonObject> updateMoviePoster(int id, String posterPath) {
        return updateOrInsert(SQL_UPDATE_MOVIE_POSTER, new JsonArray()
                .add(posterPath)
                .add(id));
    }


    @Override
    public Future<JsonObject> insertSeries(int id, String seriesTitle, String posterPath) {
        return updateOrInsert(SQL_INSERT_SERIES, new JsonArray()
                .add(id)
                .add(seriesTitle)
                .add(posterPath));
    }

    @Override
    public Future<JsonObject> updateSeriesPoster(int id, String posterPath) {
        return updateOrInsert(SQL_UPDATE_SERIES_POSTER, new JsonArray()
                .add(posterPath)
                .add(id));
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
        json.put("start", formToDBDate(json.getString("start"), false));
        json.put("end", formToDBDate(json.getString("end"), true));
        StringBuilder sb = new StringBuilder(SQL_QUERY_VIEWS_META);
        ifTrue(json.getBoolean("is-first"), () -> sb.append(" AND WasFirst"));
        ifTrue(json.getBoolean("is-cinema"), () -> sb.append(" AND WasCinema"));
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
        return query(SQL_GET_WATCHING_SERIES, new JsonArray()
                .add(username)
                .add(username));
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
                .add(username)
                .add(listId)
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

    @Override
    public Future<JsonObject> getListsSize(String username) {
        return query(SQL_GET_LISTS_SIZE, new JsonArray()
                .add(username)
                .add(username)
                .add(username)
                .add(username));
    }

    @Override
    public Future<JsonObject> insertUserSeriesInfo(String username, String seriesId) {
        return updateOrInsert(SQL_INSERT_USER_SERIES_INFO, new JsonArray()
                .add(username)
                .add(seriesId));
    }

    @Override
    public Future<JsonObject> changeSeriesToInactive(String username, String seriesId) {
        return updateOrInsert(SQL_SET_SERIES_INACTIVE, new JsonArray()
                .add(username)
                .add(seriesId));
    }

    @Override
    public Future<JsonObject> getInactiveSeries(String username) {
        return query(SQL_GET_INACTIVE_SERIES, new JsonArray()
                .add(username)
                .add(username));
    }

    @Override
    public Future<JsonObject> changeSeriesToActive(String username, String seriesId) {
        return updateOrInsert(SQL_SET_SERIES_ACTIVE, new JsonArray()
                .add(username)
                .add(seriesId));
    }

    @Override
    public Future<JsonObject> getTodayInHistory(String username) {
        return query(SQL_GET_TODAY_IN_HISTORY, new JsonArray()
                .add(username));
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

    @Override
    public Future<JsonObject> getHomeStatistics(String username) {
        return query(SQL_GET_HOME_STATISTICS, new JsonArray()
                .add(username)
                .add(username)
                .add(username)
                .add(username)
                .add(username));
    }

    @Override
    public Future<JsonObject> getOscarAwards(String movieId) {
        return query(SQL_GET_OSCAR_AWARDS, new JsonArray()
                .add(movieId));
    }

    @Override
    public Future<JsonObject> insertEvent(String username, Event event) {
        return updateOrInsert(SQL_INSERT_EVENT, new JsonArray()
                .add(username)
                .add(event));
    }

    @Override
    public Future<JsonObject> insertApiKeyEvent(String apiKey, Event event, String data) {
        return updateOrInsert(SQL_INSERT_API_KEY_EVENT, new JsonArray()
                .add(apiKey)
                .add(event)
                .add(data));
    }

    @Override
    public Future<JsonObject> insertLoginEvent(String username, String client, String server) {
        return updateOrInsert(SQL_INSERT_LOGIN_EVENT, new JsonArray()
                .add(username)
                .add(client)
                .add(server));
    }

    @Override
    public Future<JsonObject> getNewUsersCount(AdminCountParams params) {
        StringBuilder sql = new StringBuilder();
        JsonArray sqlParams = new JsonArray();

        final String VALUE_LABEL = getValueLabel(params);

        if (params.isSum()) {
            sql.append(String.format("SELECT MIN(result.%s) as Start%s, MAX(result.%s) as End%s, " +
                    "SUM(result.Count) as Count FROM ( ", VALUE_LABEL, VALUE_LABEL, VALUE_LABEL, VALUE_LABEL));
        }

        sql.append(String.format("SELECT %S (users.AddedTime) AS %s, count(*) AS Count FROM Users users ",
                VALUE_LABEL, VALUE_LABEL));
        sql.append("WHERE users.AddedTime IS NOT NULL ");

        appendSql(params.getYear(), sql, "AND YEAR(users.AddedTime) = ?", sqlParams);
        appendSql(params.getMonth(), sql, "AND MONTH(users.AddedTime) = ?", sqlParams);
        appendSql(params.getDay(), sql, "AND DAY(users.AddedTime) = ?", sqlParams);
        appendSql(params.getStartDate(), sql, "AND DATE(users.AddedTime) >= STR_TO_DATE(?, '%Y-%m-%d')", sqlParams);
        appendSql(params.getEndDate(), sql, "AND DATE(users.AddedTime) <= STR_TO_DATE(?, '%Y-%m-%d')", sqlParams);
        sql.append(String.format("GROUP BY %s ORDER BY %s %S", VALUE_LABEL, VALUE_LABEL, getOrderValue(params)));

        if (params.isSum()) {
            sql.append(") result");
        }

        sql.append(";");

        return future(fut -> query(sql.toString(), sqlParams).rxSetHandler()
                .map(obj -> new JsonObject().put("rows", obj.getJsonArray("rows")))
                .subscribe(fut::complete, fut::fail));
    }

    @Override
    public Future<JsonObject> getUsersSessions(AdminSessionsParams params) {
        StringBuilder sql = new StringBuilder("SELECT user, client, timestamp FROM v_user_session ");
        JsonArray sqlParams = new JsonArray();

        sql.append("WHERE 1 = 1 ");

        appendSql(params.getYear(), sql, "AND YEAR(timestamp) = ?", sqlParams);
        appendSql(params.getMonth(), sql, "AND MONTH(timestamp) = ?", sqlParams);
        appendSql(params.getDay(), sql, "AND DAY(timestamp) = ?", sqlParams);
        appendSql(params.getStartDate(), sql, "AND DATE(timestamp) >= STR_TO_DATE(?, '%Y-%m-%d')", sqlParams);
        appendSql(params.getEndDate(), sql, "AND DATE(timestamp) <= STR_TO_DATE(?, '%Y-%m-%d')", sqlParams);

        sql.append(";");

        return future(fut -> query(sql.toString(), sqlParams).rxSetHandler()
                .map(obj -> new JsonObject().put("rows", obj.getJsonArray("rows")))
                .subscribe(fut::complete, fut::fail));
    }

    private static String getValueLabel(AdminCountParams params) {
        return params.getAggregation() == null ? "Date" : params.getAggregation().getValue();
    }

    private static String getOrderValue(AdminCountParams params) {
        return params.getAggregation() == null ? "DESC" : "ASC";
    }

    private void appendSql(Object param, StringBuilder builder, String sql, JsonArray sqlParams) {
        if (param != null) {
            builder.append(sql).append(" ");
            sqlParams.add(param);
        }
    }

    @Override
    public Future<Boolean> isPrivilegeGranted(String apiKey, Privilege privilege) {
        return query(SQL_IS_PRIVILEGE_GRANTED, new JsonArray()
                .add(apiKey)
                .add(privilege))
                .map(res -> res.getJsonArray("rows")
                        .getJsonObject(0)
                        .getLong("PrivilegeExists")
                        .equals(1L));
    }
}
