package mail;

import database.DatabaseService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;

import static io.vertx.serviceproxy.ProxyHelper.registerService;
import static io.vertx.serviceproxy.ProxyHelper.unregisterService;

public class MailVerticle extends AbstractVerticle {
  private MailClient mClient;
  private MessageConsumer<JsonObject> serviceConsumer;

  @Override
  public void start() throws Exception {
    DatabaseService databaseService = DatabaseService.createProxy(vertx, DatabaseService.SERVICE_ADDRESS);
    MailService mailService = MailService.create(mClient, databaseService);
    mClient = MailClient.createNonShared(vertx, new MailConfig().setTrustAll(true));
    serviceConsumer = registerService(MailService.class, vertx, mailService, MailService.SERVICE_ADDRESS);
  }

  @Override
  public void stop() throws Exception {
    unregisterService(serviceConsumer);
    mClient.close();
  }
}
