package common.util.rx;

import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.config.ConfigRetriever;
import io.vertx.rxjava.core.Vertx;
import lombok.extern.slf4j.Slf4j;
import rx.Single;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@Slf4j
public class ConfigRxRetriever {
  private ConfigRetrieverOptions options = new ConfigRetrieverOptions();
  private Vertx vertx;

  private ConfigRxRetriever(Vertx vertx) {
    this.vertx = vertx;
  }

  public static ConfigRxRetriever createConfigRetriever(Vertx vertx) {
    return new ConfigRxRetriever(vertx);
  }

  // TODO: 31.08.2017 if jar -> load from server
  // TODO: 31.08.2017 if classes -> load from server (if has connection), override with local

  // TODO: 31.08.2017 listen for config updates

  public ConfigRxRetriever withEnvVars() {
    options.addStore(new ConfigStoreOptions()
        .setType("env"));
    return this;
  }

  public ConfigRxRetriever withSysParams() {
    options.addStore(new ConfigStoreOptions()
        .setType("sys"));
    return this;
  }

  public ConfigRxRetriever withHttp(String host, boolean ssl, String path) {
    options.addStore(new ConfigStoreOptions()
        .setType("http")
        .setConfig(new JsonObject()
            .put("host", host)
            .put("port", ssl ? 443 : 80)
            .put("ssl", ssl)
            .put("path", path)));
    return this;
  }

  public ConfigRxRetriever withHttpBasicAuth(String host, boolean ssl, String path,
                                             String username, String password) {
    options.addStore(new ConfigStoreOptions()
        .setType("http-basic-auth")
        .setConfig(new JsonObject()
            .put("host", host)
            .put("port", ssl ? 443 : 80)
            .put("ssl", ssl)
            .put("path", path)
            .put("username", username)
            .put("password", password)));
    return this;
  }

  public ConfigRxRetriever withHttpBasicAuth(JsonObject settings) {
    return withHttpBasicAuth(settings.getString("host"),
        settings.getBoolean("ssl"),
        settings.getString("path"),
        settings.getString("username"),
        settings.getString("password"));
  }

  public ConfigRxRetriever withEventbus(String configAddress) {
    options.addStore(new ConfigStoreOptions()
        .setType("event-bus")
        .setConfig(new JsonObject()
            .put("address", configAddress)));
    return this;
  }

  public ConfigRxRetriever withDirectory(String moduleName) {
    options.addStore(new ConfigStoreOptions()
        .setType("directory")
        .setConfig(new JsonObject()
            .put("path", moduleName + "/src/config")
            .put("filesets", new JsonArray()
                .add(new JsonObject().put("pattern", "*.json"))
                .add(new JsonObject().put("pattern", "*.properties").put("format", "properties")))));
    return this;
  }

  public ConfigRxRetriever withJsonFile(String name) {
    options.addStore(new ConfigStoreOptions()
        .setType("file")
        .setConfig(new JsonObject()
            .put("path", "")));
    return this;
  }

  public ConfigRxRetriever withPropertyFile(String name) {
    options.addStore(new ConfigStoreOptions()
        .setType("file")
        .setFormat("properties")
        .setConfig(new JsonObject()
            .put("path", "")));
    return this;
  }

  public ConfigRxRetriever withJson(JsonObject json) {
    options.addStore(new ConfigStoreOptions()
        .setType("json")
        .setConfig(json.copy()));
    return this;
  }

  public Single<ConfigRetriever> rxBuildRetriever() {
    ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
    return Single.just(retriever).doOnUnsubscribe(retriever::close);
  }
}
