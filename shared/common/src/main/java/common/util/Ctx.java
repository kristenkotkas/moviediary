package common.util;

import io.vertx.core.*;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@Slf4j
public class Ctx {

  public static Context ctx() {
    Context context = Vertx.currentContext();
    if (context == null) {
      throw new IllegalStateException("Vertx context is called from non-vertx thread!");
    }
    return context;
  }

  public static Vertx vertx() {
    return ctx().owner();
  }

  public static Vertx async(Runnable task) {
    Context ctx = ctx();
    ctx.runOnContext(v -> task.run());
    return ctx.owner();
  }

  public static <T> Vertx asyncBlocking(Handler<Future<T>> blockingHandler, Handler<AsyncResult<T>> resultHandler) {
    Context ctx = ctx();
    ctx.executeBlocking(blockingHandler, resultHandler);
    return ctx.owner();
  }

  public static <T> Handler<AsyncResult<T>> wrap(Consumer<T> consumer) {
    return ar -> {
      if (ar.failed()) {
        log.error("Async action failed.", ar.cause());
        return;
      }
      consumer.accept(ar.result());
    };
  }

  public static void main(String[] args) { // todo rm
    Ctx.<Integer>asyncBlocking(fut -> fut.complete(3), wrap(x -> log.info("Result: " + x)));
    Ctx.<Integer>asyncBlocking(fut -> fut.fail("not 3"), wrap(x -> log.info("Result: " + x)));
  }
}
