package common.util;

import lombok.NonNull;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
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

  public static boolean ifTrue(boolean check, Runnable ifTrue, Runnable... andThen) {
    if (check) {
      ifTrue.run();
      return true;
    }
    Stream.of(andThen).forEach(Runnable::run);
    return false;
  }

  public static boolean ifFalse(boolean check, Runnable ifFalse, Runnable... andThen) {
    if (!check) {
      ifFalse.run();
      return false;
    }
    Stream.of(andThen).forEach(Runnable::run);
    return true;
  }

  public static void check(boolean check, Runnable ifTrue, Runnable ifFalse, Runnable... andThen) {
    if (check) {
      ifTrue.run();
    } else {
      ifFalse.run();
    }
    Stream.of(andThen).forEach(Runnable::run);
  }

  public static <T> T ifPresent(T input, Consumer<T> consumer) {
    if (input != null) {
      consumer.accept(input);
    }
    return input;
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

  @SafeVarargs
  public static <T> T chain(T input, Consumer<T>... consumers) {
    return ifPresent(input, in -> Arrays.stream(consumers).forEach(consumer -> consumer.accept(in)));
  }

  public static <T, S> S map(T input, Function<T, S> mapper) {
    return mapper.apply(input);
  }

  public static <T> T def(T input, T def) {
    return input != null ? input : def;
  }
}
