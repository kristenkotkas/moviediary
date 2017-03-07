package server.security;

import org.pac4j.core.client.IndirectClientV2;
import org.pac4j.core.client.RedirectAction;
import org.pac4j.core.context.WebContext;

import static server.router.UiRouter.UI_LOGIN;

public class RedirectClient extends IndirectClientV2 {
    public static String REDIRECT_URL = "redirectUrl";

    @Override
    protected void internalInit(WebContext context) {
        super.internalInit(context);
        setRedirectActionBuilder(webContext -> {
            webContext.setSessionAttribute(REDIRECT_URL, webContext.getFullRequestURL());
            return RedirectAction.redirect(UI_LOGIN);
        });
    }
}
