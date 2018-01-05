package server.router;

import io.vertx.rxjava.ext.web.Router;

public interface Routable {

  void route(Router router);
}
