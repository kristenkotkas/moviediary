package common.entity.tuple;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@FunctionalInterface
public interface PairFunction<T, S> {
  Pair<T, S> apply(T t, S s);
}
