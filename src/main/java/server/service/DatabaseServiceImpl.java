package server.service;

import eu.moviediary.database.tables.pojos.Listentries;
import eu.moviediary.database.tables.pojos.Movies;
import eu.moviediary.database.tables.pojos.Series;
import eu.moviediary.database.tables.pojos.Seriesinfo;
import eu.moviediary.database.tables.records.SeriesRecord;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jooq.Configuration;
import org.jooq.DatePart;
import org.jooq.Record;
import org.jooq.SelectConditionStep;
import org.jooq.impl.DSL;
import server.database.CommonDao;
import static eu.moviediary.database.tables.Listentries.LISTENTRIES;
import static eu.moviediary.database.tables.Listsinfo.LISTSINFO;
import static eu.moviediary.database.tables.Movies.MOVIES;
import static eu.moviediary.database.tables.Series.SERIES;
import static eu.moviediary.database.tables.Seriesinfo.SERIESINFO;
import static eu.moviediary.database.tables.Userseriesinfo.USERSERIESINFO;
import static eu.moviediary.database.tables.Views.VIEWS;
import static io.vertx.rxjava.core.Future.failedFuture;
import static io.vertx.rxjava.core.Future.future;
import static java.lang.System.currentTimeMillis;
import static org.jooq.impl.DSL.*;
import static server.service.DatabaseService.Column.USERNAME;
import static server.service.DatabaseService.Column.getSortedColumns;
import static server.service.DatabaseService.Column.getSortedValues;
import static server.service.DatabaseService.SQLCommand.INSERT;
import static server.service.DatabaseService.SQLCommand.UPDATE;
import static server.service.DatabaseService.createDataMap;
import static server.util.CommonUtils.contains;
import static server.util.CommonUtils.ifPresent;
import static server.util.CommonUtils.ifTrue;
import static server.util.CommonUtils.nonNull;
import static server.util.StringUtils.formToDBDate;
import static server.util.StringUtils.genString;
import static server.util.StringUtils.hash;
import static server.util.StringUtils.movieDateToDBDate;

/**
 * Database service implementation.
 */
public class DatabaseServiceImpl extends CommonDao implements DatabaseService {
    private static final String SQL_GET_YEARS_DIST =
            "SELECT Year, COUNT(1) AS Count FROM Views " +
                    "JOIN Movies ON Movies.Id = Views.MovieId " +
                    "WHERE Username = ? AND Start >= ? AND Start <= ?";
    private static final String SQL_GET_WEEKDAYS_DIST =
            "SELECT ((DAY_OF_WEEK(Start) + 5) % 7) AS Day, COUNT(1) AS Count " +
                    "FROM Views " +
                    "WHERE Username = ? AND Start >= ? AND Start <= ?";
    private static final String SQL_GET_TIME_DIST =
            "SELECT HOUR(Start) AS Hour, COUNT(1) AS Count FROM Views " +
                    "WHERE Username = ? AND Start >= ? AND Start <= ? ";
    private static final String SQL_GET_MONTH_YEAR_DIST =
            "SELECT MONTH(Start) AS Month, YEAR(Start) AS Year, COUNT(1) AS Count " +
                    "FROM Views " +
                    "WHERE Username = ? AND Start >= ? AND Start <= ? ";
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
            "SELECT MovieId, Title, COUNT(1) AS Count, Image FROM Views " +
                    "JOIN Movies ON Movies.Id = Views.MovieId " +
                    "WHERE Username = ? AND Start >= ? AND Start <= ?";
    private static final String SQL_QUERY_SETTINGS = "SELECT * FROM Settings WHERE Username = ?";
    private static final String SQL_GET_MOVIE_VIEWS =
            "SELECT Id, Start, WasCinema FROM Views" +
                    " WHERE Username = ? AND MovieId = ?" +
                    " ORDER BY Start DESC";
    private static final String SQL_QUERY_VIEWS_META =
            "SELECT Count(1) AS Count, SUM(TIMESTAMPDIFF(MINUTE, Start, End)) AS Runtime " +
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
            "SELECT Title, Start, MovieId, (DAY_OF_WEEK(Start) - 1) AS week_day, WasCinema FROM Views " +
                    "JOIN Movies ON Movies.Id = Views.MovieId " +
                    "WHERE Username = ? " +
                    "ORDER BY Start DESC LIMIT 5";
    private static final String SQL_GET_TOP_MOVIES =
            "SELECT MovieId, Title, COUNT(1) AS Count, Image FROM Views " +
                    "JOIN Movies ON Movies.Id = Views.MovieId " +
                    "WHERE Username = ? " +
                    "GROUP BY MovieId ORDER BY Count DESC LIMIT 5";
    private static final String SQL_REMOVE_SEASON =
            "DELETE FROM Series WHERE Username = ? AND SeasonId = ?;";
    private static final String SQL_INSERT_NEW_LIST =
            "INSERT INTO ListsInfo (Username, ListName, TimeCreated) VALUES (?, ?, ?);";
    private static final String SQL_GET_LISTS =
            "SELECT Id, ListName FROM ListsInfo WHERE Username = ? AND Active ORDER BY TimeCreated ASC;";
    private static final String SQL_REMOVE_FROM_LIST =
            "DELETE FROM ListEntries WHERE Username = ? AND ListId = ? AND MovieId = ?;";
    private static final String SQL_GET_IN_LISTS =
            "SELECT ListId FROM ListEntries WHERE Username = ? AND MovieId = ?;";
    private static final String SQL_GET_LIST_ENTRIES =
            "SELECT MovieId, Title, ListName, Year, Image, Time, ListId, " +
                    "MovieId IN (SELECT DISTINCT Views.MovieId " +
                    "FROM ListEntries " +
                    "JOIN Views ON ListEntries.MovieId = Views.MovieId " +
                    "WHERE Views.Username = ? AND ListEntries.Username = ? AND " +
                    "ListId = ?) AS Seen FROM ListEntries " +
                    "JOIN Movies ON ListEntries.MovieId = Movies.Id " +
                    "JOIN ListsInfo ON ListsInfo.Id = ListEntries.ListId " +
                    "WHERE ListEntries.Username = ? AND ListId = ? " +
                    "ORDER BY Time DESC;";
    private static final String SQL_CHANGE_LIST_NAME =
            "UPDATE ListsInfo SET ListName = ? WHERE Username = ? AND Id = ?;";
    private static final String SQL_DELETE_LIST =
            "UPDATE ListsInfo SET Active = 0 WHERE Username = ? AND Id = ?;";

    private JDBCClient client;

    protected DatabaseServiceImpl(Vertx vertx, JsonObject config, Configuration jooqConfig) {
        super(vertx.getDelegate(), jooqConfig);
        this.client = JDBCClient.createShared(vertx, config.getJsonObject("database"));
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
    public Future<Boolean> insertMovie(int id, String movieTitle, int year, String posterPath) {
      return asyncInsertIgnore(MOVIES, new Movies()
          .setId(id)
          .setTitle(movieTitle)
          .setYear((short) year)
          .setImage(posterPath));
    }

    @Override
    public Future<Boolean> insertSeries(int id, String seriesTitle, String posterPath) {
      return asyncInsertIgnore(SERIESINFO, new Seriesinfo()
          .setId(id)
          .setTitle(seriesTitle)
          .setImage(posterPath));
    }

    @Override
    public Future<Boolean> insertEpisodeView(String username, String jsonParam) {
        return asyncInsertIgnore(SERIES, new JsonObject(jsonParam)
            .mapTo(Series.class)
            .setTime(currentTimeMillis()));
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
      return asyncJson(dsl -> {
        SelectConditionStep<Record> where = dsl
            .select(date(min(VIEWS.START)).as("Start"))
            .select(count().as("Count"))
            .select(sum(timestampDiff(DatePart.MINUTE, VIEWS.START, VIEWS.END)).as("Runtime"))
            .from(VIEWS)
            .where(VIEWS.USERNAME.eq(username));
        JsonObject json = new JsonObject(jsonParam);
        ifTrue(json.getBoolean("is-first", false), () -> where.and(VIEWS.WASFIRST.eq((byte) 1)));
        ifTrue(json.getBoolean("is-cinema", false), () -> where.and(VIEWS.WASCINEMA.eq((byte) 1)));
        return where.fetchOneMap();
      });
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

    // TODO: 22/05/2018 test
  @Override
  public Future<Boolean> insertSeasonViews(String username, JsonObject seasonData, String seriesId) {
    JsonArray episodes = seasonData.getJsonArray("episodes");
    return episodes.isEmpty()
        ? Future.succeededFuture(false)
        : async(dsl -> {
      Stream<SeriesRecord> seriesRecords = episodes.stream()
          .map(obj -> (JsonObject) obj)
          .map(json -> new Series()
              .setUsername(username)
              .setSeriesid(Integer.parseInt(seriesId))
              .setEpisodeid(json.getInteger("id"))
              .setSeasonid(seasonData.getString("_id"))
              .setTime(currentTimeMillis()))
          .map(series -> dsl.newRecord(SERIES, series));
      return dsl.loadInto(SERIES)
          .onDuplicateKeyIgnore()
          .loadRecords(seriesRecords)
          .fields(SERIES.USERNAME, SERIES.SERIESID, SERIES.EPISODEID, SERIES.SEASONID, SERIES.TIME)
          .execute()
          .stored() > 0;
    });
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
      String sql = "SELECT Id " +
          "               ,ListName " +
          "               ,TimeCreated " +
          "           FROM ListsInfo " +
          "          WHERE Username = ? " +
          "            AND Active = 0 " +
          "          ORDER BY TimeCreated ASC;";
        return query(sql, new JsonArray().add(username));
    }

    @Override
    public Future<Boolean> insertIntoList(String username, String jsonParam) {
      return asyncInsertIgnore(LISTENTRIES, new JsonObject(jsonParam)
          .mapTo(Listentries.class)
          .setUsername(username)
          .setTime(currentTimeMillis()));
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
    public Future<String> getListName(String username, String listId) {
      return async(dsl -> dsl
          .select(LISTSINFO.LISTNAME)
          .from(LISTSINFO)
          .where(LISTSINFO.USERNAME.eq(username))
          .and(LISTSINFO.ID.eq(Integer.parseInt(listId)))
          .fetchOne()
          .value1());
    }

    @Override
    public Future<JsonArray> getLastListsHome(String username) {
      return asyncArray(dsl -> dsl
          .select(LISTENTRIES.LISTID, LISTSINFO.LISTNAME, MOVIES.ID, MOVIES.TITLE, MOVIES.YEAR)
          .from(LISTENTRIES)
          .join(MOVIES).on(MOVIES.ID.eq(LISTENTRIES.MOVIEID))
          .join(LISTSINFO).on(LISTSINFO.ID.eq(LISTENTRIES.LISTID))
          .where(LISTENTRIES.USERNAME.eq(username))
          .and(LISTSINFO.ACTIVE.isTrue())
          .orderBy(LISTENTRIES.TIME.desc())
          .limit(5)
          .fetchMaps());
    }

    @Override
    public Future<Boolean> restoreDeletedList(String username, String listId) {
      return async(dsl -> dsl
          .update(LISTSINFO)
          .set(LISTSINFO.ACTIVE, (byte) 1)
          .where(LISTSINFO.USERNAME.eq(username))
          .and(LISTSINFO.ID.eq(Integer.parseInt(listId)))
          .execute() > 0);
    }

  @Override
  public Future<JsonObject> getListsSize(String username) {
    String sql = "SELECT li.Id AS id " +
        "      ,COALESCE(COUNT(le.ListId), 0) AS count " +
        "      ,COALESCE((SELECT SUM(le2.MovieId IN (SELECT DISTINCT v.MovieId " +
        "                                                FROM Views v " +
        "                                               WHERE v.Username = li.Username)) " +
        "                   FROM ListEntries le2 " +
        "                  WHERE le2.Username = li.Username " +
        "                    AND le2.ListId = li.Id " +
        "                  GROUP BY le2.ListId), 0) AS seen " +
        "  FROM ListsInfo li " +
        "  LEFT JOIN ListEntries le ON le.ListId = li.Id " +
        " WHERE li.Username = ? " +
        "   AND li.Active = 1 " +
        " GROUP BY li.Id " +
        " ORDER BY li.TimeCreated ASC;";
    return query(sql, new JsonArray().add(username));
  }

    @Override
    public Future<Boolean> insertUserSeriesInfo(String username, String seriesId) {
        return async(dsl -> dsl()
                .insertInto(USERSERIESINFO)
                .columns(USERSERIESINFO.USERNAME, USERSERIESINFO.SERIESID)
                .values(username, Integer.parseInt(seriesId))
                .onDuplicateKeyIgnore()
                .execute() > 0);
    }

    @Override
    public Future<Boolean> toggleSeriesState(String username, String seriesId, boolean active) {
      return async(dsl -> dsl
          .update(USERSERIESINFO)
          .set(USERSERIESINFO.ACTIVE, active ? 1 : 0)
          .where(USERSERIESINFO.USERNAME.eq(username))
          .and(USERSERIESINFO.SERIESID.eq(Integer.parseInt(seriesId)))
          .execute() > 0);
    }

    @Override
    public Future<JsonArray> getInactiveSeries(String username) {
      return asyncArray(dsl -> dsl
          .select(SERIESINFO.TITLE)
          .select(SERIESINFO.IMAGE)
          .select(SERIES.SERIESID)
          .select(count(SERIES.SERIESID).as("Count"))
          .select(USERSERIESINFO.ACTIVE)
          .from(SERIES)
          .join(SERIESINFO).on(SERIES.SERIESID.eq(SERIESINFO.ID))
          .join(USERSERIESINFO).on(SERIES.SERIESID.eq(USERSERIESINFO.SERIESID))
          .where(SERIES.USERNAME.eq(username))
          .and(USERSERIESINFO.ACTIVE.isFalse())
          .groupBy(SERIES.SERIESID, USERSERIESINFO.ACTIVE)
          .orderBy(SERIESINFO.TITLE)
          .fetchMaps());
    }

  @Override
  public Future<JsonArray> getTodayInHistory(String username) {
    return asyncArray(dsl -> dsl
        .select(VIEWS.MOVIEID, MOVIES.TITLE, DSL.year(VIEWS.START).as("Year"), VIEWS.WASCINEMA)
        .from(VIEWS)
        .join(MOVIES).on(MOVIES.ID.eq(VIEWS.MOVIEID))
        .where(VIEWS.USERNAME.eq(username))
        .and(dayOfMonth(VIEWS.START).eq(dayOfMonth(DSL.currentTimestamp())))
        .and(DSL.month(VIEWS.START).eq(DSL.month(DSL.currentDate())))
        .orderBy(MOVIES.YEAR.desc())
        .fetchMaps());
  }

  @Override
  public Future<JsonObject> getHomeStatistics(String username) {
    return asyncJson(dsl -> dsl
        .select(count().as("total_views"))
        .select(sum(when(VIEWS.WASFIRST.isTrue(), 1)).cast(Integer.class).as("first_view"))
        .select(countDistinct(VIEWS.MOVIEID).as("unique_movies"))
        .select(sum(when(VIEWS.WASCINEMA.isTrue(), 1)).cast(Integer.class).as("total_cinema"))
        .select(sum(timestampDiff(DatePart.MINUTE, VIEWS.START, VIEWS.END)).cast(Integer.class).as("total_runtime"))
        .from(VIEWS)
        .where(VIEWS.USERNAME.eq(username))
        .fetchOneMap());
  }
}
