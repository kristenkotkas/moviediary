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
public class Pair<T, S> {
  private T fst;
  private S snd;

  public Pair<T, S> setFst(T fst) {
    this.fst = fst;
    return this;
  }

  public Pair<T, S> setSnd(S snd) {
    this.snd = snd;
    return this;
  }
}
