package server.util;

public class CommonUtils {

    public static boolean contains(Object thiz, Object... objects) {
        for (Object object : objects) {
            if (thiz.equals(object)) {
                return true;
            }
        }
        return false;
    }
}
