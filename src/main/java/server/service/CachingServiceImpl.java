package server.service;

import io.vertx.core.Future;
import io.vertx.ext.web.impl.ConcurrentLRUCache;

/**
 * Caching for services.
 **/
public abstract class CachingServiceImpl<T> implements CachingService<T> {
    protected static final int DEFAULT_MAX_CACHE_SIZE = 10000;
    private final ConcurrentLRUCache<String, CacheItem<T>> cache;

    protected CachingServiceImpl(int maxCacheSize) {
        if (maxCacheSize < 1) {
            throw new IllegalArgumentException("maxCacheSize must be >= 1");
        }
        cache = new ConcurrentLRUCache<>(maxCacheSize);
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
        if (useCache) {
            if (cacheItem.isUpToDate()) {
                future.complete(cacheItem.get());
                return true;
            }
        }
        return false;
    }
}