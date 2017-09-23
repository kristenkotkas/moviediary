package util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
public class MappingUtils {

  public static JsonArray getRows(JsonObject json) {
    return json.getJsonArray("rows", new JsonArray());
  }

  public static JsonObject toRowsJson(JsonObject json) {
    return new JsonObject().put("rows", getRows(json));
  }
}
