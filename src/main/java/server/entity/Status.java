package server.entity;

import io.vertx.ext.web.RoutingContext;

/**
 * Contains http status codes and convenience methods.
 */
public class Status {
    public static final int OK = 200;
    public static final int CREATED = 201;
    public static final int NOCONTENT = 204;
    public static final int FOUND = 302;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int PRECONDITION_FAILED = 412;
    public static final int INTERNAL_SERVER_ERROR = 500;
    public static final int NOT_IMPLEMENTED = 501;
    public static final int SERVICE_UNAVAILABLE = 503;

    public static void ok(RoutingContext ctx) {
        ctx.response().setStatusCode(OK).end();
    }

    public static void notFound(RoutingContext ctx) {
        ctx.response().setStatusCode(NOT_FOUND).end();
    }

    public static void badRequest(RoutingContext ctx) {
        ctx.response().setStatusCode(BAD_REQUEST).end();
    }

    public static void serviceUnavailable(RoutingContext ctx) {
        ctx.response().setStatusCode(SERVICE_UNAVAILABLE).end();
    }

    public static void notImplemented(RoutingContext ctx) {
        ctx.response().setStatusCode(NOT_IMPLEMENTED).end();
    }
}
