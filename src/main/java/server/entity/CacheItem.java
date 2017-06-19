package server.entity;

import io.vertx.rxjava.core.Future;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static io.vertx.rxjava.core.Future.future;
import static java.lang.System.currentTimeMillis;
import static server.util.CommonUtils.check;

/**
 * Cached value wrapper.
 * Default timeout is 1 hour.
 **/
public class CacheItem<T> {
  private long timeout = TimeUnit.HOURS.toMillis(1);
  private long timestamp = 0;
  private T value;

  public T get() {
    return value;
  }

  public T set(T value) {
    this.value = value;
    timestamp = currentTimeMillis();
    return value;
  }

  public CacheItem<T> setTimeout(TimeUnit unit, long length) {
    timeout = unit.toMillis(length);
    return this;
  }

  public CacheItem<T> invalidate() {
    timestamp = 0;
    return this;
  }

  public boolean isUpToDate() {
    return currentTimeMillis() - timestamp <= timeout;
  }

  public Future<T> get(boolean use, BiConsumer<Future<T>, CacheItem<T>> updater) {
    return future(fut -> check(use && isUpToDate(),
        () -> fut.complete(value),
        () -> updater.accept(fut, this)));
  }
}
