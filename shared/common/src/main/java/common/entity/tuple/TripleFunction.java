package common.entity.tuple;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@FunctionalInterface
public interface TripleFunction<T, R, S> {
  Triple<T, R, S> apply(T t, R r, S s);
}
