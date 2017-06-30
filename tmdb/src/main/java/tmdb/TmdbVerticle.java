package tmdb;

import database.DatabaseService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

import static io.vertx.serviceproxy.ProxyHelper.registerService;
import static io.vertx.serviceproxy.ProxyHelper.unregisterService;

public class TmdbVerticle extends AbstractVerticle {
  private WebClient webClient;
  private MessageConsumer<JsonObject> serviceConsumer;

  @Override
  public void start() throws Exception {
    webClient = WebClient.create(vertx, new WebClientOptions().setSsl(true).setKeepAlive(false));
    DatabaseService databaseService = DatabaseService.createProxy(vertx, DatabaseService.SERVICE_ADDRESS);
    TmdbService tmdbService = TmdbService.create(vertx, config(), webClient, databaseService);
    serviceConsumer = registerService(TmdbService.class, vertx, tmdbService, TmdbService.SERVICE_ADDRESS);
  }

  @Override
  public void stop() throws Exception {
    unregisterService(serviceConsumer);
    webClient.close();
  }
}
