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
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.types.HttpEndpoint;
import rx.Single;
import java.util.List;
import java.util.Optional;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
public class GatewayVerticle extends RestApiRxVerticle {

  @Override
  public void start(Future<Void> future) throws Exception {
    super.start();
    // TODO: 21.08.2017 use circuit breaker etc..

    String host = "localhost"; // TODO: 23.08.2017 from config
    int port = 8080;

    Router router = Router.router(vertx);
    enableLocalSession(router);
    router.route().handler(BodyHandler.create());
    router.get("/api/v").handler(this::apiVersion);

    // TODO: 23.08.2017 pac4j auth handlers

    // TODO: 23.08.2017 login, logout, auth callback, eventbus, etc..

    router.route("/api/*").handler(this::dispatchRequests);
    router.route("/*").handler(this::handleUi);

    // TODO: 23.08.2017 https? -> only if used without nginx


    // TODO: 23.08.2017 pass logger to publishLogEvent or smth -> shows correct class in log
    createHttpServer(router, host, port)
        .flatMap(v -> publishApiGateway(host, port))
        .flatMap(v -> publishLogEvent("gateway", new JsonObject()
            .put("info", "API Gateway is running on port " + port)))
        .subscribe(toSubscriber(future));
  }

  private void handleUi(RoutingContext ctx) {
    // TODO: 23.08.2017 router to ui module -> serve static content for react
  }

  private void dispatchRequests(RoutingContext ctx) {
    // TODO: 23.08.2017 route requests to other modules
    int offset = "/api/".length();

    getAllHttpEndpoints()
        .doOnError(err -> Status.badGateway(ctx, err))
        .doOnSuccess(list -> {
          String path = ctx.request().uri();
          if (path.length() < offset) {
            Status.notFound(ctx);
            return;
          }
          String prefix = path.substring(offset).split("/")[0];
          String apiPath = path.substring(offset + prefix.length());
          Optional<Record> client = list.stream()
                                        .filter(r -> r.getMetadata().containsKey("api.name"))
                                        .filter(r -> r.getMetadata().getString("api.name").equals(prefix))
                                        .findAny();
          if (!client.isPresent()) {
            Status.notFound(ctx);
            return;
          }
          doDispatch(ctx, apiPath, discovery.getReference(client.get()).get());
        });
  }

  private void doDispatch(RoutingContext ctx, String path, HttpClient client) {
    HttpClientRequest serviceRequest = client.request(ctx.request().method(), path, res -> res.bodyHandler(body -> {
      if (res.statusCode() >= 500) {
        Status.serviceUnavailable(ctx, res.statusMessage());
        return;
      }
      ctx.response().headers().addAll(res.headers());
      ctx.response().setStatusCode(res.statusCode()).end(body);
    }));
    serviceRequest.headers().addAll(ctx.request().headers());
    // TODO: 24.08.2017 pass user auth data to service?, (in header)
    if (ctx.getBody() != null) {
      serviceRequest.end(ctx.getBody());
      return;
    }
    serviceRequest.end();
  }

  private void apiVersion(RoutingContext ctx) {
    ctx.response()
       .end(new JsonObject().put("version", "v2").encodePrettily());
  }

  private Single<List<Record>> getAllHttpEndpoints() {
    return discovery.rxGetRecords(record -> record.getType().equals(HttpEndpoint.TYPE));
  }
}
