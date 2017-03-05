package server.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
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
    private static final Logger log = LoggerFactory.getLogger(ServerVerticle.class);

    private List<Routable> routables;
    // TODO: 02/03/2017 pass in services with constructor for testing

    @Override
    public void start(Future<Void> future) throws Exception {
        Router router = Router.router(vertx); //handles addresses client connects to
        DatabaseService database = DatabaseService.create(vertx, config());
        TmdbService tmdb = TmdbService.create(vertx, config(), database);
        BankLinkService bls = BankLinkService.create(vertx, config());
        MailService mail = MailService.create(vertx, config(), database);
        SecurityConfig securityConfig = new SecurityConfig(config(), database);
        routables = Arrays.asList(
                new AuthRouter(vertx, database, config(), securityConfig), //authentication
                new TmdbRouter(vertx, tmdb), //tmdb rest api
                new BankLinkRouter(vertx, bls), //pangalink
                new EventBusRouter(vertx, database, tmdb), //eventbus
                new DatabaseRouter(vertx, config(), database, mail, securityConfig), //database rest api
                new MailRouter(vertx, mail), //mail
                new UiRouter(vertx, securityConfig, database)); //ui
        routables.forEach(routable -> routable.route(router));
        vertx.createHttpServer()
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