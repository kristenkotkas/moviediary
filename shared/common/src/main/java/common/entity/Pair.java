package common.entity;

import lombok.Builder;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@Builder
public class Pair<T, S> {
  private final T fst;
  private final S snd;
}
