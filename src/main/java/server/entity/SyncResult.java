package server.entity;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import server.util.CommonUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Wrapper for getting an asynchronous result synchronously.
 * Synchronous method will block at await() until asynchronous method calls ready().
 *
 * @author <a href="https://bitbucket.org/kristjanhk/">Kristjan Hendrik Küngas</a>
 */
public class SyncResult<T> {
    private static final Logger LOG = LoggerFactory.getLogger(SyncResult.class);
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final CountDownLatch latch = new CountDownLatch(1);

    private T value;
    private Throwable error;

    public T get() {
        if (error != null) {
            throw new RuntimeException(error);
        }
        return value;
    }

    public T get(T def) {
        return value != null ? get() : def;
    }

    public SyncResult<T> set(T obj) {
        this.value = obj;
        return this;
    }

    public SyncResult<T> setReady(T obj) {
        this.value = obj;
        ready();
        return this;
    }

    public boolean isPresent() {
        return value != null;
    }

    public SyncResult<T> ifPresent(Consumer<? super T> consumer) {
        CommonUtils.ifPresent(value, consumer::accept);
        return this;
    }

    public SyncResult<T> peek(Consumer<? super T> consumer) {
        consumer.accept(value);
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

    public SyncResult<T> fail(Throwable err) {
        error = err;
        ready();
        return this;
    }
}
