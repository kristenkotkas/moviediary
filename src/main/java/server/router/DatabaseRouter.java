package server.router;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.pac4j.http.client.indirect.FormClient;
import server.security.SecurityConfig;
import server.service.DatabaseService;

import static io.vertx.core.http.HttpHeaders.LOCATION;
import static server.entity.Status.*;
import static server.router.UiRouter.UI_FORM_REGISTER;
import static server.service.DatabaseService.getRows;
import static server.util.CommonUtils.contains;
import static server.util.HandlerUtils.jsonResponse;
import static server.util.HandlerUtils.resultHandler;

public class DatabaseRouter extends Routable {
    public static final String USER_EXISTS = "userExists";

    public static final String API_USERS_ALL = "/api/users/all";
    public static final String API_USERS_INSERT = "/api/users/insert";
    public static final String API_VIEWS_ALL = "/api/views/all";

    private final DatabaseService database;
    private final SecurityConfig securityConfig;

    public DatabaseRouter(Vertx vertx, DatabaseService database, SecurityConfig securityConfig) {
        super(vertx);
        this.database = database;
        this.securityConfig = securityConfig;
    }

    @Override
    public void route(Router router) {
        router.get(API_USERS_ALL).handler(this::handleUsersAll);
        router.post(API_USERS_INSERT).handler(this::handleUsersInsert);
        router.get(API_VIEWS_ALL).handler(this::handleViewsAll);
    }

    // TODO: 19.02.2017 different inserts for all auth methods
    private void handleUsersInsert(RoutingContext ctx) {
        String username = ctx.request().getFormAttribute("username");
        String password = ctx.request().getFormAttribute("password");
        String firstname = ctx.request().getFormAttribute("firstname");
        String lastname = ctx.request().getFormAttribute("lastname");
        if (contains("", username, password, firstname, lastname)) {
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
                        .add("")
                        .add(firstname).add(lastname))
                        .setHandler(resultHandler(ctx, ar -> ctx.response()
                                .putHeader(LOCATION, securityConfig.getPac4jConfig()
                                        .getClients()
                                        .findClient(FormClient.class)
                                        .getCallbackUrl() + formAuthData(username, password))
                                .setStatusCode(FOUND)
                                .end()));
            } else {
                redirect(ctx, UI_FORM_REGISTER + userExists());
            }
        }));
    }

    private void handleUsersAll(RoutingContext ctx) {
        database.getAllUsers().setHandler(resultHandler(ctx, jsonResponse(ctx)));
    }

    private void handleViewsAll(RoutingContext ctx) {
        database.getAllViews().setHandler(resultHandler(ctx, jsonResponse(ctx)));
    }

    private String formAuthData(String username, String password) {
        return "&username=" + username + "&password=" + password;
    }

    private String userExists() {
        return "?" + USER_EXISTS + "=true";
    }
}
