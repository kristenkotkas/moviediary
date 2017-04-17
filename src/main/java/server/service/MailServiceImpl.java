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

import static io.vertx.core.Future.future;
import static server.entity.Language.getString;
import static server.router.MailRouter.API_MAIL_VERIFY;
import static server.service.DatabaseService.*;
import static server.util.CommonUtils.check;
import static server.util.StringUtils.genString;

/**
 * Mail service implementation.
 */
public class MailServiceImpl implements MailService {
    private static final Logger LOG = LoggerFactory.getLogger(MailServiceImpl.class);
    private static final String FROM = "moviediary@kyngas.eu";

    private final DatabaseService database;
    private final MailClient client;

    protected MailServiceImpl(Vertx vertx, DatabaseService database) {
        this.database = database;
        this.client = MailClient.createNonShared(vertx, new MailConfig().setTrustAll(true));
    }

    /**
     * Sends verification mail to user.
     */
    @Override
    public Future<JsonObject> sendVerificationEmail(RoutingContext ctx, String userEmail) {
        String unique = genString();
        MailMessage email = new MailMessage()
                .setFrom(FROM)
                .setTo(userEmail)
                .setSubject(getString("MAIL_REGISTER_TITLE", ctx))
                .setHtml(createContent(ctx, userEmail, unique));
        Map<Column, String> data = createDataMap(userEmail);
        data.put(Column.VERIFIED, unique);
        return future(fut -> database.update(Table.SETTINGS, data).setHandler(ar -> check(ar.succeeded(),
                () -> client.sendMail(email, result -> check(result.succeeded(),
                        () -> fut.complete(result.result().toJson()),
                        () -> fut.fail("Failed to send email to user: " + result.cause()))),
                () -> fut.fail("Could not set unique verification string DB: " + ar.cause()))));
    }

    /**
     * Verifies user email based on unique code.
     */
    @Override
    public Future<JsonObject> verifyEmail(String email, String unique) {
        return future(fut -> database.getSettings(email).setHandler(ar -> check(ar.succeeded(),
                () -> check(getRows(ar.result()).getJsonObject(0).getString(DB_VERIFIED).equals(unique), () -> {
                    Map<Column, String> map = createDataMap(email);
                    map.put(Column.VERIFIED, "1");
                    database.update(Table.SETTINGS, map).setHandler(result -> check(result.succeeded(),
                            () -> fut.complete(result.result()),
                            () -> fut.fail("Failed to update user unique string DB: " + result.cause())));
                }, () -> fut.fail("User presented unique string does not match DB.")),
                () -> fut.fail("Failed to get user settings from DB: " + ar.cause()))));
    }

    /**
     * Creates email content that is displayed in users verification email.
     */
    private String createContent(RoutingContext ctx, String userEmail, String unique) {
        return "<p>" + getString("MAIL_REGISTER_TEXT", ctx) + "</p>" +
                "<a href=\"https://movies.kyngas.eu" + API_MAIL_VERIFY +
                "?" + EMAIL + "=" + userEmail +
                "&" + UNIQUE + "=" + unique + "\">" +
                getString("MAIL_REGISTER_CLICK_ME", ctx) +
                "</a>";
    }
}
