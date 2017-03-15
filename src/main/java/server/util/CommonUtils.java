package server.util;

import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.vertx.VertxProfileManager;
import org.pac4j.vertx.VertxWebContext;


import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Objects;
import java.util.function.Consumer;

public class CommonUtils {
    private static final Logger LOG = LoggerFactory.getLogger(CommonUtils.class);

    public static boolean nonNull(Object... objects) {
        for (Object obj : objects) {
            if (obj == null) {
                return false;
            }
        }
        return true;
    }

    public static boolean contains(Object thiz, Object... objects) {
        Objects.requireNonNull(thiz, "Checkable object must exist.");
        for (Object object : objects) {
            if (thiz.equals(object)) {
                return true;
            }
        }
        return false;
    }

    public static CommonProfile getProfile(RoutingContext ctx) {
        return new VertxProfileManager(new VertxWebContext(ctx)).get(true).orElse(null);
    }

    public static RSAPrivateKey getDerPrivateKey(byte[] keyBytes, String algorithm) throws Exception {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        return (RSAPrivateKey) KeyFactory.getInstance(algorithm).generatePrivate(spec);
    }

    /**
     * Changes vertx logging to SLF4J.
     */
    public static void setLoggingToSLF4J() {
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
    }

    public static <T> Future<T> future(Consumer<Future<T>> consumer) {
        Future<T> future = Future.future();
        consumer.accept(future);
        return future;
    }
}