package server.router;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import server.service.DatabaseService;

import static server.entity.Status.CREATED;
import static server.entity.Status.serviceUnavailable;
import static server.service.DatabaseService.getRows;
import static server.util.CommonUtils.noneOf;
import static server.util.HandlerUtils.jsonResponse;
import static server.util.HandlerUtils.resultHandler;

public class DatabaseRouter extends Routable {
    public static final String API_USERS_ALL = "/api/users/all";
    public static final String API_USERS_INSERT = "/api/users/insert";
    public static final String API_VIEWS_ALL = "/api/views/all";

    private final DatabaseService database;

    public DatabaseRouter(Vertx vertx, DatabaseService database) {
        super(vertx);
        this.database = database;
    }

    @Override
    public void route(Router router) {
        router.get(API_USERS_ALL).handler(this::handleUsersAll);
        router.post(API_USERS_INSERT).handler(this::handleUsersInsert);
        router.get(API_VIEWS_ALL).handler(this::handleViewsAll);
    }

    private void handleUsersInsert(RoutingContext ctx) {
        String username = ctx.request().getFormAttribute("username");
        String password = ctx.request().getFormAttribute("password");
        String firstname = ctx.request().getFormAttribute("firstname");
        String lastname = ctx.request().getFormAttribute("lastname");
        if (!noneOf("", username, password, firstname, lastname)) {
            serviceUnavailable(ctx, new Throwable("All fields must be filled!"));
            return;
        }
        database.getUser(username).setHandler(resultHandler(ctx, result -> {
            boolean exists = getRows(result).stream()
                    .map(obj -> (JsonObject) obj)
                    .anyMatch(json -> json.getString("Email").equals(username));
            if (!exists) {
                database.insertUser(new JsonArray()
                        .add(username).add(password)
                        .add("unique")
                        .add(firstname).add(lastname))
                        .setHandler(resultHandler(ctx, ar -> ctx.response()
                                .setStatusCode(CREATED)
                                .end("Account registered!: " + ar.encodePrettily())));
            } else {
                serviceUnavailable(ctx, new Throwable("User already exists!"));
            }
        }));
    }

    private void handleUsersAll(RoutingContext ctx) {
        database.getAllUsers().setHandler(resultHandler(ctx, jsonResponse(ctx)));
    }

    private void handleViewsAll(RoutingContext ctx) {
        database.getAllViews().setHandler(resultHandler(ctx, jsonResponse(ctx)));
    }
}
