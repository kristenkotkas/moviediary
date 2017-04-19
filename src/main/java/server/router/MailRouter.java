package server.router;

import io.vertx.core.logging.Logger;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import server.service.MailService;

import static io.vertx.core.logging.LoggerFactory.getLogger;
import static server.entity.Status.*;
import static server.router.DatabaseRouter.DISPLAY_MESSAGE;
import static server.router.UiRouter.UI_LOGIN;
import static server.service.MailService.EMAIL;
import static server.service.MailService.UNIQUE;
import static server.util.CommonUtils.nonNull;

/**
 * Contains routes that handle email services.
 */
public class MailRouter extends EventBusRoutable {
    private static final Logger LOG = getLogger(MailRouter.class);
    public static final String API_MAIL_VERIFY = "/public/api/v1/mail/verify";

    private final MailService mail;

    public MailRouter(Vertx vertx, MailService mail) {
        super(vertx);
        this.mail = mail;
    }

    @Override
    public void route(Router router) {
        router.get(API_MAIL_VERIFY).handler(this::handleMailVerify);
    }

    /**
     * Verifies user email and redirects to login page.
     */
    private void handleMailVerify(RoutingContext ctx) {
        String email = ctx.request().getParam(EMAIL);
        String unique = ctx.request().getParam(UNIQUE);
        if (!nonNull(email, unique)) {
            badRequest(ctx);
            return;
        }
        mail.verifyEmail(email, unique)
                .rxSetHandler()
                .subscribe(r -> redirect(ctx, userVerified()), err -> serviceUnavailable(ctx, err));
    }

    public static String userVerified() {
        return UI_LOGIN + "?" + DISPLAY_MESSAGE + "=" + "LOGIN_VERIFIED";
    }
}
