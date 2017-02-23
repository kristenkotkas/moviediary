package server.router;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.UserSessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import org.pac4j.vertx.handler.impl.*;
import server.security.SecurityConfig;

import static server.router.EventBusRouter.EVENTBUS_ALL;
import static server.router.UiRouter.UI_HOME;
import static server.router.UiRouter.UI_LOGIN;
import static server.security.SecurityConfig.AUTHORIZER;
import static server.security.SecurityConfig.AuthClient.getClientNames;
import static server.util.NetworkUtils.MAX_BODY_SIZE;
import static server.util.NetworkUtils.isServer;

public class AuthRouter extends Routable {
    public static final String AUTH_API = "(?!\\/api\\/users\\/insert)(\\/api\\/.*)";
    public static final String AUTH_PRIVATE = "/private/*";
    public static final String AUTH_LOGOUT = "/logout";
    private static final String CALLBACK = "/callback";

    private final JsonObject config;
    private final SecurityConfig securityConfig;

    public AuthRouter(Vertx vertx, JsonObject config, SecurityConfig securityConfig) {
        super(vertx);
        this.config = config;
        this.securityConfig = securityConfig;
    }

    // TODO: 19.02.2017 if fb/google auth is canceled -> redirect to login with error message?
    // TODO: 19.02.2017 if any actions fail -> custom page or redirect to login?

    @Override
    public void route(Router router) {
        router.route().handler(BodyHandler.create()
                .setBodyLimit(MAX_BODY_SIZE)
                .setMergeFormAttributes(true));
        router.route().handler(CookieHandler.create());
        router.route().handler(createSessionHandler());
        router.route().handler(UserSessionHandler.create(securityConfig.getAuthProvider()));

        SecurityHandler securityHandler = new SecurityHandler(vertx, securityConfig.getPac4jConfig(),
                securityConfig.getAuthProvider(), new SecurityHandlerOptions()
                .withClients(getClientNames())
                .withAuthorizers(AUTHORIZER));
        router.route(AUTH_PRIVATE).handler(securityHandler);
        router.routeWithRegex(AUTH_API).handler(securityHandler);
        router.route(EVENTBUS_ALL).handler(securityHandler);

        CallbackHandler callback = new CallbackHandler(vertx, securityConfig.getPac4jConfig(),
                new CallbackHandlerOptions()
                        .setDefaultUrl(UI_HOME)
                        .setMultiProfile(false));
        router.route(CALLBACK).handler(callback);

        router.get(AUTH_LOGOUT).handler(new ApplicationLogoutHandler(vertx, new ApplicationLogoutHandlerOptions()
                .setDefaultUrl(UI_LOGIN), securityConfig.getPac4jConfig()));
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
