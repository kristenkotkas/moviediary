package sys;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import sys.verticle.ServerVerticle;

import java.io.IOException;

import static sys.util.FileUtils.getConfig;
import static sys.util.FileUtils.parseArguments;


/**
 * Launches the server.
 */
public class Launcher {

    public static void main(String[] args) throws IOException {
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory"); //set vertx logging to slf4j
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
