package server.util;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

import java.util.function.Consumer;

import static server.entity.Status.serviceUnavailable;

public class HandlerUtils {
    private static final Logger log = LoggerFactory.getLogger(HandlerUtils.class);
    private static final String CONTENT_JSON = "application/json";

    public static String parseParam(String requestUri) {
        return requestUri.split(":")[1];
    }

    public static <T> Consumer<T> jsonResponse(RoutingContext ctx) {
        return result -> ctx.response()
                .putHeader(HttpHeaders.CONTENT_TYPE, CONTENT_JSON)
                .end(Json.encodePrettily(result));
    }

    public static <T> Handler<AsyncResult<T>> resultHandler(RoutingContext ctx, Consumer<T> success) {
        return ar -> {
            if (ar.succeeded()) {
                success.accept(ar.result());
            } else {
                log.error(ar.cause());
                serviceUnavailable(ctx, ar.cause());
            }
        };
    }

    /**
     * Completes or fails future based on result.
     *
     * @param future to use
     * @return this handler
     */
    public static <T> Handler<AsyncResult<T>> futureHandler(Future<Void> future) {
        return ar -> {
            if (ar.succeeded()) {
                future.complete();
            } else {
                future.fail(ar.cause());
            }
        };
    }
}
