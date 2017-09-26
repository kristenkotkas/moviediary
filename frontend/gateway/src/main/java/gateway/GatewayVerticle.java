package gateway;

import common.util.Status;
import common.verticle.rx.RestApiRxVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.servicediscovery.ServiceReference;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.types.EventBusService;
import io.vertx.servicediscovery.types.HttpEndpoint;
import rx.Single;
import java.util.List;
import java.util.Optional;
import static common.util.ConditionUtils.chain;
import static common.util.ConditionUtils.check;
import static common.util.Status.*;
import static common.util.rx.RxUtils.toSubscriber;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
public class GatewayVerticle extends RestApiRxVerticle {

  @Override
  public void start(Future<Void> future) throws Exception {
    super.start();
    // TODO: 21.08.2017 use circuit breaker etc..
    String host = config().getString("http.host", "localhost");
    int port = config().getInteger("http.port", 8080);

    Router router = Router.router(vertx);
    enableLocalSession(router);
    router.route().handler(BodyHandler.create());
    router.get("/api/v").handler(this::apiVersion);

    // TODO: 23.08.2017 pac4j auth handlers

    // TODO: 23.08.2017 login, logout, auth callback, eventbus, etc..

    router.route("/api/*").handler(this::dispatchRequests);
    router.route("/*").handler(Status::notFound);

    // TODO: 23.08.2017 https? -> only if used without nginx

    createHttpServer(router, host, port)
        .flatMap(v -> publishApiGateway(host, port))
        .subscribe(toSubscriber(future));
  }

  private void dispatchRequests(RoutingContext ctx) {
    int offset = "/api/".length();
    getAllHttpEndpoints()
        .doOnError(err -> badGateway(ctx, err))
        .subscribe(list -> {
          String path = ctx.request().uri();
          if (path.length() < offset) {
            notFound(ctx);
            return;
          }
          String prefix = path.substring(offset).split("/")[0];
          String apiPath = path.substring(offset + prefix.length());
          Optional<Record> client = list.stream()
                                        .filter(r -> r.getMetadata().containsKey("api.name"))
                                        .filter(r -> r.getMetadata().getString("api.name").equals(prefix))
                                        .findAny();
          if (!client.isPresent()) {
            notFound(ctx);
            return;
          }
          doDispatch(ctx, apiPath, discovery.getReference(client.get()));
        });
  }

  private void doDispatch(RoutingContext ctx, String path, ServiceReference ref) {
    HttpClientRequest req = ref.getAs(HttpClient.class).request(ctx.request().method(), path,
        res -> res.bodyHandler(body -> check(res.statusCode() >= 500,
            () -> serviceUnavailable(ctx, res.statusMessage()),
            () -> chain(ctx.response(), r -> res.headers().addAll(r.headers()))
                .setStatusCode(res.statusCode())
                .end(body),
            () -> discovery.release(ref))));
    req.headers().addAll(ctx.request().headers());
    // TODO: 24.08.2017 pass user auth data to service?, (in header)
    if (ctx.getBody() != null) {
      req.end(ctx.getBody());
      return;
    }
    req.end();
  }

  private void apiVersion(RoutingContext ctx) {
    ctx.response().end(new JsonObject().put("version", "v2").encodePrettily());
  }

  private Single<List<Record>> getAllHttpEndpoints() {
    return discovery.rxGetRecords(record -> record.getType().equals(HttpEndpoint.TYPE));
  }

  private Single<List<Record>> getAllEventbusServices() {
    return discovery.rxGetRecords(record -> record.getType().equals(EventBusService.TYPE));
  }
}
