package common.entity.tuple;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Pair<T, S> {
  private final T fst;
  private final S snd;

  public static <T, S> Pair<T, S> of(T fst, S snd) {
    return new Pair<>(fst, snd);
  }

  public static <T, S> PairBuilder<T, S> builder() {
    return new PairBuilder<>();
  }

  public <R> Pair<R, S> mapFst(R fst) {
    return Pair.of(fst, snd);
  }

  public <R> Pair<T, R> mapSnd(R snd) {
    return Pair.of(fst, snd);
  }

  public static class PairBuilder<T, S> {
    private T fst;
    private S snd;

    public PairBuilder<T, S> fst(T fst) {
      this.fst = fst;
      return this;
    }

    public PairBuilder<T, S> snd(S snd) {
      this.snd = snd;
      return this;
    }

    public Pair<T, S> build() {
      return new Pair<>(fst, snd);
    }

    public String toString() {
      return "Pair.PairBuilder(fst=" + this.fst + ", snd=" + this.snd + ")";
    }
  }
}
