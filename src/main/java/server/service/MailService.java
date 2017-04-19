package server.service;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.RoutingContext;

/**
 * Service which interacts with a mail server.
 */
public interface MailService {
    String EMAIL = "email";
    String UNIQUE = "unique";

    static MailService create(Vertx vertx, DatabaseService database) {
        return new MailServiceImpl(vertx, database);
    }

    Future<JsonObject> sendVerificationEmail(RoutingContext ctx, String userEmail);

    Future<JsonObject> verifyEmail(String email, String unique);
}
