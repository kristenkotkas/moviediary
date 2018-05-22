package server.entity;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@FunctionalInterface
public interface FunctionThrowingEx<T, S> {
  S apply(T t) throws Exception;
}
