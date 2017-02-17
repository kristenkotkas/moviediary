package server.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import server.entity.Status;
import server.router.DatabaseRouter;
import server.router.Routable;
import server.router.TmdbRouter;
import server.router.UiRouter;
import server.service.DatabaseService;
import server.service.TmdbService;

import java.util.Arrays;
import java.util.List;

import static server.util.NetworkUtils.*;

/**
 * Main server logic.
 */
public class ServerVerticle extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(ServerVerticle.class);

    private TmdbService tmdb;
    private DatabaseService database;
    private List<Routable> routables;

    @Override
    public void start(Future<Void> future) throws Exception {
        Router router = Router.router(vertx); //handles addresses client connects to
        tmdb = TmdbService.create(vertx, config()); //tmdb api service
        database = DatabaseService.create(vertx, config()); //
        routables = Arrays.asList(
                new TmdbRouter(vertx, tmdb), //tmdb rest api
                new DatabaseRouter(vertx, database), //database rest api
                new UiRouter(vertx, config())); //ui
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