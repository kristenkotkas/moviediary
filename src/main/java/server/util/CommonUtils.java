package server.util;

import java.util.Objects;

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
}
