package server.service;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.mail.MailClient;
import io.vertx.rxjava.ext.web.RoutingContext;
import server.service.DatabaseService.Column;
import server.service.DatabaseService.Table;

import java.util.Map;

import static io.vertx.rxjava.core.Future.future;
import static server.entity.Language.getString;
import static server.router.MailRouter.API_MAIL_VERIFY;
import static server.service.DatabaseService.createDataMap;
import static server.service.DatabaseService.getRows;
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
    // TODO: 19.04.2017 rewrite with singles
    String unique = genString();
    MailMessage email = new MailMessage()
        .setFrom(FROM)
        .setTo(userEmail)
        .setSubject(getString("MAIL_REGISTER_TITLE", ctx))
        .setHtml(createContent(ctx, userEmail, unique));
    Map<Column, String> data = createDataMap(userEmail); // TODO: 19.04.2017 map builder?
    data.put(Column.VERIFIED, unique);
    return future(fut -> database.update(Table.SETTINGS, data).setHandler(ar -> check(ar.succeeded(),
        () -> client.sendMail(email, result -> check(result.succeeded(),
            () -> fut.complete(result.result().toJson()),
            () -> fut.fail("Failed to send email to user: " + result.cause()))),
        () -> fut.fail("Could not set unique verification string DB: " + ar.cause()))));
        /*return future(fut -> database.update(Table.SETTINGS, data)
                .rxSetHandler()
                .doOnError(err -> fut.fail("Could not set unique verification string DB: " + err))
                .toCompletable()
                .andThen(client.rxSendMail(email))
                .doOnError(err -> fut.fail("Failed to send email to user: " + err))
                .map(MailResult::toJson)
                .subscribe(fut::complete));*/
  }

  /**
   * Verifies user email based on unique code.
   */
  @Override
  public Future<JsonObject> verifyEmail(String email, String unique) {
    return future(fut -> database.getSettings(email).setHandler(ar -> check(ar.succeeded(),
        () -> check(getRows(ar.result()).getJsonObject(0)
            .getString(Column.VERIFIED.getName()).equals(unique), () -> {
          Map<Column, String> map = createDataMap(email);
          map.put(Column.VERIFIED, "1");
          database.update(Table.SETTINGS, map).setHandler(result -> check(result.succeeded(),
              () -> fut.complete(result.result()),
              () -> fut.fail("Failed to update user unique string DB: " + result.cause())));
        }, () -> fut.fail("User presented unique string does not match DB.")),
        () -> fut.fail("Failed to get user settings from DB: " + ar.cause()))));


        /*return future(fut -> database.getSettings(email)
                .rxSetHandler()
                .doOnError(err -> fut.fail("Failed to get user settings from DB: " + err))
                .map(json -> getRows(json).getJsonObject(0).getString(Column.VERIFIED.getName()).equals(unique))
                .toObservable()
                .flatMap(bool -> ifThen(() -> bool, database.update(Table.SETTINGS, ImmutableMap
                                .<Column, String>builder()
                                .put(Column.VERIFIED, "1").build())
                                .rxSetHandler()
                                .toObservable(),
                        Observable.error(new Throwable("User presented unique string does not match DB."))))
                .toSingle()
                .doOnError(err -> fut.fail("Failed to update user unique string DB: " + err))
                .subscribe(fut::complete));*/
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
