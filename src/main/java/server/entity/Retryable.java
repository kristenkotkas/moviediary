package server.entity;

import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Objects.requireNonNull;
import static server.util.CommonUtils.check;

/**
 * Retries asynchronous actions as long as there are retries left.
 * Calls an asynchronous failure action when out of retries.
 *
 * @author <a href="https://bitbucket.org/kristjanhk/">Kristjan Hendrik Küngas</a>
 */
public class Retryable {
  private final AtomicInteger counter;
  private final int maxRetries;

  private Retryable(int retries) {
    this.counter = new AtomicInteger(retries);
    this.maxRetries = retries;
  }

  public static Retryable create(int retries) {
    if (retries < 0) {
      throw new IllegalArgumentException("Retries must be >= 0");
    }
    return new Retryable(retries);
  }

  public int left() {
    return counter.get();
  }

  public int decrement() {
    return counter.decrementAndGet();
  }

  public Retryable reset() {
    counter.set(maxRetries);
    return this;
  }

  public Retryable retry(Runnable again, Runnable failure) {
    requireNonNull(again);
    requireNonNull(failure);
    check(decrement() > 0, again, failure);
    return this;
  }
}
