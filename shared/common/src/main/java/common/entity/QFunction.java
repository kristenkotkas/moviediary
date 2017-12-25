package common.entity;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@FunctionalInterface
public interface QFunction<T, A, R, S, V> {
  V apply(T t, A a, R r, S s);
}
