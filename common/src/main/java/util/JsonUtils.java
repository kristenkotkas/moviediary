package util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class JsonUtils {

  public static JsonArray getRows(JsonObject json) {
    return json.getJsonArray("rows");
  }
}
