package launcher;

import common.entity.Nameable;
import common.entity.Pair;
import common.util.ConfigUtils;
import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.rxjava.config.ConfigRetriever;
import io.vertx.rxjava.core.Vertx;
import rx.Single;
import java.util.List;
import static common.util.ConfigUtils.getDeployerConfig;
import static common.util.FileUtils.getLocalConfig;
import static common.util.LangUtils.capitalize;
import static common.util.rx.ConfigRxRetriever.createConfigRetriever;
import static common.util.rx.RxUtils.singleForEach;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@SuppressWarnings("unused")
public enum Deployer implements Nameable {
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
        //.withHttpBasicAuth(getConfig()) // todo for development
        .withJson(getLocalConfig("/local.json"))
        .rxBuildRetriever()
        .flatMap(ConfigRetriever::rxGetConfig)
        .flatMap(ConfigUtils::validateConfig)
        .flatMap(config -> singleForEach(getVerticleNames(), pair -> vertx
            .rxDeployVerticle(pair.getSnd(), getDeployerConfig(config, pair.getFst()))));
  }

  private static List<Pair<Nameable, String>> getVerticleNames() {
    return stream(values())
        .map(verticle -> Pair.<Nameable, String>builder()
            .fst(verticle)
            .snd(verticle.name().split("_")[1].toLowerCase())
            .build())
        .map(pair -> pair.setSnd(pair.getSnd() + "." + capitalize(pair.getSnd()) + "Verticle"))
        .collect(toList());
  }
}
