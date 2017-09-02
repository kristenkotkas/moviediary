package database;

import io.vertx.core.Future;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.rxjava.core.AbstractVerticle;
import static database.DatabaseService.ADDRESS;
import static io.vertx.serviceproxy.ProxyHelper.registerService;
import static io.vertx.serviceproxy.ProxyHelper.unregisterService;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
public class DatabaseVerticle extends AbstractVerticle {
  private JDBCClient jdbcClient;
  private MessageConsumer<JsonObject> consumer;

  @Override
  public void start() throws Exception {
    jdbcClient = JDBCClient.createNonShared(vertx.getDelegate(), config().getJsonObject("jdbc"));
    DatabaseService database = DatabaseService.create(vertx.getDelegate(), config());
    consumer = registerService(DatabaseService.class, vertx.getDelegate(), database, ADDRESS);
  }

  @Override
  public void stop(Future<Void> future) throws Exception {
    unregisterService(consumer);
    jdbcClient.close(future.completer());
  }
}
