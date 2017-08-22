package launcher;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.rxjava.core.Vertx;
import lombok.extern.slf4j.Slf4j;
import rx.Single;
import java.util.List;
import static common.util.FileUtils.getConfig;
import static common.util.LangUtils.capitalize;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@Slf4j
public enum Verticle {
  BACKEND_AUTH,
  BACKEND_DATABASE,
  FRONTEND_UI,
  FRONTEND_GATEWAY;

  public static void deployVerticles(String... args) {
    Vertx vertx = Vertx.vertx(new VertxOptions()
        .setAddressResolverOptions(new AddressResolverOptions()
            .addServer("8.8.8.8")
            .addServer("8.8.4.4")));
    DeploymentOptions dOptions = new DeploymentOptions().setConfig(getConfig(args));
    List<String> names = getVerticleNames();
    if (!names.isEmpty()) {
      Single<String> single = vertx.rxDeployVerticle(names.get(0), dOptions);
      for (String name : names.subList(1, names.size())) {
        single = single.flatMap(s -> vertx.rxDeployVerticle(name, dOptions));
      }
      single.doOnError(err -> log.error("Verticle deployment failed: " + err))
            .subscribe(s -> log.info("Verticle deployment succeeded."));
    }
  }

  private static List<String> getVerticleNames() {
    return stream(values())
        .map(verticle -> verticle.name().split("_")[1].toLowerCase())
        .map(name -> name + "." + capitalize(name) + "Verticle")
        .collect(toList());
  }
}
