package launcher;

import common.entity.Nameable;
import common.entity.tuple.Pair;
import common.util.ConfigUtils;
import common.util.chain.SChain;
import common.util.rx.ConfigRxRetriever;
import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.rxjava.config.ConfigRetriever;
import io.vertx.rxjava.core.Vertx;
import rx.Single;

import java.util.List;

import static common.util.ConfigUtils.getDeployerConfig;
import static common.util.FileUtils.getLocalConfig;
import static common.util.LangUtils.capitalize;
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
    return SChain.of(Vertx.vertx(new VertxOptions()
        .setAddressResolverOptions(new AddressResolverOptions()
            .addServer("8.8.8.8")
            .addServer("8.8.4.4"))))
        .mapPChain(ConfigRxRetriever::createConfigRetriever)
        //.mapSnd(config -> config.withHttpBasicAuth(getConfig())) // todo for development
        .mapSnd(config -> config.withJson(getLocalConfig("/local.json")))
        .mapSnd(ConfigRxRetriever::rxBuildRetriever)
        .mapSnd(single -> single.flatMap(ConfigRetriever::rxGetConfig))
        .mapSnd(single -> single.flatMap(ConfigUtils::validateConfig))
        .mapSnd((vertx, single) -> single.flatMap(config -> singleForEach(getVerticleNames(),
            pair -> vertx.rxDeployVerticle(pair.getSnd(), getDeployerConfig(config, pair.getFst())))))
        .getSecond();
  }

  private static List<Pair<Nameable, String>> getVerticleNames() {
    return stream(values())
        .map(verticle -> Pair.<Nameable, String>of(verticle, verticle.name().split("_")[1].toLowerCase()))
        .map(pair -> pair.mapSnd(pair.getSnd() + "." + capitalize(pair.getSnd()) + "Verticle"))
        .collect(toList());
  }
}
