package server.router;

import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import mail.rxjava.MailService;

import static server.entity.Status.*;
import static server.router.DatabaseRouter.DISPLAY_MESSAGE;
import static server.router.UiRouter.UI_LOGIN;
import static util.ConditionUtils.nonNull;

/**
 * Contains routes that handle email services.
 */
public class MailRouter extends EventBusRoutable {
  public static final String API_MAIL_VERIFY = "/public/api/v1/mail/verify";
  private final MailService mail;

  public MailRouter(Vertx vertx, mail.MailService mail) {
    super(vertx);
    this.mail = new MailService(mail);
  }

  public static String userVerified() {
    return UI_LOGIN + "?" + DISPLAY_MESSAGE + "=" + "LOGIN_VERIFIED";
  }

  @Override
  public void route(Router router) {
    router.get(API_MAIL_VERIFY).handler(this::handleMailVerify);
  }

  /**
   * Verifies user email and redirects to login page.
   */
  private void handleMailVerify(RoutingContext ctx) {
    String email = ctx.request().getParam("email");
    String unique = ctx.request().getParam("unique");
    if (!nonNull(email, unique)) {
      badRequest(ctx);
      return;
    }
    mail.rxVerifyEmail(email, unique)
        .subscribe(json -> redirect(ctx, userVerified()), err -> serviceUnavailable(ctx, err));
  }
}
