package server.entity;

/**
 * Represents a function that accepts three arguments and produces a result.
 */
@FunctionalInterface
public interface TriFunction<T, A, R, S> {
    S apply(T t, A a, R r);
}
