package common.ex;

import io.vertx.core.VertxException;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
public class ConfigValidationException extends VertxException {

  public ConfigValidationException(String key, String message) {
    super("Config: " + key + " - " + message);
  }
}
