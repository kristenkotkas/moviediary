package common.entity;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@FunctionalInterface
public interface TriFunction<T, A, R, S> {
  S apply(T t, A a, R r);
}
