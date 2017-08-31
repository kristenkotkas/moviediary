package common.util;

import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.rxjava.core.Vertx;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
public class VertxUtils {
  private static final Vertx VERTX;

  static {
    setLoggingToSLF4J();
    VERTX = Vertx.vertx(new VertxOptions()
        .setAddressResolverOptions(new AddressResolverOptions()
            .addServer("8.8.8.8")
            .addServer("8.8.4.4")));
  }

  public static Vertx getVertx() {
    return VERTX;
  }

  /**
   * Changes vertx logging to SLF4J.
   */
  public static void setLoggingToSLF4J() {
    System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
  }

  public static Vertx toRxVertx(io.vertx.core.Vertx vertx) {
    return new Vertx(vertx);
  }
}
