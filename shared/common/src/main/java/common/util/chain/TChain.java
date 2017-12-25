package common.util.chain;

import common.entity.TriFunction;
import common.entity.tuple.Pair;
import common.entity.tuple.TriConsumer;
import common.entity.tuple.Triple;
import common.entity.tuple.TripleFunction;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

import static common.util.ConditionUtils.map;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class TChain<T, A, R> {
  private final T first;
  private final A second;
  private final R third;

  public static <T, A, R> TChain<T, A, R> of(T fst, A snd, R trd) {
    return new TChain<>(fst, snd, trd);
  }

  public Triple<T, A, R> getTriple() {
    return Triple.of(first, second, third);
  }

  public TChain<T, A, R> peek(TriConsumer<T, A, R> consumer) {
    consumer.accept(first, second, third);
    return this;
  }

  // TODO: 9.10.2017 all

  public <S> SChain<S> mapSingle(TriFunction<T, A, R, S> mapper) {
    return SChain.of(mapper.apply(first, second, third));
  }

  public <S, V> PChain<S, V> mapPair(TripleToPairFunction<T, A, R, S, V> mapper) {
    return map(mapper.apply(first, second, third), pair -> PChain.of(pair.getFst(), pair.getSnd()));
  }

  public TChain<T, A, R> mapTriple(TripleFunction<T, A, R> mapper) {
    return map(mapper.apply(first, second, third),
        triple -> TChain.of(triple.getFst(), triple.getSnd(), triple.getTrd()));
  }

  public <S> TChain<S, A, R> mapTripleFst(Function<T, S> mapper) {
    return TChain.of(mapper.apply(first), second, third);
  }

  public <S> TChain<T, S, R> mapTripleSnd(Function<A, S> mapper) {
    return TChain.of(first, mapper.apply(second), third);
  }

  public <S> TChain<T, A, S> mapTripleTrd(Function<R, S> mapper) {
    return TChain.of(first, second, mapper.apply(third));
  }

  public <S> QChain<T, A, R, S> mapQuad(TriFunction<T, A, R, S> mapper) {
    return QChain.of(first, second, third, mapper.apply(first, second, third));
  }

  public <S> QChain<T, A, R, S> toQuad(S fth) {
    return QChain.of(first, second, third, fth);
  }

  public interface TripleToPairFunction<T, A, R, S, V> {
    Pair<S, V> apply(T t, A a, R r);
  }
}
