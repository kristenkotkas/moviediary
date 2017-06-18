package server.service;

import io.vertx.core.Handler;
import io.vertx.ext.web.impl.ConcurrentLRUCache;
import io.vertx.rxjava.core.Future;
import rx.Single;
import server.entity.CacheItem;

/**
 * Caching implementation for services.
 **/
public abstract class CachingService<T> {
  protected static final int DEFAULT_MAX_CACHE_SIZE = 10000;
  private final ConcurrentLRUCache<String, CacheItem<T>> cache;

  protected CachingService() {
    this(CachingService.DEFAULT_MAX_CACHE_SIZE);
  }

  protected CachingService(int maxCacheSize) {
    if (maxCacheSize < 1) {
      throw new IllegalArgumentException("maxCacheSize must be >= 1");
    }
    cache = new ConcurrentLRUCache<>(maxCacheSize);
  }

  /**
   * Gets cached item or creates a new one if it does not exist.
   *
   * @param key to get cache value from
   */
  void getCached(String key, Handler<CacheItem<T>> handler) { // TODO: 18/06/2017 migrate to handler
    handler.handle(getCached(key));
  }

  public Single<CacheItem<T>> rxGetCached(String key) {
    return Single.create(sub -> getCached(key, sub::onSuccess));
  }

  public CacheItem<T> getCached(String key) {
    return cache.computeIfAbsent(key, it -> new CacheItem<>());
  }

  /**
   * Tries to complete given future using cached value if using cache is allowed, cache exists and is up to date.
   *
   * @param useCache  only uses cache if allowed
   * @param cacheItem to use
   * @param future    to complete
   */
  protected boolean tryCachedResult(boolean useCache, CacheItem<T> cacheItem, Future<T> future) {
    if (useCache && cacheItem.isUpToDate()) {
      future.complete(cacheItem.get());
      return true;
    }
    return false;
  }
}