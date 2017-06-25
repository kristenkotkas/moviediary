package database;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;

import static database.DatabaseService.SERVICE_ADDRESS;
import static io.vertx.serviceproxy.ProxyHelper.registerService;
import static io.vertx.serviceproxy.ProxyHelper.unregisterService;

public class DatabaseVerticle extends AbstractVerticle {
  private JDBCClient dbClient;
  private MessageConsumer<JsonObject> serviceConsumer;

  @Override
  public void start() throws Exception {
    dbClient = JDBCClient.createNonShared(vertx, config().getJsonObject("mysql"));
    serviceConsumer = registerService(DatabaseService.class, vertx, DatabaseService.create(dbClient), SERVICE_ADDRESS);
  }

  @Override
  public void stop(Future<Void> future) throws Exception {
    unregisterService(serviceConsumer);
    dbClient.close(future.completer());
  }
}
