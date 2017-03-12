package server.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import server.router.*;
import server.security.SecurityConfig;
import server.service.BankLinkService;
import server.service.DatabaseService;
import server.service.MailService;
import server.service.TmdbService;

import java.util.Arrays;
import java.util.List;

import static server.util.HandlerUtils.futureHandler;
import static server.util.NetworkUtils.*;

/**
 * Main server logic.
 * Creates services, security configuration and routes.
 * Creates a HTTP server.
 */
public class ServerVerticle extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(ServerVerticle.class);

    private List<Routable> routables;
    // TODO: 02/03/2017 pass in services with constructor for testing

    /**
     * Creates service for interacting with database.
     * Creates service for interacting with TheMovieDatabase API.
     * Creates service for interacting with bankLink application.
     * Creates service for interacting with mail server.
     * Creates Pac4j security engine configuration.
     * <p>
     * Creates authentication for routes.
     * Creates external API for TheMovieDatabase services.
     * Creates external API for bankLink services.
     * Creates Eventbus (aka Websocket for browsers) addresses for various services.
     * Creates external API for database services.
     * Creates external API for mail services.
     * Creates routes for UI rendering.
     * <p>
     * Starts the HTTP server.
     */
    @Override
    public void start(Future<Void> future) throws Exception {
        Router router = Router.router(vertx);
        DatabaseService database = DatabaseService.create(vertx, config());
        TmdbService tmdb = TmdbService.create(vertx, config(), database);
        BankLinkService bankLink = BankLinkService.create(vertx, config());
        MailService mail = MailService.create(vertx, database);
        SecurityConfig securityConfig = new SecurityConfig(config(), database);
        routables = Arrays.asList(
                new AuthRouter(vertx, config(), securityConfig),
                new TmdbRouter(vertx, tmdb),
                new BankLinkRouter(vertx, bankLink),
                new EventBusRouter(vertx, database, tmdb),
                new DatabaseRouter(vertx, config(), database, mail),
                new MailRouter(vertx, mail),
                new UiRouter(vertx, securityConfig));
        routables.forEach(routable -> routable.route(router));
        vertx.createHttpServer(new HttpServerOptions().setCompressionSupported(true))
                .requestHandler(router::accept)
                .listen(config().getInteger(HTTP_PORT, DEFAULT_PORT),
                        config().getString(HTTP_HOST, DEFAULT_HOST), futureHandler(future));
    }

    @Override
    public void stop() throws Exception {
        for (Routable routable : routables) {
            routable.close();
        }
    }
}