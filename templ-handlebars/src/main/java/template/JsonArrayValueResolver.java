package template;

import com.github.jknack.handlebars.ValueResolver;
import io.vertx.core.json.JsonArray;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Json array resolver.
 * <p>
 * Original source: https://github.com/vert-x3/vertx-web/blob/master/vertx-template-engines/vertx-web-templ-handlebars/
 */
public class JsonArrayValueResolver implements ValueResolver {
  public static final ValueResolver INSTANCE = new JsonArrayValueResolver();

  @Override
  public Object resolve(Object context, String name) {
    if (context instanceof JsonArray) {
      JsonArray array = (JsonArray) context;
      if ("length".equals(name) || "size".equals(name)) {
        return array.size();
      }
      Object value = array.getValue(Integer.valueOf(name));
      if (value != null) {
        return value;
      }
    }
    return UNRESOLVED;
  }

  @Override
  public Object resolve(Object context) {
    if (context instanceof JsonArray) {
      return context;
    }
    return UNRESOLVED;
  }

  @Override
  public Set<Map.Entry<String, Object>> propertySet(Object context) {
    return Collections.emptySet();
  }
}
