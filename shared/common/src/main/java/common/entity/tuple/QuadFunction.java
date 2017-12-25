package common.entity.tuple;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@FunctionalInterface
public interface QuadFunction<T, A, R, S> {
  Quad<T, A, R, S> apply(T t, A a, R r, S s);
}
