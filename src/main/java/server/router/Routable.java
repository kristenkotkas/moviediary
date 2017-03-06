package server.router;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

/**
 * Generic class that contains routes.
 */
public abstract class Routable implements AutoCloseable {
    protected final Vertx vertx;

    public Routable(Vertx vertx) {
        this.vertx = vertx;
    }

    public abstract void route(Router router);

    @Override
    public void close() throws Exception {
    }
}
