package server.service;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.web.RoutingContext;
import server.service.DatabaseService.*;

import java.util.Map;

import static server.entity.Language.getString;
import static server.router.MailRouter.API_MAIL_VERIFY;
import static server.service.DatabaseService.*;
import static server.util.StringUtils.genString;

/**
 * Mail service implementation.
 */
public class MailServiceImpl extends CachingServiceImpl<JsonObject> implements MailService {
    private static final Logger LOG = LoggerFactory.getLogger(MailServiceImpl.class);
    private static final String FROM = "moviediary@kyngas.eu";

    private final DatabaseService database;
    private final MailClient client;

    protected MailServiceImpl(Vertx vertx, DatabaseService database) {
        super(DEFAULT_MAX_CACHE_SIZE);
        this.database = database;
        this.client = MailClient.createNonShared(vertx, new MailConfig().setTrustAll(true));
    }

    /**
     * Sends verification mail to user.
     */
    @Override
    public Future<JsonObject> sendVerificationEmail(RoutingContext ctx, String userEmail) {
        Future<JsonObject> future = Future.future();
        String unique = genString();
        MailMessage email = new MailMessage()
                .setFrom(FROM)
                .setTo(userEmail)
                .setSubject(getString("MAIL_REGISTER_TITLE", ctx))
                .setHtml(createContent(ctx, userEmail, unique));
        Map<Column, String> data = createDataMap(userEmail);
        data.put(Column.VERIFIED, unique);
        database.update(Table.SETTINGS, data).setHandler(ar -> {
            if (ar.succeeded()) {
                client.sendMail(email, result -> {
                    if (result.succeeded()) {
                        future.complete(result.result().toJson());
                    } else {
                        future.fail("Failed to send email to user: " + result.cause());
                    }
                });
            } else {
                future.fail("Could not set unique verification string in DB: " + ar.cause());
            }
        });
        return future;
    }

    /**
     * Verifies user email based on unique code.
     */
    @Override
    public Future<JsonObject> verifyEmail(String email, String unique) {
        Future<JsonObject> future = Future.future();
        database.getSettings(email).setHandler(ar -> {
            if (ar.succeeded()) {
                if (getRows(ar.result()).getJsonObject(0).getString(DB_VERIFIED).equals(unique)) {
                    Map<Column, String> map = createDataMap(email);
                    map.put(Column.VERIFIED, "1");
                    database.update(Table.SETTINGS, map).setHandler(result -> {
                        if (result.succeeded()) {
                            future.complete(result.result());
                        } else {
                            future.fail("Failed to update user unique string in DB: " + result.cause());
                        }
                    });
                } else {
                    future.fail("User presented unique string does not match DB.");
                }
            } else {
                future.fail("Failed to get user settings from DB: " + ar.cause());
            }
        });
        return future;
    }

    /**
     * Creates email content that is displayed in users verification email.
     */
    private String createContent(RoutingContext ctx, String userEmail, String unique) {
        return "<p>" + getString("MAIL_REGISTER_TEXT", ctx) + "</p>" +
                "<a href=\"https://movies.kyngas.eu" + API_MAIL_VERIFY +
                "?" + EMAIL + "=" + userEmail +
                "&" + UNIQUE + "=" + unique +
                "\">" + getString("MAIL_REGISTER_CLICK_ME", ctx) + "</a>";
    }
}
