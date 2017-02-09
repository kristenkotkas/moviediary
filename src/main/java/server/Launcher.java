package server;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import server.verticle.ServerVerticle;

import java.io.IOException;

import static server.util.FileUtils.getConfig;
import static server.util.FileUtils.parseArguments;

/**
 * Launches the server.
 */
public class Launcher {
    private static final Logger log = LoggerFactory.getLogger(Launcher.class);

    public static void main(String[] args) throws IOException {
        //set vertx logging to slf4j
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
        JsonObject config = getConfig().mergeIn(parseArguments(args));
        Vertx.vertx().deployVerticle(new ServerVerticle(), new DeploymentOptions().setConfig(config), ar -> {
            if (ar.succeeded()) {
                System.out.println("Server up!");
            } else {
                System.out.println("Fail: " + ar.cause());
            }
        });
    }
}
