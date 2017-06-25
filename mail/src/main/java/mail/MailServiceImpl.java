package mail;

import database.DatabaseService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.MailResult;
import io.vertx.rx.java.RxHelper;
import io.vertx.rxjava.ext.mail.MailClient;
import rx.Single;
import util.RxUtils;

import static entity.Language.getString;
import static util.JsonUtils.getRows;
import static util.StringUtils.genString;

/**
 * Mail service implementation.
 */
class MailServiceImpl implements MailService {
  private static final String FROM = "moviediary@kyngas.eu";

  private final database.rxjava.DatabaseService database;
  private final MailClient client;

  protected MailServiceImpl(io.vertx.ext.mail.MailClient client, DatabaseService database) {
    this.database = new database.rxjava.DatabaseService(database);
    this.client = new MailClient(client);
  }

  /**
   * Sends verification mail to user.
   */
  @Override
  public MailService sendVerificationEmail(String lang, String email,
                                           Handler<AsyncResult<JsonObject>> handler) { // TODO: 19.06.2017 test 
    String unique = genString();
    RxUtils.<JsonObject>single(h -> database.updateUserVerifyStatus(email, unique, h))
        .doOnError(err -> Future
            .<JsonObject>failedFuture("Could not set unique verification string DB: " + err)
            .setHandler(handler))
        .flatMap(json -> client.rxSendMail(new MailMessage()
            .setFrom(FROM)
            .setTo(email)
            .setSubject(getString("MAIL_REGISTER_TITLE", lang))
            .setHtml(createContent(lang, email, unique))))
        .map(MailResult::toJson)
        .subscribe(RxHelper.toSubscriber(handler));
    return this;
/*    return future(fut -> database.update(Table.SETTINGS, data).setHandler(ar -> check(ar.succeeded(),
            () -> client.sendMail(email, result -> check(result.succeeded(),
                    () -> fut.complete(result.result().toJson()),
                    () -> fut.fail("Failed to send email to user: " + result.cause()))),
            () -> fut.fail("Could not set unique verification string DB: " + ar.cause()))));*/
  }

  /**
   * Verifies user email based on unique code.
   */
  @Override
  public MailService verifyEmail(String email, String unique, Handler<AsyncResult<JsonObject>> handler) { // TODO: 19.06.2017 test
    database.rxGetSettings(email)
        .doOnError(err -> Future
            .<JsonObject>failedFuture("Failed to get user settings from DB: " + err)
            .setHandler(handler))
        .map(json -> getRows(json).getJsonObject(0).getString("Verified").equals(unique))
        .flatMap(isValid -> {
          if (!isValid) {
            return Single.<JsonObject>error(new Throwable("User presented unique string does not match DB."));
          }
          return Single.just(true);
        })
        .flatMap(b -> RxUtils.<JsonObject>single(h -> database.updateUserVerifyStatus(email, "1", h)))
        .subscribe(RxHelper.toSubscriber(handler));
    return this;
/*    return future(fut -> database.getSettings(email).setHandler(ar -> check(ar.succeeded(),
        () -> check(getRows(ar.result()).getJsonObject(0)
            .getString(Column.VERIFIED.getName()).equals(unique), () -> {
          Map<Column, String> map = createDataMap(email);
          map.put(Column.VERIFIED, "1");
          database.update(Table.SETTINGS, map).setHandler(result -> check(result.succeeded(),
              () -> fut.complete(result.result()),
              () -> fut.fail("Failed to update user unique string DB: " + result.cause())));
        }, () -> fut.fail("User presented unique string does not match DB.")),
        () -> fut.fail("Failed to get user settings from DB: " + ar.cause()))));*/
  }

  /**
   * Creates email content that is displayed in users verification email.
   */
  private String createContent(String lang, String userEmail, String unique) {
    return "<p>" + getString("MAIL_REGISTER_TEXT", lang) + "</p>" +
        "<a href=\"https://movies.kyngas.eu" + "/public/api/v1/mail/verify" +
        "?" + "email" + "=" + userEmail +
        "&" + "unique" + "=" + unique + "\">" +
        getString("MAIL_REGISTER_CLICK_ME", lang) +
        "</a>";
  }
}
