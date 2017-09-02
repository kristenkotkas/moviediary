package common.entity;

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
public class Triple<T, R, S> {
  private T fst;
  private R snd;
  private S trd;

  public Triple<T, R, S> setFst(T fst) {
    this.fst = fst;
    return this;
  }

  public Triple<T, R, S> setSnd(R snd) {
    this.snd = snd;
    return this;
  }

  public Triple<T, R, S> setTrd(S trd) {
    this.trd = trd;
    return this;
  }
}
