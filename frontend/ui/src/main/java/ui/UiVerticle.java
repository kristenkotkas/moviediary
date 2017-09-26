package ui;

import common.verticle.rx.RestApiRxVerticle;
import io.vertx.core.Future;
import io.vertx.rxjava.ext.web.handler.StaticHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@Slf4j
public class UiVerticle extends RestApiRxVerticle {

  // TODO: 22.08.2017 fix webpack related stuff

  @Override
  public void start(Future<Void> fut) throws Exception {
    super.start();
    startRestRouter(fut,
        r -> r.route("/*", StaticHandler.create("../")
                                        .setAllowRootFileSystemAccess(true) // TODO: 26.09.2017 remove
                                        .setDirectoryListing(true)
                                        .setCachingEnabled(false)
                                        .setIncludeHidden(true)));
  }
}
