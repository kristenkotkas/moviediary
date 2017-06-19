package server.util;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rx.java.SingleOnSubscribeAdapter;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.vertx.VertxProfileManager;
import org.pac4j.vertx.VertxWebContext;
import rx.Single;
import server.security.SecurityConfig;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.vertx.rxjava.core.Future.failedFuture;
import static io.vertx.rxjava.core.Future.succeededFuture;

public class CommonUtils {

  public static boolean nonNull(Object... objects) {
    for (Object obj : objects) {
      if (obj == null) {
        return false;
      }
    }
    return true;
  }

  public static boolean contains(Object thiz, Object... objects) {
    Objects.requireNonNull(thiz, "Checkable object must exist.");
    for (Object object : objects) {
      if (thiz.equals(object)) {
        return true;
      }
    }
    return false;
  }

  public static CommonProfile getProfile(RoutingContext ctx, SecurityConfig securityConfig) {
    return new VertxProfileManager(new VertxWebContext(ctx.getDelegate(), securityConfig.getVertxSessionStore()))
        .get(true).orElse(null);
  }

  public static String getUsername(RoutingContext ctx, SecurityConfig securityConfig) {
    return getProfile(ctx, securityConfig).getEmail();
  }

  public static RSAPrivateKey getDerPrivateKey(byte[] keyBytes, String algorithm) throws Exception {
    return (RSAPrivateKey) KeyFactory.getInstance(algorithm).generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
  }

  /**
   * Changes vertx logging to SLF4J.
   */
  public static void setLoggingToSLF4J() {
    System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
  }

  public static <T> T ifPresent(T input, Consumer<T> consumer) {
    if (input != null) {
      consumer.accept(input);
      return input;
    }
    return null;
  }

  public static <T> boolean ifMissing(T input, Runnable runnable) {
    if (input == null) {
      runnable.run();
      return true;
    }
    return false;
  }

  public static boolean ifTrue(boolean check, Runnable runnable) {
    if (check) {
      runnable.run();
      return true;
    }
    return false;
  }

  public static boolean ifFalse(boolean check, Runnable runnable) {
    if (!check) {
      runnable.run();
      return false;
    }
    return true;
  }

  public static void check(boolean check, Runnable ifTrue, Runnable ifFalse) {
    if (check) {
      ifTrue.run();
    } else {
      ifFalse.run();
    }
  }

  public static Future<Boolean> check(boolean check) {
    return check ? succeededFuture() : failedFuture("check method returned false");
  }

  public static <T> T createIfMissing(T input, Supplier<T> supplier) {
    return input != null ? input : supplier.get();
  }

  public static <K, V> MapBuilder<K, V> mapBuilder() {
    return new MapBuilder<>();
  }

  public static <K, V> MapBuilder<K, V> mapBuilder(Map<K, V> map) {
    return new MapBuilder<>(map);
  }

  public static JsonArray getRows(JsonObject json) {
    return json.getJsonArray("rows");
  }

  public static class MapBuilder<K, V> {
    private final Map<K, V> map;

    private MapBuilder() {
      this(new HashMap<>());
    }

    private MapBuilder(Map<K, V> map) {
      this.map = map;
    }

    public MapBuilder<K, V> put(K key, V value) {
      map.put(key, value);
      return this;
    }

    public Map<K, V> build() {
      return map;
    }
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