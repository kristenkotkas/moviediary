package server.util;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.apache.http.HttpHeaders;

import java.util.function.Consumer;

import static server.entity.Status.serviceUnavailable;
import static server.util.CommonUtils.check;

public class HandlerUtils {
    private static final Logger LOG = LoggerFactory.getLogger(HandlerUtils.class);
    private static final String CONTENT_JSON = "application/json";

    public static String parseParam(String requestUri) {
        return requestUri.split(":")[1];
    }

    /**
     * End response with a JSON string.
     */
    public static <T> Consumer<T> jsonResponse(RoutingContext ctx) {
        return result -> ctx.response()
                .putHeader(HttpHeaders.CONTENT_TYPE, CONTENT_JSON)
                .end(Json.encodePrettily(result));
    }

    /**
     * A result handler that will call consumer of success and send service unavailable response on failure.
     */
    public static <T> Handler<AsyncResult<T>> resultHandler(RoutingContext ctx, Consumer<T> success) {
        return ar -> check(ar.succeeded(), () -> success.accept(ar.result()), () -> {
            LOG.error(ar.cause());
            serviceUnavailable(ctx, ar.cause());
        });
    }

    public static <T> Handler<AsyncResult<T>> resultHandler(Consumer<T> success, Consumer<AsyncResult<T>> failure) {
        return ar -> check(ar.succeeded(),
                () -> success.accept(ar.result()),
                () -> failure.accept(ar));
    }

    /**
     * Ends the response and sends client the result.
     *
     * @param ctx to use
     * @return this handler
     */
    public static Handler<AsyncResult<Buffer>> endHandler(RoutingContext ctx) {
        return ar -> check(ar.succeeded(),
                () -> ctx.getDelegate().response().end(ar.result()),
                () -> ctx.getDelegate().fail(ar.cause()));
    }
}
