package server.util;

import io.vertx.ext.web.RoutingContext;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.vertx.VertxProfileManager;
import org.pac4j.vertx.VertxWebContext;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class CommonUtils {

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

    /**
     * Lowercases and capitalizes given name.
     *
     * @param name to use
     * @return capitalized text string
     */
    public static String capitalizeName(String name) {
        return Arrays.stream(name.split(" "))
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    public static CommonProfile getProfile(RoutingContext ctx) {
        ProfileManager<CommonProfile> profileManager = new VertxProfileManager(new VertxWebContext(ctx));
        Optional<CommonProfile> profile = profileManager.get(true);
        return profile.orElse(null);
    }

}
