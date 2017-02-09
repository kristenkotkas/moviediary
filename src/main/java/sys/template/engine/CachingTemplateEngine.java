package sys.template.engine;

import io.vertx.ext.web.impl.ConcurrentLRUCache;

import java.util.Objects;

/**
 * Caches templates.
 * <p>
 * Original source: https://github.com/vert-x3/vertx-web/
 *
 * @author <a href="https://bitbucket.org/kristjanhk/">Kristjan Hendrik Küngas</a>
 */
public abstract class CachingTemplateEngine<T> {
    protected final ConcurrentLRUCache<String, T> cache;
    protected String extension;

    protected CachingTemplateEngine(String ext, int maxCacheSize) {
        Objects.requireNonNull(ext);
        if (maxCacheSize < 1) {
            throw new IllegalArgumentException("maxCacheSize must be >= 1");
        }
        doSetExtension(ext);
        cache = new ConcurrentLRUCache<>(maxCacheSize);
    }

    protected String adjustLocation(String location) {
        if (!location.endsWith(extension)) {
            location += extension;
        }
        return location;
    }

    protected void doSetExtension(String ext) {
        extension = ext.charAt(0) == '.' ? ext : "." + ext;
    }
}