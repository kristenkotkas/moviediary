package server.util;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;

public class NetworkUtils {
    public static final String HTTP_HOST = "http_host";
    public static final String HTTP_PORT = "http_port";
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 8081;

    /**
     * Completes or fails future based on result.
     *
     * @param future to use
     * @return this handler
     */
    public static Handler<AsyncResult<HttpServer>> futureHandler(Future<Void> future) {
        return ar -> {
            if (ar.succeeded()) {
                future.complete();
            } else {
                future.fail(ar.cause());
            }
        };
    }
}