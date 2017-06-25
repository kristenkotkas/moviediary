package server.util;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.rx.java.SingleOnSubscribeAdapter;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.vertx.VertxProfileManager;
import org.pac4j.vertx.VertxWebContext;
import rx.Single;
import server.security.SecurityConfig;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class CommonUtils {

  public static CommonProfile getProfile(RoutingContext ctx, SecurityConfig securityConfig) {
    return new VertxProfileManager(new VertxWebContext(ctx.getDelegate(), securityConfig.getVertxSessionStore()))
        .get(true).orElse(null);
  }

  public static String getUsername(RoutingContext ctx, SecurityConfig securityConfig) {
    return getProfile(ctx, securityConfig).getEmail();
  }

  public static <T> Single<T> single(Consumer<Handler<AsyncResult<T>>> consumer) {
    return Single.create(new SingleOnSubscribeAdapter<>(consumer));
  }

  public static <T> Single<T> check(boolean check, T ifTrue, String ifError) {
    if (check) {
      return Single.just(ifTrue);
    }
    return Single.error(new Throwable(ifError));
  }

  public static <T> Single<T> check(boolean check, Supplier<Single<T>> supplier, Runnable ifError) {
    if (check) {
      return supplier.get();
    }
    ifError.run();
    return Single.just(null);
  }
}