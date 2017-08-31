package common.config;

import io.vertx.config.spi.ConfigStore;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpHeaders;
import lombok.AllArgsConstructor;
import java.util.Base64;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@AllArgsConstructor
public class HttpBasicAuthConfigStore implements ConfigStore {
  private final HttpClient client;
  private final String path;
  private final String username;
  private final String password;

  @Override
  public void get(Handler<AsyncResult<Buffer>> handler) {
    client.get(path, res -> res
        .exceptionHandler(err -> handler.handle(Future.failedFuture(err)))
        .bodyHandler(body -> handler.handle(Future.succeededFuture(body))))
          .exceptionHandler(err -> handler.handle(Future.failedFuture(err)))
          .putHeader(HttpHeaders.AUTHORIZATION, getBasicAuthBase64())
          .end();
  }

  private String getBasicAuthBase64() {
    return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(UTF_8));
  }

  @Override
  public void close(Handler<Void> completionHandler) {
    client.close();
    completionHandler.handle(null);
  }
}
