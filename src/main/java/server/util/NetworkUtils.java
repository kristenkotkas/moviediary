package server.util;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class NetworkUtils {
    private static final Logger log = LoggerFactory.getLogger(NetworkUtils.class);

    public static final String HTTP_HOST = "http_host";
    public static final String HTTP_PORT = "http_port";
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 8081;

    public static final String SSL_HOST = "production_host";

    private static final int KB = 1024;
    private static final int MB = 1024 * KB;
    public static final int MAX_BODY_SIZE = 25 * MB;

    private static int s_isServer = -1;

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

    /**
     * Checks if given host is resolved to localhost.
     *
     * @param host to resolve
     * @return boolean
     */
    private static boolean isLoopbackInterface(String host) {
        if (host != null && !host.isEmpty()) {
            try {
                return InetAddress.getByName(host).isLoopbackAddress();
            } catch (UnknownHostException e) {
                log.error("Failed to check for loopback address.", e);
            }
        }
        return false;
    }

    /**
     * Checks if is currently running on server specified in configuration.
     *
     * @param config to use
     * @return boolean
     */
    public static boolean isServer(JsonObject config) {
        if (s_isServer == -1) {
            s_isServer = isLoopbackInterface(config.getString(SSL_HOST)) ? 1 : 0;
        }
        return s_isServer == 1;
    }
}