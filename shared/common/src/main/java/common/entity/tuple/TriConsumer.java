package common.entity.tuple;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@FunctionalInterface
public interface TriConsumer<T, R, S> {
  void accept(T t, R r, S s);
}
