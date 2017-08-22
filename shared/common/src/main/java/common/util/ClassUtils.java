package common.util;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
public class ClassUtils {

  @SuppressWarnings("unchecked")
  public static <T> T uncheckedCast(Object value) throws ClassCastException {
    return (T) value;
  }

  public static <T> T checkedCast(Object value, Class<T> clazz) {
    return clazz.isInstance(value) ? clazz.cast(value) : null;
  }
}
