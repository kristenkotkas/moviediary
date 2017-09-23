package database;

import common.verticle.rx.RestApiRxVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import static database.DatabaseService.ADDRESS;
import static io.vertx.serviceproxy.ProxyHelper.registerService;
import static io.vertx.serviceproxy.ProxyHelper.unregisterService;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
public class DatabaseVerticle extends RestApiRxVerticle {
  private JDBCClient jdbcClient;
  private MessageConsumer<JsonObject> consumer;

  @Override
  public void start(Future<Void> future) throws Exception {
    super.start();
    jdbcClient = JDBCClient.createNonShared(vertx.getDelegate(), config().getJsonObject("jdbc"));
    DatabaseService database = DatabaseService.create(vertx.getDelegate(), config());
    consumer = registerService(DatabaseService.class, vertx.getDelegate(), database, ADDRESS);
    startRouter(DatabaseService.ADDRESS, DatabaseService.class, future);
    // TODO: 3.09.2017 setup security for public users (getallusers should not be available for everyone)
  }

  @Override
  public void stop(Future<Void> future) throws Exception {
    unregisterService(consumer);
    jdbcClient.close(future.completer());
  }
}
