package server.service;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Service which interacts with a mail server.
 */
public interface MailService extends CachingService<JsonObject> {
    String EMAIL = "email";
    String UNIQUE = "unique";

    static MailService create(Vertx vertx, JsonObject config, DatabaseService database) {
        return new MailServiceImpl(vertx, config, database);
    }

    Future<JsonObject> sendVerificationEmail(RoutingContext ctx, String userEmail);

    Future<JsonObject> verifyEmail(String email, String unique);
}
