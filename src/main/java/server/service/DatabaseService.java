package server.service;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface DatabaseService extends CachingService<JsonObject> {
    String CACHE_ALL = "all";
    String CACHE_USER = "user_";

    String DB_USERNAME = "Username";
    String DB_PASSWORD = "Password";
    String DB_FIRSTNAME = "Firstname";
    String DB_LASTNAME = "Lastname";

    String COLUMNS = "columnNames";
    String ROWS = "rows";
    String RESULTS = "results";
    String COLUMN_NUMS = "numColumns";
    String ROWS_NUMS = "numRows";

    static DatabaseService create(Vertx vertx, JsonObject config) {
        return new DatabaseServiceImpl(vertx, config);
    }

    Future<JsonObject> insertUser(String username, String password, String firstname, String lastname);

    Future<JsonObject> getUser(String username);

    Future<JsonObject> getAllUsers();

    Future<JsonObject> getAllViews();

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
}
