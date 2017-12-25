package common.util.chain;

import common.entity.tuple.Pair;
import common.entity.tuple.Triple;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Consumer;
import java.util.function.Function;

import static common.util.ConditionUtils.map;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SChain<T> {
  private final T first;

  public static <T> SChain<T> of(T item) {
    return new SChain<>(item);
  }

  public SChain<T> peek(Consumer<T> consumer) {
    consumer.accept(first);
    return this;
  }

  /**
   * Example:
   * SChain.of(1).mapSChain(nr -> nr + 1) => SChain.of(2)
   */
  public <S> SChain<S> mapSChain(Function<T, S> mapper) {
    return SChain.of(mapper.apply(first));
  }

  /**
   * Example:
   * SChain.of(1).mapPChain(nr -> nr + 1) => PChain.of(1, 2)
   */
  public <S> PChain<T, S> mapPChain(Function<T, S> mapper) {
    return PChain.of(first, mapper.apply(first));
  }

  /**
   * Example:
   * SChain.of(1).mapTChain(nr -> Pair.of(nr + 1, nr + 2)) => TChain.of(1, 2, 3)
   */
  public <R, S> TChain<T, R, S> mapTChain(SingleToPairFunction<T, R, S> mapper) {
    return map(mapper.apply(first), pair -> TChain.of(first, pair.getFst(), pair.getSnd()));
  }

  /**
   * Example:
   * SChain.of(1).mapQChain(nr -> Triple.of(nr + 1, nr + 2, nr + 3)) => QChain.of(1, 2, 3, 4)
   */
  public <A, R, S> QChain<T, A, R, S> mapQChain(SingleToTripleFunction<T, A, R, S> mapper) {
    return map(mapper.apply(first), tri -> QChain.of(first, tri.getFst(), tri.getSnd(), tri.getTrd()));
  }

  /**
   * Example:
   * SChain.of(1).toPChain(2) => PChain.of(1, 2)
   */
  public <S> PChain<T, S> toPChain(S snd) {
    return PChain.of(first, snd);
  }

  /**
   * Example:
   * SChain.of(1).toTChain(2, 3) => TChain.of(1, 2, 3)
   */
  public <R, S> TChain<T, R, S> toTChain(R snd, S trd) {
    return TChain.of(first, snd, trd);
  }

  /**
   * Example:
   * SChain.of(1).toQChain(2, 3, 4) => QChain.of(1, 2, 3, 4)
   */
  public <A, R, S> QChain<T, A, R, S> toQChain(A snd, R trd, S fth) {
    return QChain.of(first, snd, trd, fth);
  }

  public interface SingleToPairFunction<T, R, S> {
    Pair<R, S> apply(T t);
  }

  public interface SingleToTripleFunction<T, A, R, S> {
    Triple<A, R, S> apply(T t);
  }
}
