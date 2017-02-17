package server.service;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface DatabaseService extends CachingService<JsonObject> {
    String CACHE_ALL = "all";

    String COLUMNS = "columnNames";
    String ROWS = "rows";
    String COLUMN_NUMS = "numColumns";
    String ROWS_NUMS = "numRows";

    static DatabaseService create(Vertx vertx, JsonObject config) {
        return new DatabaseServiceImpl(vertx, config);
    }

    Future<JsonObject> getAllUsers();

    Future<JsonObject> getAllViews();

    static JsonArray getColumns(JsonObject json) {
        return json.getJsonArray(COLUMNS);
    }

    static JsonArray getRows(JsonObject json) {
        return json.getJsonArray(ROWS);
    }

    static Integer getNumColumns(JsonObject json) {
        return json.getInteger(COLUMN_NUMS);
    }

    static Integer getNumRows(JsonObject json) {
        return json.getInteger(ROWS_NUMS);
    }
}
