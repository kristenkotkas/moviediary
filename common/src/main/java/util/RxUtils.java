package util;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.rx.java.SingleOnSubscribeAdapter;
import rx.Single;

import java.util.function.Consumer;

public class RxUtils {

  public static <T> Single<T> single(Consumer<Handler<AsyncResult<T>>> consumer) {
    return Single.create(new SingleOnSubscribeAdapter<>(consumer));
  }
}
