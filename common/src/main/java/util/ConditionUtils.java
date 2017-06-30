package util;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ConditionUtils {

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

  public static boolean ifTrue(boolean check, Runnable runnable) {
    if (check) {
      runnable.run();
      return true;
    }
    return false;
  }

  public static boolean ifFalse(boolean check, Runnable runnable) {
    if (!check) {
      runnable.run();
      return false;
    }
    return true;
  }

  public static void check(boolean check, Runnable ifTrue, Runnable ifFalse) {
    if (check) {
      ifTrue.run();
    } else {
      ifFalse.run();
    }
  }

  public static <T> T ifPresent(T input, Consumer<T> consumer) {
    if (input != null) {
      consumer.accept(input);
      return input;
    }
    return null;
  }

  public static <T> T createIfMissing(T input, Supplier<T> supplier) {
    return input != null ? input : supplier.get();
  }
}
