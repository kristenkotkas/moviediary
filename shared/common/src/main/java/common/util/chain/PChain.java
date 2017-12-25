package common.util.chain;

import common.entity.tuple.Pair;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static common.util.ConditionUtils.map;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PChain<T, A> {
  private final T first;
  private final A second;

  public static <T, R> PChain<T, R> of(T item, R item2) {
    return new PChain<>(item, item2);
  }

  public Pair<T, A> getPair() {
    return Pair.of(first, second);
  }

  /**
   * Example:
   * PChain.of(1, 2).peek((first, second) -> doSomethingWith(first, second))
   */
  public PChain<T, A> peek(BiConsumer<T, A> consumer) {
    consumer.accept(first, second);
    return this;
  }

  /**
   * Example:
   * PChain.of(1, 2).peekFst(first -> doSomethingWith(first))
   */
  public PChain<T, A> peekFst(Consumer<T> consumer) {
    consumer.accept(first);
    return this;
  }

  /**
   * Example:
   * PChain.of(1, 2).peekSnd(second -> doSomethingWith(second))
   */
  public PChain<T, A> peekSnd(Consumer<A> consumer) {
    consumer.accept(second);
    return this;
  }

  /**
   * Example:
   * PChain.of(1, 2).mapFst(first -> first + 1) => PChain.of(2, 2)
   */
  public <S> PChain<S, A> mapFst(Function<T, S> mapper) {
    return PChain.of(mapper.apply(first), second);
  }

  /**
   * Example:
   * PChain.of(1, 2).mapFst((first, second) -> first + second) => PChain.of(3, 2)
   */
  public <S> PChain<S, A> mapFst(BiFunction<T, A, S> mapper) {
    return PChain.of(mapper.apply(first, second), second);
  }

  /**
   * Example:
   * PChain.of(1, 2).mapSnd(second -> second + 1) => PChain.of(1, 3)
   */
  public <S> PChain<T, S> mapSnd(Function<A, S> mapper) {
    return PChain.of(first, mapper.apply(second));
  }

  /**
   * Example:
   * PChain.of(1, 2).mapSnd((first, second) -> first + second) => PChain.of(1, 3)
   */
  public <S> PChain<T, S> mapSnd(BiFunction<T, A, S> mapper) {
    return PChain.of(first, mapper.apply(first, second));
  }

  /**
   * Example:
   * PChain.of(1, 2).mapSChain((first, second) -> first + second) => SChain.of(3)
   */
  public <S> SChain<S> mapSChain(BiFunction<T, A, S> mapper) {
    return SChain.of(mapper.apply(first, second));
  }

  /**
   * Example:
   * PChain.of(1, 2).mapPChain((first, second) -> Pair.of(first + 1, second + 2)) => PChain.of(2, 4)
   */
  public <R, S> PChain<R, S> mapPChain(PairToPairFunction<T, A, R, S> mapper) {
    return map(mapper.apply(first, second), pair -> PChain.of(pair.getFst(), pair.getSnd()));
  }

  /**
   * Example:
   * PChain.of(1, 2).mapTChain((first, second) -> first + second) => TChain.of(1, 2, 3)
   */
  public <R> TChain<T, A, R> mapTChain(BiFunction<T, A, R> mapper) {
    return TChain.of(first, second, mapper.apply(first, second));
  }

  /**
   * Example:
   * PChain.of(1, 2).mapQChain((first, second) -> Pair.of(first + 2, second + 3)) => QChain.of(1, 2, 3, 5)
   */
  public <R, S> QChain<T, A, R, S> mapQChain(PairToPairFunction<T, A, R, S> mapper) {
    return map(mapper.apply(first, second), pair -> QChain.of(first, second, pair.getFst(), pair.getSnd()));
  }

  /**
   * Example:
   * PChain.of(1, 2).toTChain(3) => TChain.of(1, 2, 3)
   */
  public <S> TChain<T, A, S> toTChain(S trd) {
    return TChain.of(first, second, trd);
  }

  /**
   * Example:
   * PChain.of(1, 2).toQChain(3, 4) => QChain.of(1, 2, 3, 4)
   */
  public <R, S> QChain<T, A, R, S> toQChain(R trd, S fth) {
    return QChain.of(first, second, trd, fth);
  }

  public interface PairToPairFunction<T, A, R, S> {
    Pair<R, S> apply(T t, A a);
  }
}
