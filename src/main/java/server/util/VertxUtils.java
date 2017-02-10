package server.util;

public class VertxUtils {

    /**
     * Changes vertx logging to SLF4J.
     */
    public static void setLoggingToSLF4J() {
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
    }
}
