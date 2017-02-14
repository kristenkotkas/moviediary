package server.router;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import server.service.DatabaseService;

import static server.util.HandlerUtils.jsonResponse;
import static server.util.HandlerUtils.resultHandler;

public class DatabaseRouter extends Routable {
    public static final String API_USERS_ALL = "/api/users/all";

    private final DatabaseService database;

    public DatabaseRouter(Vertx vertx, DatabaseService database) {
        super(vertx);
        this.database = database;
    }

    @Override
    public void route(Router router) {
        router.get(API_USERS_ALL).handler(this::handleUsersAll);
    }

    private void handleUsersAll(RoutingContext ctx) {
        database.getAllUsers().setHandler(resultHandler(ctx, jsonResponse(ctx)));
    }
}
