package common.config;

import io.vertx.config.spi.ConfigStore;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Base64;
import static common.util.ConditionUtils.ifMissing;
import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static io.vertx.core.http.HttpHeaders.AUTHORIZATION;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@Slf4j
@RequiredArgsConstructor
public class HttpBasicAuthConfigStore implements ConfigStore {
  private final HttpClient client;
  private final String path;
  private final String username;
  private final String password;

  private Buffer cache;
  private boolean firstFetched = false;

  @Override
  public void get(Handler<AsyncResult<Buffer>> handler) {
    // FIXME: 26.09.2017 creating ConfigRetriever triggers config fetching without waiting for result
    //ignore first fetch for now
    if (!firstFetched) {
      firstFetched = true;
      handler.handle(Future.failedFuture("nope"));
      return;
    }
    if (cache != null) {
      handler.handle(succeededFuture(cache));
      return;
    }
    log.info("Fetching config from server...");
    client.get(path, res -> res
        .exceptionHandler(err -> handler.handle(failedFuture(err)))
        .bodyHandler(body -> handler.handle(succeededFuture(ifMissing(cache, () -> cache = body)))))
          .exceptionHandler(err -> handler.handle(failedFuture(err)))
          .putHeader(AUTHORIZATION, getBasicAuthBase64())
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
