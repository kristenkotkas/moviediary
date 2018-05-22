package server.util;

import io.vertx.core.Future;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
public class FutureUtils {

  @SuppressWarnings("unchecked")
  public static <T> io.vertx.rxjava.core.Future<T> toRx(Future<T> future) {
    return io.vertx.rxjava.core.Future.future(fut -> future.setHandler(fut.getDelegate()));
  }

  @SuppressWarnings("unchecked")
  public static <T> Future<T> fromRx(io.vertx.rxjava.core.Future<T> future) {
    return future.getDelegate();
  }
}
