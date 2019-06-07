package server.router;

import io.vertx.core.impl.Action;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import server.entity.Event;
import server.entity.Privilege;
import server.entity.Status;
import server.entity.admin.AdminCountParams;
import server.entity.admin.BadRequestException;
import server.service.AdminService;
import server.service.DatabaseService;

import static server.entity.Status.badRequest;
import static server.util.CommonUtils.check;
import static server.util.HandlerUtils.resultHandler;

public class AdminRouter implements Routable {

    private static final String PARAM_API_KEY = "api_key";

    private static final String API_USERS_NEW = "/public/api/v1/admin/user/new*";

    private final AdminService adminService;
    private final DatabaseService database;

    public AdminRouter(AdminService adminService, DatabaseService database) {
        this.adminService = adminService;
        this.database = database;
    }

    // TODO: 6. juuni. 2019 lisada special characterite handlemine
    @Override
    public void route(Router router) {
        router.get(API_USERS_NEW).handler(this::handleUserNew);
    }

    /**
     * Returns current users count in database as String response.
     */
    private void handleUserNew(RoutingContext ctx) {
        AdminCountParams params = new AdminCountParams(ctx.request().params());
        try {
            params.checkParameters();
            handlePrivilegeProtectedResource(ctx, Privilege.ADMIN, API_USERS_NEW,
                    () -> database.getNewUsersCount(params));
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
}
