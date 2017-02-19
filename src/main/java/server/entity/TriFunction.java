package server.entity;

@FunctionalInterface
public interface TriFunction<T, U, R, S> {
    S apply(T t, U u, R r);
}
