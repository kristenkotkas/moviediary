package common.util.rx;

import common.entity.Mutable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.rx.java.RxHelper;
import rx.Observable;
import rx.Single;
import rx.Subscriber;
import java.util.Collection;
import java.util.function.Function;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
public class RxUtils {

  public static <T> Subscriber<T> toSubscriber(Handler<AsyncResult<T>> handler) {
    return RxHelper.toSubscriber(handler);
  }

  public static <T> Single<T> single() {
    return Single.just(null);
  }

  public static <T> Observable<T> observable() {
    return Observable.just(null);
  }

  public static <T> Single<T> singleForEach(Collection<T> collection, Function<T, Single<T>> function) {
    Mutable<Single<T>> single = Mutable.of(single());
    collection.forEach(s -> single.setIt(single.getIt().flatMap(t -> function.apply(s))));
    return single.getIt();
  }
}
