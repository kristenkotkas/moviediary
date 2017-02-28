package server.util;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.vertx.VertxProfileManager;
import org.pac4j.vertx.VertxWebContext;
import sun.misc.BASE64Decoder;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Objects;

public class CommonUtils {
    private static final Logger log = LoggerFactory.getLogger(CommonUtils.class);

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

    public static PrivateKey getPemPrivateKey(String temp, String algorithm) throws Exception {

        String privKeyPEM = temp.replace("-----BEGIN " + algorithm + " PRIVATE KEY-----", "");
        privKeyPEM = privKeyPEM.replace("-----END " + algorithm + " PRIVATE KEY-----", "");
        //System.out.println("Private key\n"+privKeyPEM);

        BASE64Decoder b64=new BASE64Decoder();
        byte[] decoded = b64.decodeBuffer(privKeyPEM);

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance(algorithm);
        return kf.generatePrivate(spec);
    }
}