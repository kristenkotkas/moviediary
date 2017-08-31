package common.util.rx;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FutureRxUtils {

  // FIXME: 22.08.2017 breaks apiGen

/*  public static <T> Future<List<T>> reduce(List<Future<T>> futures) {
    return new Future<>(common.util.FuncUtils.reduce(futures.stream()
                                                            .map(Future::getDelegate)
                                                            .collect(Collectors.<io.vertx.core.Future<T>>toList())));
  }*/
}
