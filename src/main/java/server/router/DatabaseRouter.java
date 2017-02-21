package server.router;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import server.security.FormClient;
import server.security.SecurityConfig;
import server.service.DatabaseService;

import static io.vertx.core.http.HttpHeaders.LOCATION;
import static server.entity.Status.*;
import static server.router.UiRouter.UI_FORM_REGISTER;
import static server.security.FormClient.*;
import static server.service.DatabaseService.DB_USERNAME;
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

    // TODO: 19.02.2017 hash password
    private void handleUsersInsert(RoutingContext ctx) {
        String username = ctx.request().getFormAttribute(FORM_USERNAME);
        String password = ctx.request().getFormAttribute(FORM_PASSWORD);
        String firstname = ctx.request().getFormAttribute(FORM_FIRSTNAME);
        String lastname = ctx.request().getFormAttribute(FORM_LASTNAME);
        if (contains("", username, password, firstname, lastname)) {
            serviceUnavailable(ctx, new Throwable("All fields must be filled!"));
            return;
        }
        database.getUser(username).setHandler(resultHandler(ctx, result -> {
            boolean exists = getRows(result).stream()
                    .map(obj -> (JsonObject) obj)
                    .anyMatch(json -> json.getString(DB_USERNAME).equals(username));
            if (!exists) {
                database.insertUser(username, password, firstname, lastname).setHandler(resultHandler(ctx, ar ->
                        ctx.response().putHeader(LOCATION, securityConfig.getPac4jConfig()
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
        //database.getAllViews().setHandler(resultHandler(ctx, jsonResponse(ctx)));
    }

    private String formAuthData(String username, String password) {
        return "&username=" + username + "&password=" + password;
    }

    private String userExists() {
        return "?" + USER_EXISTS + "=true";
    }
}
