package server.util;

public class CommonUtils {

    public static boolean noneOf(Object thiz, Object... objects) {
        for (Object object : objects) {
            if (thiz.equals(object)) {
                return false;
            }
        }
        return true;
    }
}
