package template;

import com.github.jknack.handlebars.ValueResolver;
import io.vertx.core.json.JsonObject;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * JsonObject resolver.
 * If context is instance of Map.Entry -> will get Entry value and try to resolve it.
 */
public class JsonObjectValueResolver implements ValueResolver {
  public static final ValueResolver INSTANCE = new JsonObjectValueResolver();

  @Override
  public Object resolve(Object context, String name) {
    Object value = tryResolve(context, name);
    if (value != null) {
      return value;
    }
    if (context instanceof Map.Entry) {
      value = tryResolve(((Map.Entry) context).getValue(), name);
      if (value != null) {
        return value;
      }
    }
    return UNRESOLVED;
  }

  private Object tryResolve(Object context, String name) {
    if (context instanceof JsonObject) {
      return ((JsonObject) context).getValue(name);
    }
    return null;
  }

  @Override
  public Object resolve(Object context) {
    if (context instanceof JsonObject) {
      return context;
    }
    return UNRESOLVED;
  }

  @Override
  public Set<Map.Entry<String, Object>> propertySet(Object context) {
    if (context instanceof JsonObject) {
      return ((JsonObject) context).getMap().entrySet();
    }
    return Collections.emptySet();
  }
}