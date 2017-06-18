package server;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.Vertx;

import static server.util.CommonUtils.setLoggingToSLF4J;
import static server.util.FileUtils.getConfig;

/**
 * Launches the server.
 * Forces DNS resolver to use Google's DNS servers.
 */
public class Launcher {
  private static final Logger LOG = LoggerFactory.getLogger(Launcher.class);

  public static void main(String[] args) {
    setLoggingToSLF4J();
    Vertx.vertx(new VertxOptions().setAddressResolverOptions(new AddressResolverOptions()
        .addServer("8.8.8.8")
        .addServer("8.8.4.4")))
        .rxDeployVerticle("server.verticle.ServerVerticle", new DeploymentOptions().setConfig(getConfig(args)))
        .subscribe(s -> LOG.info("Server up!"), LOG::error);
  }
}
