package common.config;

import io.vertx.config.spi.ConfigStore;
import io.vertx.config.spi.ConfigStoreFactory;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
public class HttpBasicAuthConfigStoreFactory implements ConfigStoreFactory {

  @Override
  public String name() {
    return "http-basic-auth";
  }

  @Override
  public ConfigStore create(Vertx vertx, JsonObject configuration) {
    String host = configuration.getString("host");
    int port = configuration.getInteger("port", 80);
    String path = configuration.getString("path", "/");
    String username = configuration.getString("username", "");
    String password = configuration.getString("password", "");
    HttpClient client = vertx.createHttpClient(new HttpClientOptions()
        .setSsl(configuration.getBoolean("ssl", false))
        .setDefaultHost(host)
        .setDefaultPort(port)
        .setTrustAll(true));
    return new HttpBasicAuthConfigStore(client, path, username, password);
  }
}
