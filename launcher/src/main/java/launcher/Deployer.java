package launcher;

import common.util.FileUtils;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.rxjava.config.ConfigRetriever;
import io.vertx.rxjava.core.Vertx;
import rx.Single;
import java.util.List;
import static common.util.LangUtils.capitalize;
import static common.util.rx.ConfigRxRetriever.createConfigRetriever;
import static common.util.rx.RxUtils.singleForEach;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@SuppressWarnings("unused")
public enum Deployer {
  BACKEND_AUTH,
  BACKEND_DATABASE,
  FRONTEND_UI,
  FRONTEND_GATEWAY;

  public static Single<String> deployVerticles() {
    Vertx vertx = Vertx.vertx(new VertxOptions()
        .setAddressResolverOptions(new AddressResolverOptions()
            .addServer("8.8.8.8")
            .addServer("8.8.4.4")));

    return createConfigRetriever(vertx)
        .withHttpBasicAuth(FileUtils.getConfig())
        .rxBuildRetriever()
        .flatMap(ConfigRetriever::rxGetConfig)
        .flatMap(config -> singleForEach(getVerticleNames(), name -> vertx
            .rxDeployVerticle(name, new DeploymentOptions().setConfig(config))));
  }

  private static List<String> getVerticleNames() {
    return stream(values())
        .map(verticle -> verticle.name().split("_")[1].toLowerCase())
        .map(name -> name + "." + capitalize(name) + "Verticle")
        .collect(toList());
  }
}
