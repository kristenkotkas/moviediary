package server.router;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.auth.AuthProvider;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.ext.web.handler.CookieHandler;
import io.vertx.rxjava.ext.web.handler.SessionHandler;
import io.vertx.rxjava.ext.web.handler.UserSessionHandler;
import io.vertx.rxjava.ext.web.sstore.LocalSessionStore;
import org.pac4j.vertx.handler.impl.*;
import server.entity.Language;
import server.security.SecurityConfig;

import static server.router.UiRouter.UI_HOME;
import static server.security.SecurityConfig.AUTHORIZER;
import static server.security.SecurityConfig.AuthClient.getClientNames;
import static server.util.NetworkUtils.MAX_BODY_SIZE;
import static server.util.NetworkUtils.isServer;

/**
 * Contains routes that require authentication and authorization.
 * Sets up body handling, cookie handling and user session handling.
 */
public class AuthRouter extends EventBusRoutable {
    private static final Logger LOG = LoggerFactory.getLogger(AuthRouter.class);
    private static final String XSS_PROTECTION = "xssprotection";
    private static final String CALLBACK = "/callback";
    public static final String AUTH_PRIVATE = "/private/*";
    public static final String AUTH_LOGOUT = "/logout";

    public static final String TRANSLATIONS = "translations";
    public static final String MESSENGER = "messenger";

    private final JsonObject config;
    private final SecurityConfig securityConfig;

    public AuthRouter(Vertx vertx, JsonObject config, SecurityConfig securityConfig) {
        super(vertx);
        this.config = config;
        this.securityConfig = securityConfig;
        listen(TRANSLATIONS, reply(Language::getJsonTranslations));
        gateway(MESSENGER, log());
    }

    /**
     * Enables HTML body handling.
     * Enables cookie handling.
     * Enables user session handling.
     * Sets up Pac4j security engine to authenticate users on /private/* and /eventbus/* addresses.
     * Sets up Pac4j security engine to authorize users against database.
     * Enables Pac4j indirect client callbacks.
     * Enables Pac4j user deauthentication.
     */
    @Override
    public void route(Router router) {
        router.route().handler(BodyHandler.create()
                .setBodyLimit(MAX_BODY_SIZE)
                .setMergeFormAttributes(true));
        router.route().handler(CookieHandler.create());
        router.route().handler(createSessionHandler());
        router.route().handler(UserSessionHandler.create(new AuthProvider(securityConfig.getAuthProvider())));

        SecurityHandler securityHandler = new SecurityHandler(vertx.getDelegate(), securityConfig.getPac4jConfig(),
                securityConfig.getAuthProvider(), new SecurityHandlerOptions()
                .withClients(getClientNames())
                .withAuthorizers(AUTHORIZER + "," + XSS_PROTECTION));
        router.route(AUTH_PRIVATE).getDelegate().handler(securityHandler);
        router.route(EVENTBUS_ALL).getDelegate().handler(securityHandler);

        CallbackHandler callback = new CallbackHandler(vertx.getDelegate(), securityConfig.getPac4jConfig(),
                new CallbackHandlerOptions()
                        .setDefaultUrl(UI_HOME)
                        .setMultiProfile(false));
        router.route(CALLBACK).getDelegate().handler(callback);

        router.route(AUTH_LOGOUT).getDelegate().handler(new ApplicationLogoutHandler(vertx.getDelegate(),
                new ApplicationLogoutHandlerOptions(), securityConfig.getPac4jConfig()));
    }

    /**
     * Creates a session handler which on server environment enables some security measures.
     *
     * @return session handler
     */
    private SessionHandler createSessionHandler() {
        boolean isServer = isServer(config);
        return SessionHandler.create(LocalSessionStore.create(vertx))
                .setCookieSecureFlag(isServer)
                .setCookieHttpOnlyFlag(isServer)
                .setNagHttps(false);
    }
}
