package server;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import server.verticle.ServerVerticle;

import java.io.IOException;

import static server.util.FileUtils.getConfig;
import static server.util.VertxUtils.setLoggingToSLF4J;

/**
 * Launches the server.
 */
public class Launcher {
    private static final Logger log = LoggerFactory.getLogger(Launcher.class);

    public static void main(String[] args) throws IOException {
        setLoggingToSLF4J();
        Vertx.vertx(new VertxOptions()
                .setAddressResolverOptions(new AddressResolverOptions()
                        .addServer("8.8.8.8")
                        .addServer("8.8.4.4")))
                .deployVerticle(new ServerVerticle(), new DeploymentOptions().setConfig(getConfig(args)), ar -> {
            if (ar.succeeded()) {
                log.info("Server up!");
            } else {
                log.error(ar.cause());
            }
        });
    }
}
