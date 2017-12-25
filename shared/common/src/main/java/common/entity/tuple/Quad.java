package common.entity.tuple;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@Getter
@EqualsAndHashCode
@ToString
@Builder
public class Quad<T, A, R, S> {
  private T fst;
  private A snd;
  private R trd;
  private S fth;

  public static <T, A, R, S> Quad<T, A, R, S> of(T fst, A snd, R trd, S fth) {
    return new Quad<>(fst, snd, trd, fth);
  }

  public Quad<T, A, R, S> setFst(T fst) {
    this.fst = fst;
    return this;
  }

  public Quad<T, A, R, S> setSnd(A snd) {
    this.snd = snd;
    return this;
  }

  public Quad<T, A, R, S> setTrd(R trd) {
    this.trd = trd;
    return this;
  }

  public Quad<T, A, R, S> setFth(S fth) {
    this.fth = fth;
    return this;
  }
}
