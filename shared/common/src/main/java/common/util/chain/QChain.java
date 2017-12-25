package common.util.chain;

import common.entity.QFunction;
import common.entity.tuple.QuadConsumer;
import common.entity.tuple.QuadFunction;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.stream.Stream;

import static common.util.ConditionUtils.map;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class QChain<T, A, R, S> {
  private final T fst;
  private final A snd;
  private final R trd;
  private final S fth;

  public static <T, A, R, S> QChain<T, A, R, S> of(T fst, A snd, R trd, S fth) {
    return new QChain<>(fst, snd, trd, fth);
  }

  @SafeVarargs
  public final QChain<T, A, R, S> chain(QuadConsumer<T, A, R, S>... consumers) {
    if (consumers != null) {
      Stream.of(consumers).forEach(biConsumer -> biConsumer.accept(fst, snd, trd, fth));
    }
    return this;
  }

  public <V> SChain<V> mapSingle(QFunction<T, A, R, S, V> mapper) {
    return SChain.of(mapper.apply(fst, snd, trd, fth));
  }

  // TODO: 9.10.2017 pair mapper, triple mapper

  public QChain<T, A, R, S> mapQuad(QuadFunction<T, A, R, S> mapper) {
    return map(mapper.apply(fst, snd, trd, fth),
        quad -> QChain.of(quad.getFst(), quad.getSnd(), quad.getTrd(), quad.getFth()));
  }
}
