package common.util;

import io.vertx.core.Future;
import io.vertx.core.impl.CompositeFutureImpl;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FutureUtils {

  public static <T> Future<List<T>> reduce(List<Future<T>> futures) {
    return CompositeFutureImpl.all(futures.toArray(new Future[futures.size()]))
                              .map(v -> futures.stream()
                                               .map(Future::result)
                                               .collect(Collectors.toList()));
  }
}
