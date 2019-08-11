package server.router;

import io.vertx.core.impl.Action;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import server.entity.Event;
import server.entity.Privilege;
import server.entity.Status;
import server.entity.admin.AdminCountParams;
import server.entity.admin.AdminSessionsParams;
import server.entity.admin.BadRequestException;
import server.service.DatabaseService;

import java.util.Arrays;
import java.util.stream.Collectors;

import static server.entity.Status.badRequest;
import static server.util.CommonUtils.check;
import static server.util.HandlerUtils.resultHandler;

public class AdminRouter implements Routable {

    private static final String PARAM_API_KEY = "api_key";

    private static final String API_USER_NEW = "/public/api/v1/admin/user/new*";
    private static final String API_USER_SESSION = "/public/api/v1/admin/user/session*";

    private final DatabaseService database;

    public AdminRouter(DatabaseService database) {
        this.database = database;
    }

    // TODO: 6. juuni. 2019 lisada special characterite handlemine
    @Override
    public void route(Router router) {
        router.get(API_USER_NEW).handler(this::handleUserNew);
        router.get(API_USER_SESSION).handler(this::handleUserSession);
    }

    /**
     * Returns current users count in database as String response.
     */
    private void handleUserNew(RoutingContext ctx) {
        AdminCountParams params = new AdminCountParams(ctx.request().params());
        try {
            params.checkParameters();
            handlePrivilegeProtectedResource(ctx, Privilege.ADMIN, API_USER_NEW,
                    () -> database.getNewUsersCount(params));
        } catch (BadRequestException e) {
            badRequest(ctx, e);
        }
    }

    private void handleUserSession(RoutingContext ctx) {
        AdminSessionsParams params = new AdminSessionsParams(ctx.request().params());
        try {
            params.checkParameters();
            handlePrivilegeProtectedResource(ctx, Privilege.ADMIN, API_USER_SESSION, () -> getUsersSessions(params));
        } catch (BadRequestException e) {
            badRequest(ctx, e);
        }
    }

    /**
     * Returns required resource if api key provided has required privilege.
     */
    private void handlePrivilegeProtectedResource(RoutingContext ctx, Privilege privilege, String logData,
                                                  Action<Future<JsonObject>> action) {
        String apiKey = ctx.request().getParam(PARAM_API_KEY);
        if (apiKey == null) {
            badRequest(ctx);
            return;
        }
        database.insertApiKeyEvent(apiKey, Event.API_REQUEST, logData);
        database.isPrivilegeGranted(apiKey, privilege)
                .setHandler(resultHandler(ctx, result -> check(result,
                        () -> action.perform().rxSetHandler()
                                .doOnError(ctx::fail)
                                .subscribe(res -> ctx.response().end(res.encodePrettily())),
                        () -> ctx.response()
                                .setStatusCode(Status.FORBIDDEN)
                                .end(Status.FORBIDDEN + ": Forbidden\nCause: Not enough privileges"))
                ));
    }

    private Future<JsonObject> getUsersSessions(AdminSessionsParams params) {
        return database.getUsersSessions(params).map(obj -> {
            obj.getJsonArray("rows").forEach(row -> {
                JsonObject json = ((JsonObject) row);
                json.put("login", new JsonArray(
                        Arrays.stream(json.getString("login").split(","))
                                .map(elem -> new JsonObject()
                                        .put("timestamp", elem.split("_")[0])
                                        .put("client", elem.split("_")[1])
                                ).collect(Collectors.toList())));
            });
            return obj;
        });
    }
}
