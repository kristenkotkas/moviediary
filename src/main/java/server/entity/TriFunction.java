package server.entity;

@FunctionalInterface
public interface TriFunction<T, A, R, S> {
    S apply(T t, A u, R r);
}
