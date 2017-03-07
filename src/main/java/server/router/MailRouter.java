package server.router;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import server.service.MailService;

import static server.entity.Status.badRequest;
import static server.entity.Status.redirect;
import static server.router.DatabaseRouter.DISPLAY_MESSAGE;
import static server.router.UiRouter.UI_LOGIN;
import static server.service.MailService.EMAIL;
import static server.service.MailService.UNIQUE;
import static server.util.CommonUtils.getProfile;
import static server.util.CommonUtils.nonNull;
import static server.util.HandlerUtils.jsonResponse;
import static server.util.HandlerUtils.resultHandler;

/**
 * Contains routes that handle email services.
 */
public class MailRouter extends Routable {
    private static final Logger LOG = LoggerFactory.getLogger(MailRouter.class);
    private static final String API_MAIL_SEND = "/private/api/mail/send";
    public static final String API_MAIL_VERIFY = "/public/api/mail/verify";

    private final MailService mail;

    public MailRouter(Vertx vertx, MailService mail) {
        super(vertx);
        this.mail = mail;
    }

    @Override
    public void route(Router router) {
        // TODO: 3.03.2017 remove? 
        router.post(API_MAIL_SEND).handler(this::handleMailSend);
        router.get(API_MAIL_VERIFY).handler(this::handleMailVerify);
    }

    private void handleMailVerify(RoutingContext ctx) {
        String email = ctx.request().getParam(EMAIL);
        String unique = ctx.request().getParam(UNIQUE);
        if (!nonNull(email, unique)) {
            badRequest(ctx);
            return;
        }
        mail.verifyEmail(email, unique).setHandler(resultHandler(ctx,
                json -> redirect(ctx, UI_LOGIN + verified(ctx))));
    }

    private void handleMailSend(RoutingContext ctx) {
        mail.sendVerificationEmail(ctx, getProfile(ctx).getEmail()).setHandler(resultHandler(ctx, jsonResponse(ctx)));
    }

    private String verified(RoutingContext ctx) {
        return "?" + DISPLAY_MESSAGE + "=" + "LOGIN_VERIFIED";
    }
}
