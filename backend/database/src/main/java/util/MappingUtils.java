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
}
