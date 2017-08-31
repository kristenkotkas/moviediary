package common.entity;

import lombok.Builder;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@Builder
public class Triple<T, R, S> {
  private final T fst;
  private final R snd;
  private final S trd;
}
