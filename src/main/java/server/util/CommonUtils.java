package server.util;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.vertx.VertxProfileManager;
import org.pac4j.vertx.VertxWebContext;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Objects;

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

    public static PrivateKey getDerPrivateKey(byte[] keyBytes, String algorithm) throws Exception {
        PKCS8EncodedKeySpec spec =
                new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance(algorithm);
        return kf.generatePrivate(spec);
    }
}