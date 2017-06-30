package omdb;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

import static io.vertx.serviceproxy.ProxyHelper.registerService;
import static io.vertx.serviceproxy.ProxyHelper.unregisterService;

public class OmdbVerticle extends AbstractVerticle {
  private WebClient webClient;
  private MessageConsumer<JsonObject> serviceConsumer;

  @Override
  public void start() throws Exception {
    webClient = WebClient.create(vertx, new WebClientOptions().setSsl(true).setKeepAlive(false));
    OmdbService omdbService = OmdbService.create(vertx, config(), webClient);
    serviceConsumer = registerService(OmdbService.class, vertx, omdbService, OmdbService.SERVICE_ADDRESS);
  }

  @Override
  public void stop() throws Exception {
    unregisterService(serviceConsumer);
    webClient.close();
  }
}
