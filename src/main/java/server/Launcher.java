package server;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.Vertx;

import static util.FileUtils.getConfig;

/**
 * Launches the server.
 * Forces DNS resolver to use Google's DNS servers.
 */
public class Launcher {
  private static final Logger LOG = LoggerFactory.getLogger(Launcher.class);

  public static void main(String[] args) {
    System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
    Vertx vertx = Vertx.vertx(new VertxOptions()
        .setAddressResolverOptions(new AddressResolverOptions()
            .addServer("8.8.8.8")
            .addServer("8.8.4.4")));
    DeploymentOptions dOptions = new DeploymentOptions().setConfig(getConfig(args));
    vertx.rxDeployVerticle("database.DatabaseVerticle", dOptions)
        .flatMap(s -> vertx.rxDeployVerticle("tmdb.TmdbVerticle", dOptions))
        .flatMap(s -> vertx.rxDeployVerticle("omdb.OmdbVerticle", dOptions))
        .flatMap(s -> vertx.rxDeployVerticle("mail.MailVerticle", dOptions))
        .flatMap(s -> vertx.rxDeployVerticle("server.verticle.ServerVerticle", dOptions))
        .subscribe(s -> LOG.info("Server up!"), LOG::error);
  }
}
