package server.service;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;
import server.util.CommonUtils;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static java.util.stream.Collectors.toList;
import static server.service.DatabaseService.Column.USERNAME;

/**
 * Service which interacts with database.
 */
public interface DatabaseService {

    static Map<Column, String> createDataMap(String username) {
        return CommonUtils.<Column, String>mapBuilder()
                .put(USERNAME, username)
                .build();
    }

    static DatabaseService create(Vertx vertx, JsonObject config) {
        return new DatabaseServiceImpl(vertx, config);
    }

    /**
     * Gets columns as json array.
     *
     * @param json to use
     */
    static JsonArray getColumns(JsonObject json) {
        return json.getJsonArray("columnNames");
    }

    /**
     * Gets results as json objects in a json array.
     *
     * @param json to use
     */
    static JsonArray getRows(JsonObject json) {
        return json.getJsonArray("rows");
    }

    /**
     * Gets results as json arrays in a json array.
     *
     * @param json to use
     */
    static JsonArray getResults(JsonObject json) {
        return json.getJsonArray("results");
    }

    /**
     * Gets number of columns.
     *
     * @param json to use
     */
    static Integer getNumColumns(JsonObject json) {
        return json.getInteger("numColumns");
    }

    /**
     * Gets number of results.
     *
     * @param json to use
     */
    static Integer getNumRows(JsonObject json) {
        return json.getInteger("numRows");
    }

    Future<JsonObject> insertUser(String username, String password, String firstname, String lastname);

    Future<JsonObject> insertMovie(int id, String movieTitle, int year, String posterPath);

    Future<JsonObject> insertWishlist(String username, int movieId);

    Future<JsonObject> insertView(String user, String param);

    Future<JsonObject> isInWishlist(String username, int movieId);

    Future<JsonObject> getWishlist(String username);

    Future<JsonObject> getSettings(String username);

    Future<JsonObject> update(Table table, Map<Column, String> data);

    Future<JsonObject> insert(Table table, Map<Column, String> data);

    Future<JsonObject> getUser(String username);

    Future<JsonObject> getAllUsers();

    Future<JsonObject> getViews(String username, String param, int page);

    Future<JsonObject> getMovieViews(String username, String param);

    Future<String> getUsersCount();

    Future<JsonObject> getYearsDist(String username, String param);

    Future<JsonObject> getWeekdaysDist(String username, String param);

    Future<JsonObject> getTimeDist(String username, String param);

    Future<JsonObject> getAllTimeMeta(String username, String param);

    Future<JsonObject> getViewsMeta(String username, String param);

    Future<JsonObject> removeView(String username, String param);

    Future<JsonObject> removeEpisode(String username, String param);

    Future<JsonObject> insertEpisodeView(String username, String param);

    Future<JsonObject> getSeenEpisodes(String username, int seriesId);

    Future<JsonObject> insertSeries(int id, String seriesTitle, String posterPath);

    Future<JsonObject> getWatchingSeries(String username);

    /**
     * Creates a SQL command string from given Table and list of Columns.
     * Does not set values.
     */
    enum SQLCommand {
        UPDATE((table, columns) -> {
            StringBuilder sb = new StringBuilder("UPDATE ").append(table.getName()).append(" SET ");
            columns.stream()
                    .filter(column -> column != USERNAME)
                    .forEach(column -> sb
                            .append(columns.indexOf(column) == 0 ? "" : ", ").append(column.getName()).append(" = ?"));
            return sb.append(" WHERE Username = ?").toString();
        }),
        INSERT((table, columns) -> {
            StringBuilder sb = new StringBuilder("INSERT INTO ").append(table.getName()).append(" (");
            columns.forEach(column -> sb.append(columns.indexOf(column) == 0 ? "" : ", ").append(column.getName()));
            sb.append(") VALUES (");
            columns.forEach(column -> sb.append(columns.indexOf(column) == 0 ? "" : ", ").append("?"));
            return sb.append(")").toString();
        });

        private final BiFunction<Table, List<Column>, String> commandCreator;

        SQLCommand(BiFunction<Table, List<Column>, String> commandCreator) {
            this.commandCreator = commandCreator;
        }

        public String create(Table table, List<Column> columns) {
            return commandCreator.apply(table, columns);
        }
    }

    /**
     * Tables used in database.
     */
    enum Table {
        MOVIES("Movies"),
        SETTINGS("Settings"),
        USERS("Users"),
        VIEWS("Views"),
        WISHLIST("Wishlist");

        private final String tableName;

        Table(String tableName) {
            this.tableName = tableName;
        }

        public String getName() {
            return tableName;
        }
    }

    /**
     * Columns used in database.
     */
    enum Column {
        FIRSTNAME("Firstname"),
        LASTNAME("Lastname"),
        USERNAME("Username"),
        PASSWORD("Password"),
        SALT("Salt"),
        RUNTIMETYPE("RuntimeType"),
        VERIFIED("Verified");

        private final String columnName;

        Column(String columnName) {
            this.columnName = columnName;
        }

        public static List<Column> getSortedColumns(Map<Column, String> data) {
            List<Column> columns = data.keySet().stream()
                    .filter(column -> column != USERNAME)
                    .collect(toList());
            columns.add(USERNAME);
            return columns;
        }

        public static JsonArray getSortedValues(List<Column> columns, Map<Column, String> data) {
            return columns.stream()
                    .map(data::get)
                    .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
        }

        public String getName() {
            return columnName;
        }
    }
}
