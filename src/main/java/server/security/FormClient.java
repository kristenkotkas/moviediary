package server.security;

import io.vertx.core.json.JsonObject;
import org.pac4j.core.client.IndirectClientV2;
import org.pac4j.core.client.RedirectAction;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.util.CommonHelper;
import server.service.DatabaseService;

import static server.router.UiRouter.UI_FORM_LOGIN;
import static server.util.CommonUtils.nonNull;
import static server.util.NetworkUtils.isServer;

public class FormClient extends IndirectClientV2<FormCredentials, FormProfile> {
    public static final String FORM_USERNAME = "username";
    public static final String FORM_PASSWORD = "password";
    public static final String FORM_FIRSTNAME = "firstname";
    public static final String FORM_LASTNAME = "lastname";

    public static final String ERROR_PARAMETER = "error";

    public static final String MISSING_FIELD_ERROR = "missing_field";

    private String loginUrl;

    // TODO: 19.02.2017 redirect back to login if invalid

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

/*    @Override
    protected FormCredentials retrieveCredentials(final WebContext context) throws HttpAction {
        CommonHelper.assertNotNull("credentialsExtractor", getCredentialsExtractor());
        CommonHelper.assertNotNull("authenticator", getAuthenticator());

        final String username = context.getRequestParameter(this.usernameParameter);
        UsernamePasswordCredentials credentials;
        try {
            // retrieve credentials
            credentials = getCredentialsExtractor().extract(context);
            logger.debug("usernamePasswordCredentials: {}", credentials);
            if (credentials == null) {
                throw handleInvalidCredentials(context, username, "Username and password cannot be blank -> return to the form with error", MISSING_FIELD_ERROR, 401);
            }
            // validate credentials
            getAuthenticator().validate(credentials, context);
        } catch (final CredentialsException e) {
            throw handleInvalidCredentials(context, username, "Credentials validation fails -> return to the form with error", computeErrorMessage(e), 403);
        }

        return credentials;
    }

    private HttpAction handleInvalidCredentials(final WebContext context, final String username, String message, String errorMessage, int errorCode) throws HttpAction {
        String redirectionUrl = addParameter(this.loginUrl, this.usernameParameter, username);
        redirectionUrl = addParameter(redirectionUrl, ERROR_PARAMETER, errorMessage);
        logger.debug("redirectionUrl: {}", redirectionUrl);
        logger.debug(message);
        return HttpAction.redirect(message, context, redirectionUrl);
    }*/

    protected String computeErrorMessage(final TechnicalException e) {
        return e.getClass().getSimpleName();
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
