package server;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;
import lombok.extern.slf4j.Slf4j;
import server.verticle.ServerVerticle;

import static io.vertx.rxjava.core.RxHelper.deployVerticle;
import static io.vertx.rxjava.core.Vertx.vertx;
import static server.util.CommonUtils.setLoggingToSLF4J;
import static server.util.FileUtils.getConfig;

/**
 * Launches the server.
 * Forces DNS resolver to use Google's DNS servers.
 */
@Slf4j
public class Launcher {

  public static void main(String[] args) {
    setLoggingToSLF4J();
    deployVerticle(vertx(new VertxOptions().setAddressResolverOptions(new AddressResolverOptions()
        .addServer("8.8.8.8")
        .addServer("8.8.4.4"))), new ServerVerticle(), new DeploymentOptions()
        .setConfig(getConfig(args)))
        .subscribe(ar -> log.info("Server up!"), thr -> log.error("Failed to start server", thr));
  }
}
