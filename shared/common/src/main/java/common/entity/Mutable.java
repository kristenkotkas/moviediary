package common.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@Getter
@Setter
@AllArgsConstructor(staticName = "of")
public class Mutable<T> {
  private T it;
}
