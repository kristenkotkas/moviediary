package server.service;

import java.util.concurrent.TimeUnit;

public interface CachingService<T> {

    /**
     * Gets cached item or creates a new one if it does not exist.
     *
     * @param key to get cache value from
     */
    CacheItem<T> getCached(String key);

    /**
     * Cached value wrapper.
     * Default timeout is 1 hour.
     **/
    class CacheItem<T> {
        private long timeout = TimeUnit.HOURS.toMillis(1);
        private long timestamp = 0;
        private T value;

        public T get() {
            return value;
        }

        public T set(T value) {
            this.value = value;
            timestamp = System.currentTimeMillis();
            return value;
        }

        public CacheItem<T> setTimeout(TimeUnit unit, long length) {
            timeout = unit.toMillis(length);
            return this;
        }

        public boolean isUpToDate() {
            return System.currentTimeMillis() - timestamp <= timeout;
        }
    }
}