package common.util;

import common.entity.Nameable;
import common.ex.ConfigValidationException;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import rx.Single;
import java.util.HashSet;
import java.util.Set;
import static common.util.FileUtils.isRunningFromJar;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@Slf4j
public class ConfigUtils {
  private static final String SHARED_MODULE = "shared";
  private static final String COMMON_SERVICE = "common";
  private static final String HTTP_PORT = "http.port";

  public static DeploymentOptions getDeployerConfig(JsonObject config, Nameable nameable) {
    String[] split = nameable.name().toLowerCase().split("_");
    String module = split[0];
    String service = split[1];
    return new DeploymentOptions().setConfig(config
        .getJsonObject(SHARED_MODULE, new JsonObject())
        .getJsonObject(COMMON_SERVICE, new JsonObject())
        .mergeIn(config
            .getJsonObject(SHARED_MODULE, new JsonObject())
            .getJsonObject(module, new JsonObject()))
        .mergeIn(config
            .getJsonObject(module, new JsonObject())
            .getJsonObject(service, new JsonObject())));
  }

  public static Single<JsonObject> validateConfig(JsonObject config) {
    //log.debug("Using configuration: {}", config.encodePrettily());
    httpPortValidation(config, "root", new HashSet<>());
    return Single.just(config);
  }

  private static void httpPortValidation(JsonObject config, String key, Set<Integer> knownPorts) {
    config.getMap().forEach((k, v) -> {
      if (v instanceof JsonObject) {
        httpPortValidation(((JsonObject) v), k, knownPorts);
      } else if (HTTP_PORT.equals(k)) {
        if (!(v instanceof Integer)) {
          throw new ConfigValidationException(key, "Http port value is not an integer: " + v);
        }
        Integer port = ((Integer) v);
        if (port == -1 && isRunningFromJar()) {
          throw new ConfigValidationException(key, "Production config contains invalid port: " + port);
        } else if (knownPorts.contains(port) && port != -1) {
          throw new ConfigValidationException(key, "Configuration contains duplicate port: " + port);
        }
        knownPorts.add(port);
      }
    });
  }
}
