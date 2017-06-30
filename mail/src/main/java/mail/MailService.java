package mail;

import database.DatabaseService;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailClient;

/**
 * Service which interacts with a mail server.
 */
@VertxGen
@ProxyGen
public interface MailService {
  String SERVICE_NAME = "mail-eventbus-service";
  String SERVICE_ADDRESS = "service.mail";

  static MailService create(MailClient mClient, DatabaseService database) {
    return new MailServiceImpl(mClient, database);
  }

  static MailService createProxy(Vertx vertx, String address) {
    return new MailServiceVertxEBProxy(vertx, address);
  }

  @Fluent
  MailService sendVerificationEmail(String lang, String userEmail, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  MailService verifyEmail(String email, String unique, Handler<AsyncResult<JsonObject>> handler);
}
