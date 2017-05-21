package server.security;

import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.redirect.RedirectAction;

import static server.router.UiRouter.UI_LOGIN;

public class RedirectClient extends IndirectClient<Credentials, CommonProfile> {
    public static String REDIRECT_URL = "redirectUrl";

    @Override
    protected void clientInit(WebContext context) {
        setRedirectActionBuilder(webContext -> {
            webContext.setSessionAttribute(REDIRECT_URL, webContext.getFullRequestURL());
            return RedirectAction.redirect(UI_LOGIN);
        });
        setCredentialsExtractor(ctx -> null);
        setAuthenticator((credentials, context1) -> {
        });
    }
}
