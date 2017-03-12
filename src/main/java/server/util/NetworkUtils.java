package server.util;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class NetworkUtils {
    private static final Logger LOG = LoggerFactory.getLogger(NetworkUtils.class);

    public static final String HTTP_HOST = "http_host";
    public static final String HTTP_PORT = "http_port";
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 8081;
    public static final String SSL_HOST = "production_host";
    public static final int MAX_BODY_SIZE = 25 * 1024 * 1024;
    private static int IS_SERVER = -1;

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
                LOG.error("Failed to check for loopback address.", e);
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
        if (IS_SERVER == -1) {
            IS_SERVER = isLoopbackInterface(config.getString(SSL_HOST)) ? 1 : 0;
        }
        return IS_SERVER == 1;
    }
}