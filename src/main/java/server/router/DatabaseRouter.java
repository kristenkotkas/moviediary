package server.router;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import server.security.SecurityConfig;
import server.service.DatabaseService;
import server.service.DatabaseService.*;
import server.service.MailService;

import java.util.Map;

import static server.entity.Status.redirect;
import static server.entity.Status.serviceUnavailable;
import static server.router.UiRouter.UI_FORM_REGISTER;
import static server.router.UiRouter.UI_LOGIN;
import static server.security.DatabaseAuthorizer.ERROR;
import static server.security.FormClient.*;
import static server.service.DatabaseService.*;
import static server.util.CommonUtils.contains;
import static server.util.HandlerUtils.jsonResponse;
import static server.util.HandlerUtils.resultHandler;
import static server.util.NetworkUtils.isServer;
import static server.util.StringUtils.genString;
import static server.util.StringUtils.hash;

/**
 * Contains routes that interact with database.
 */
public class DatabaseRouter extends Routable {
    public static final String USER_EXISTS = "userExists";
    public static final String API_USERS_ALL = "/private/api/users/all";
    public static final String API_USERS_FORM_INSERT = "/public/api/users/form/insert";
    private static final Logger log = LoggerFactory.getLogger(DatabaseRouter.class);
    private final JsonObject config;
    private final DatabaseService database;
    private final MailService mail;
    private final SecurityConfig securityConfig;

    public DatabaseRouter(Vertx vertx, JsonObject config, DatabaseService database, MailService mail,
                          SecurityConfig securityConfig) {
        super(vertx);
        this.config = config;
        this.database = database;
        this.mail = mail;
        this.securityConfig = securityConfig;
    }

    @Override
    public void route(Router router) {
        router.get(API_USERS_ALL).handler(this::handleUsersAll);
        router.post(API_USERS_FORM_INSERT).handler(this::handleUsersFormInsert);
    }

    private void handleUsersFormInsert(RoutingContext ctx) {
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
                Map<Column, String> userMap = createDataMap(username);
                Map<Column, String> settingsMap = createDataMap(username);
                String salt = genString();
                userMap.put(Column.FIRSTNAME, firstname);
                userMap.put(Column.LASTNAME, lastname);
                userMap.put(Column.PASSWORD, hash(password, salt));
                userMap.put(Column.SALT, salt);
                settingsMap.put(Column.VERIFIED, isServer(config) ? "0" : "1");
                Future<JsonObject> future1 = database.insert(Table.USERS, userMap);
                Future<JsonObject> future2 = database.insert(Table.SETTINGS, settingsMap);
                CompositeFuture.all(future1, future2).setHandler(resultHandler(ctx, ar -> {
                    if (isServer(config)) {
                        mail.sendVerificationEmail(username);
                    }
                    redirect(ctx, UI_LOGIN + verifyEmail());
                }));
            } else {
                redirect(ctx, UI_FORM_REGISTER + userExists());
            }
        }));
    }

    private void handleUsersAll(RoutingContext ctx) {
        database.getAllUsers().setHandler(resultHandler(ctx, jsonResponse(ctx)));
    }

    private String formAuthData(String username, String password) {
        return "&username=" + username + "&password=" + password;
    }

    private String userExists() {
        return "?" + USER_EXISTS + "=true";
    }

    private String verifyEmail() {
        return "?" + ERROR + "=Please verify your email.";
    }
}
