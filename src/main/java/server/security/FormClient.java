package server.security;

import io.vertx.core.json.JsonObject;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.context.Cookie;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.redirect.RedirectAction;
import org.pac4j.core.util.CommonHelper;
import server.service.DatabaseService;
import static org.pac4j.core.util.CommonHelper.isBlank;
import static server.router.UiRouter.UI_FORM_LOGIN;
import static server.security.SecurityConfig.CSRF_TOKEN;
import static server.util.CommonUtils.nonNull;
import static server.util.NetworkUtils.isServer;

/**
 * Indirect client for form login.
 */
public class FormClient extends IndirectClient<FormCredentials, FormProfile> {
    public static final String FORM_USERNAME = "username";
    public static final String FORM_PASSWORD = "password";
    public static final String FORM_FIRSTNAME = "firstname";
    public static final String FORM_LASTNAME = "lastname";

    private String loginUrl;

    @Override
    protected void clientInit(WebContext context) {
        setRedirectActionBuilder(webContext -> RedirectAction.redirect(urlResolver.compute(loginUrl, context)));
        setCredentialsExtractor(ctx -> {
            String username = ctx.getRequestParameter(FORM_USERNAME);
            String password = ctx.getRequestParameter(FORM_PASSWORD);
            if (!nonNull(username, password)) {
                return null;
            }
            return new FormCredentials(username, password);
        });
    }

    public void enable(DatabaseService database, JsonObject config) {
        // TODO: 17.02.2017 store in config or smth
        loginUrl = (isServer(config) ? "https://moviediary.eu" :
                "http://localhost:" + config.getString("http_port")) + UI_FORM_LOGIN;
        setAuthenticator((credentials, context) -> {
            if (credentials == null) {
                throw new CredentialsException("No credentials provided.");
            }
            String email = credentials.getEmail();
            String password = credentials.getPassword();
            if (isBlank(email)) {
                throw new CredentialsException("Username cannot be blank.");
            }
            if (isBlank(password)) {
                throw new CredentialsException("Password cannot be blank.");
            }
            String csrfToken = context.getRequestCookies().stream()
                    .filter(cookie -> cookie.getName().equals(CSRF_TOKEN))
                    .findAny()
                    .map(Cookie::getValue)
                    .orElse(null);
            String sessionCsrfToken = (String) context.getSessionAttribute(CSRF_TOKEN);
            context.setSessionAttribute(CSRF_TOKEN, null);
            if (!nonNull(csrfToken, sessionCsrfToken) || !csrfToken.equals(sessionCsrfToken)) {
                throw new CredentialsException("Csrf check failed.");
            }
            credentials.setUserProfile(new FormProfile(email, password, database));
        });
    }

    @Override
    public String toString() {
        return CommonHelper.toString(getClass(),
                "callbackUrl", callbackUrl,
                "name", getName(),
                "loginUrl", loginUrl,
                "redirectActionBuilder", getRedirectActionBuilder(),
                "extractor", getCredentialsExtractor(),
                "authenticator", getAuthenticator(),
                "profileCreator", getProfileCreator());
    }
}
