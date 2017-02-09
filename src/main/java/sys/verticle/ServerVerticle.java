package sys.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;
import sys.router.Routable;
import sys.router.UiRouter;

/**
 * Main server logic.
 */
public class ServerVerticle extends AbstractVerticle {

    @Override
    public void start() throws Exception {
        Router router = Router.router(vertx); //handles addresses client connects to
        Routable ui = new UiRouter(vertx); //handles ui rendering
        ui.route(router);

        //if no handler found for address -> 404
        router.route().last().handler(ctx -> ctx.response().setStatusCode(404).end());

        //starts server at localhost:8080
        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(8080, "localhost");
    }
}