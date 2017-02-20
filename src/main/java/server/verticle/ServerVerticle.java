package server.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import server.entity.Status;
import server.router.*;
import server.security.SecurityConfig;
import server.service.BankLinkService;
import server.service.DatabaseService;
import server.service.TmdbService;

import java.util.Arrays;
import java.util.List;

import static server.util.HandlerUtils.futureHandler;
import static server.util.NetworkUtils.*;

/**
 * Main server logic.
 */
public class ServerVerticle extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(ServerVerticle.class);

    private List<Routable> routables;

    @Override
    public void start(Future<Void> future) throws Exception {
        Router router = Router.router(vertx); //handles addresses client connects to
        TmdbService tmdb = TmdbService.create(vertx, config());
        BankLinkService bls = BankLinkService.create(vertx, config());
        DatabaseService database = DatabaseService.create(vertx, config());
        SecurityConfig securityConfig = new SecurityConfig(config(), database);
        routables = Arrays.asList(
                new AuthRouter(vertx, config(), securityConfig), //authentication
                new TmdbRouter(vertx, tmdb), //tmdb rest api
                new BankLinkRouter(vertx, bls),
                new DatabaseRouter(vertx, database, securityConfig), //database rest api
                new UiRouter(vertx, securityConfig, database)); //ui
        routables.forEach(routable -> routable.route(router));
        router.route().last().handler(Status::notFound); //if no handler found for address -> 404

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