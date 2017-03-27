package server.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static java.util.stream.Collectors.toList;
import static server.service.DatabaseService.Column.USERNAME;

/**
 * Service which interacts with database.
 */
public interface DatabaseService {
    String DB_USERNAME = "Username";
    String DB_FIRSTNAME = "Firstname";
    String DB_LASTNAME = "Lastname";
    String DB_PASSWORD = "Password";
    String DB_SALT = "Salt";
    String DB_RUNTIME_TYPE = "RuntimeType";
    String DB_VERIFIED = "Verified";

    String COLUMNS = "columnNames";
    String ROWS = "rows";
    String RESULTS = "results";
    String COLUMN_NUMS = "numColumns";
    String ROWS_NUMS = "numRows";

    static EnumMap<Column, String> createDataMap(String username) {
        EnumMap<Column, String> map = new EnumMap<>(Column.class);
        map.put(USERNAME, username);
        return map;
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
        return json.getJsonArray(COLUMNS);
    }

    /**
     * Gets results as json objects in a json array.
     *
     * @param json to use
     */
    static JsonArray getRows(JsonObject json) {
        return json.getJsonArray(ROWS);
    }

    /**
     * Gets results as json arrays in a json array.
     *
     * @param json to use
     */
    static JsonArray getResults(JsonObject json) {
        return json.getJsonArray(RESULTS);
    }

    /**
     * Gets number of columns.
     *
     * @param json to use
     */
    static Integer getNumColumns(JsonObject json) {
        return json.getInteger(COLUMN_NUMS);
    }

    /**
     * Gets number of results.
     *
     * @param json to use
     */
    static Integer getNumRows(JsonObject json) {
        return json.getInteger(ROWS_NUMS);
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

    /**
     * Creates a SQL command string from given Table and list of Columns.
     * Does not set values.
     */
    enum SQLCommand {
        UPDATE((table, columns) -> {
            StringBuilder sb = new StringBuilder("UPDATE ")
                    .append(table.getName())
                    .append(" SET ");
            columns.stream()
                    .filter(column -> column != USERNAME)
                    .forEach(column -> sb
                            .append(columns.indexOf(column) == 0 ? "" : ", ")
                            .append(column.getName())
                            .append(" = ?"));
            return sb.append(" WHERE Username = ?").toString();
        }),
        INSERT((table, columns) -> {
            StringBuilder sb = new StringBuilder("INSERT INTO ")
                    .append(table.getName())
                    .append(" (");
            columns.forEach(column -> sb
                    .append(columns.indexOf(column) == 0 ? "" : ", ")
                    .append(column.getName()));
            sb.append(") VALUES (");
            columns.forEach(column -> sb
                    .append(columns.indexOf(column) == 0 ? "" : ", ")
                    .append("?"));
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

    static Handler<AsyncResult<SQLConnection>> connHandler(Future future, Handler<SQLConnection> handler) {
        return conn -> {
            if (conn.succeeded()) {
                handler.handle(conn.result());
            } else {
                future.fail(conn.cause());
            }
        };
    }

    /**
     * Convenience method for handling sql commands result.
     */
    static <T> Handler<AsyncResult<T>> resultHandler(SQLConnection conn, Future<JsonObject> future) {
        return ar -> {
            if (ar.succeeded()) {
                if (ar.result() instanceof ResultSet) {
                    future.complete(((ResultSet) ar.result()).toJson());
                } else {
                    future.complete(((UpdateResult) ar.result()).toJson());
                }
            } else {
                future.fail(ar.cause());
            }
            conn.close();
        };
    }
}
