package server.service;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * Service which interacts with a mail server.
 */
@VertxGen
@ProxyGen
public interface MailService {
  String EMAIL = "email";
  String UNIQUE = "unique";

  @GenIgnore
  static MailService create(Vertx vertx, DatabaseService database) {
    return new MailServiceImpl(vertx, database);
  }

  @Fluent
  MailService sendVerificationEmail(String lang, String userEmail, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  MailService verifyEmail(String email, String unique, Handler<AsyncResult<JsonObject>> handler);
}
