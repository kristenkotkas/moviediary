package server.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import server.entity.Status;
import server.router.Routable;
import server.router.TmdbRouter;
import server.router.UiRouter;
import server.service.TmdbService;

import java.util.Arrays;
import java.util.List;

/**
 * Main server logic.
 */
public class ServerVerticle extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(ServerVerticle.class);

    private TmdbService tmdb;
    private List<Routable> routables;

    @Override
    public void start() throws Exception {
        Router router = Router.router(vertx); //handles addresses client connects to
        tmdb = TmdbService.create(vertx, config()); //tmdb api service
        routables = Arrays.asList(new TmdbRouter(vertx, tmdb), new UiRouter(vertx)); //tmdb rest api + ui
        routables.forEach(routable -> routable.route(router));
        router.route().last().handler(Status::notFound); //if no handler found for address -> 404
        //starts server at localhost:8080
        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(8080, "localhost");
    }

    @Override
    public void stop() throws Exception {
        for (Routable routable : routables) {
            routable.close();
        }
    }
}