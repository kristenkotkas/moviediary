package server.security;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.pac4j.core.client.IndirectClientV2;
import org.pac4j.core.client.RedirectAction;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.util.CommonHelper;
import server.service.DatabaseService;

import static server.router.UiRouter.UI_FORM_LOGIN;
import static server.util.CommonUtils.nonNull;
import static server.util.NetworkUtils.isServer;

public class FormClient extends IndirectClientV2<FormCredentials, FormProfile> {
    private static final Logger log = LoggerFactory.getLogger(FormClient.class);
    public static final String FORM_USERNAME = "username";
    public static final String FORM_PASSWORD = "password";
    public static final String FORM_FIRSTNAME = "firstname";
    public static final String FORM_LASTNAME = "lastname";

    private String loginUrl;

    @Override
    protected void internalInit(WebContext context) {
        super.internalInit(context);
        CommonHelper.assertNotBlank("loginUrl", loginUrl);
        loginUrl = callbackUrlResolver.compute(loginUrl, context);
        setRedirectActionBuilder(webContext -> RedirectAction.redirect(loginUrl));
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
        loginUrl = (isServer(config) ? "https://movies.kyngas.eu" : "http://localhost:8081") + UI_FORM_LOGIN;
        setAuthenticator((credentials, context) -> {
            if (credentials == null) {
                throw new CredentialsException("No credentials provided.");
            }
            String email = credentials.getEmail();
            String password = credentials.getPassword();
            if (CommonHelper.isBlank(email)) {
                throw new CredentialsException("Username cannot be blank.");
            }
            if (CommonHelper.isBlank(password)) {
                throw new CredentialsException("Password cannot be blank.");
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
