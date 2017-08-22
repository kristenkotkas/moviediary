package common.util;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Status {
  public static final int OK = 200;
  public static final int CREATED = 201;
  public static final int NOCONTENT = 204;
  public static final int FOUND = 302;
  public static final int BAD_REQUEST = 400;
  public static final int UNAUTHORIZED = 401;
  public static final int FORBIDDEN = 403;
  public static final int NOT_FOUND = 404;
  public static final int PRECONDITION_FAILED = 412;
  public static final int IM_A_TEAPOT = 418;
  public static final int RATE_LIMIT = 429;
  public static final int INTERNAL_SERVER_ERROR = 500;
  public static final int NOT_IMPLEMENTED = 501;
  public static final int BAD_GATEWAY = 501;
  public static final int SERVICE_UNAVAILABLE = 503;

  public static final String CONTENT_TYPE = "content-type";
  public static final String JSON = "application/json";

  public static void ok(RoutingContext ctx, Throwable ex) {
    ctx.response()
       .setStatusCode(OK)
       .putHeader(CONTENT_TYPE, JSON)
       .end();
  }

  public static void badRequest(RoutingContext ctx, Throwable ex) {
    ctx.response()
       .setStatusCode(BAD_REQUEST)
       .putHeader(CONTENT_TYPE, JSON)
       .end(new JsonObject().put("error", ex.getMessage()).encodePrettily());
  }

  public static void notFound(RoutingContext ctx) {
    ctx.response()
       .setStatusCode(NOT_FOUND)
       .putHeader(CONTENT_TYPE, JSON)
       .end(new JsonObject().put("error", "not_found").encodePrettily());
  }

  public static void internalError(RoutingContext ctx, Throwable ex) {
    ctx.response()
       .setStatusCode(INTERNAL_SERVER_ERROR)
       .putHeader(CONTENT_TYPE, JSON)
       .end(new JsonObject().put("error", ex.getMessage()).encodePrettily());
  }

  public static void notImplemented(RoutingContext ctx) {
    ctx.response()
       .setStatusCode(NOT_IMPLEMENTED)
       .putHeader(CONTENT_TYPE, JSON)
       .end(new JsonObject().put("error", "not_implemented").encodePrettily());
  }

  public static void badGateway(RoutingContext ctx, Throwable ex) {
    log.error("Bad gateway", ex);
    ctx.response()
       .setStatusCode(BAD_GATEWAY)
       .putHeader(CONTENT_TYPE, JSON)
       .end(new JsonObject().put("error", "bad_gateway").encodePrettily());
  }

  public static void serviceUnavailable(RoutingContext ctx) {
    ctx.fail(SERVICE_UNAVAILABLE);
  }

  public static void serviceUnavailable(RoutingContext ctx, Throwable ex) {
    ctx.response()
       .setStatusCode(SERVICE_UNAVAILABLE)
       .putHeader(CONTENT_TYPE, JSON)
       .end(new JsonObject().put("error", ex.getMessage()).encodePrettily());
  }

  public static void serviceUnavailable(RoutingContext ctx, String cause) {
    ctx.response()
       .setStatusCode(SERVICE_UNAVAILABLE)
       .putHeader(CONTENT_TYPE, JSON)
       .end(new JsonObject().put("error", cause).encodePrettily());
  }
}
