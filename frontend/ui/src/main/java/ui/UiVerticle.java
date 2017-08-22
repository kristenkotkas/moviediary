package ui;

import io.vertx.core.Future;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.ext.web.Router;
import lombok.extern.slf4j.Slf4j;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@Slf4j
public class UiVerticle extends AbstractVerticle {

  // TODO: 22.08.2017 fix webpack related stuff

  @Override
  public void start(Future<Void> fut) throws Exception {
    Router router = Router.router(vertx);
    router.get("/test").handler(ctx -> ctx.response().end("Hello world!"));
    vertx.createHttpServer()
         .requestHandler(router::accept)
         .rxListen(8081)
         .map(server -> (Void) null)
         .subscribe(v -> {
           log.info("HttpServer started. Try -> http://localhost:8081/test");
           fut.complete();
         }, fut::fail);
  }
}
