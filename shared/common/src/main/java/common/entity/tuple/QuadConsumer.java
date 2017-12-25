package common.entity.tuple;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@FunctionalInterface
public interface QuadConsumer<T, A, R, S> {
  void accept(T t, A a, R r, S s);
}
