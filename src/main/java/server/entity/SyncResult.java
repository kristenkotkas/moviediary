package server.entity;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import server.util.CommonUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Wrapper for getting an asynchronous result synchronously.
 * Synchronous method will block at await() until asynchronous method calls ready().
 *
 * @author <a href="https://bitbucket.org/kristjanhk/">Kristjan Hendrik Küngas</a>
 */
public class SyncResult<T> {
  private static final Logger LOG = LoggerFactory.getLogger(SyncResult.class);
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private CountDownLatch latch = new CountDownLatch(1);

  private AtomicReference<T> value = new AtomicReference<>();
  private AtomicReference<Throwable> error = new AtomicReference<>();

  public T get() {
    if (error.get() != null) {
      throw new RuntimeException(error.get());
    }
    return value.get();
  }

  public T get(T def) {
    return value != null ? get() : def;
  }

  public SyncResult<T> set(T obj) {
    value.set(obj);
    return this;
  }

  public SyncResult<T> setReady(T obj) {
    value.set(obj);
    ready();
    return this;
  }

  public boolean isPresent() {
    return value != null;
  }

  public SyncResult<T> ifPresent(Consumer<? super T> consumer) {
    CommonUtils.ifPresent(value.get(), consumer::accept);
    return this;
  }

  public SyncResult<T> peek(Consumer<? super T> consumer) {
    consumer.accept(value.get());
    return this;
  }

  public SyncResult<T> executeAsync(Runnable runnable) {
    executor.execute(runnable);
    return this;
  }

  public SyncResult<T> await() {
    try {
      latch.await();
    } catch (InterruptedException e) {
      LOG.error("SyncResult was interrupted.", e);
    }
    return this;
  }

  public SyncResult<T> await(long timeout, TimeUnit unit) {
    try {
      latch.await(timeout, unit);
    } catch (InterruptedException e) {
      LOG.error("SyncResult was interrupted.", e);
    }
    return this;
  }

  public void ready() {
    latch.countDown();
  }

  public void reset() {
    value.set(null);
    error.set(null);
    latch = new CountDownLatch(1);
  }

  public SyncResult<T> fail(Throwable err) {
    error.set(err);
    ready();
    return this;
  }
}
