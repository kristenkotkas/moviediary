package common.util;

import lombok.NonNull;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
public class ConditionUtils {

  public static boolean nonNull(Object... objects) {
    return Arrays.stream(objects).noneMatch(Objects::isNull);
  }

  public static boolean contains(@NonNull Object thiz, Object... objects) {
    return Arrays.stream(objects).anyMatch(thiz::equals);
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

  public static <T> T ifMissing(T input, Supplier<T> ifMissing) {
    if (input == null) {
      return ifMissing.get();
    }
    return input;
  }

  @SafeVarargs
  public static <T> T coalesce(T input, T... alternatives) {
    return Stream.concat(Stream.of(input), Arrays.stream(alternatives))
                 .filter(Objects::nonNull)
                 .findFirst()
                 .orElse(null);
  }
}
