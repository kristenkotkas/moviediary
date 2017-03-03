package server.service;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import server.service.DatabaseService.*;

import java.util.Map;

import static server.router.MailRouter.API_MAIL_VERIFY;
import static server.service.DatabaseService.*;
import static server.util.StringUtils.genString;

public class MailServiceImpl extends CachingServiceImpl<JsonObject> implements MailService {
    private static final String FROM = "moviediary@kyngas.eu";

    private final Vertx vertx;
    private final JsonObject config;
    private final DatabaseService database;
    private final MailClient client;

    protected MailServiceImpl(Vertx vertx, JsonObject config, DatabaseService database) {
        super(DEFAULT_MAX_CACHE_SIZE);
        this.vertx = vertx;
        this.config = config;
        this.database = database;
        this.client = MailClient.createNonShared(vertx, new MailConfig().setTrustAll(true));
    }

    /*
    * verified
    * 0 -> false
    * 1 -> true
    * else -> false, contains unique verification string
    *
    * */

    // TODO: 3.03.2017 different languages?
    @Override
    public Future<JsonObject> sendVerificationEmail(String userEmail) {
        Future<JsonObject> future = Future.future();
        String unique = genString();
        MailMessage email = new MailMessage()
                .setFrom(FROM)
                .setTo(userEmail)
                .setSubject("MovieDiary account registration verification")
                .setHtml(createContent(userEmail, unique));
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

    private String createContent(String userEmail, String unique) {
        return "<p>Thank you for registering, click the following link to verify your account.</p>" +
                "<a href=\"https://movies.kyngas.eu" + API_MAIL_VERIFY +
                "?" + EMAIL + "=" + userEmail +
                "&" + UNIQUE + "=" + unique +
                "\">Click me</a>";
    }
}
