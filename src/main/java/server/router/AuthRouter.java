package server.router;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.UserSessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import org.pac4j.vertx.handler.impl.*;
import server.security.SecurityConfig;
import server.service.DatabaseService;

import static server.router.EventBusRouter.EVENTBUS_ALL;
import static server.router.UiRouter.UI_HOME;
import static server.security.SecurityConfig.AUTHORIZER;
import static server.security.SecurityConfig.AuthClient.getClientNames;
import static server.service.DatabaseService.*;
import static server.util.CommonUtils.getProfile;
import static server.util.NetworkUtils.MAX_BODY_SIZE;
import static server.util.NetworkUtils.isServer;

public class AuthRouter extends Routable {
    public static final String AUTH_API = "(?!\\/api\\/users\\/insert)(\\/api\\/.*)";
    public static final String AUTH_PRIVATE = "/private/*";
    public static final String AUTH_LOGOUT = "/logout";
    private static final String CALLBACK = "/callback";
    public static final String LANGUAGE = "lang";

    private final DatabaseService database;
    private final JsonObject config;
    private final SecurityConfig securityConfig;

    public AuthRouter(Vertx vertx, DatabaseService database, JsonObject config, SecurityConfig securityConfig) {
        super(vertx);
        this.database = database;
        this.config = config;
        this.securityConfig = securityConfig;
    }

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

        router.get(AUTH_LOGOUT).handler(new ApplicationLogoutHandler(vertx,
                new ApplicationLogoutHandlerOptions(), securityConfig.getPac4jConfig()));

        router.get(AUTH_PRIVATE).handler(this::handleLanguage);
    }

    private void handleLanguage(RoutingContext ctx) {
        if (!ctx.session().data().containsKey(LANGUAGE)) {
            database.getSettings(getProfile(ctx).getEmail()).setHandler(
                    ar -> ctx.session().data().put(LANGUAGE, ar.succeeded() && getNumRows(ar.result()) > 0 ?
                            getRows(ar.result()).getJsonObject(0).getString(DB_LANGUAGE) :
                            ctx.preferredLocale().language()));
        }
        ctx.next();
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
