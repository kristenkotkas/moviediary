package server.router;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import server.service.DatabaseService;
import server.service.DatabaseService.*;
import server.service.MailService;

import java.util.Map;

import static server.entity.Status.redirect;
import static server.entity.Status.serviceUnavailable;
import static server.router.MailRouter.userVerified;
import static server.router.UiRouter.UI_FORM_REGISTER;
import static server.router.UiRouter.UI_LOGIN;
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
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseRouter.class);
    public static final String DISPLAY_MESSAGE = "message";
    public static final String API_USERS_ALL = "/private/api/v1/users/all";
    public static final String API_VIEWS_COUNT = "/private/api/v1/views/count";
    public static final String API_USERS_FORM_INSERT = "/public/api/v1/users/form/insert";

    private final JsonObject config;
    private final DatabaseService database;
    private final MailService mail;

    public DatabaseRouter(Vertx vertx, JsonObject config, DatabaseService database, MailService mail) {
        super(vertx);
        this.config = config;
        this.database = database;
        this.mail = mail;
    }

    @Override
    public void route(Router router) {
        router.get(API_USERS_ALL).handler(this::handleUsersAll);
        router.get(API_VIEWS_COUNT).handler(this::handleUsersCount);
        router.post(API_USERS_FORM_INSERT).handler(this::handleUsersFormInsert);
    }

    /**
     * Returns current users count in database as String response.
     */
    private void handleUsersCount(RoutingContext ctx) {
        database.getUsersCount().setHandler(resultHandler(ctx, count -> ctx.response().end(count)));
    }

    /**
     * Inserts a form registered user to database.
     * If user with such username (email) already exists -> redirect to form register page with error message.
     * If we are running locally -> user is automatically verified.
     * If we are running on server -> user is sent a verification email.
     * When user is inserted into database, user is redirected to login page with appropriate message.
     */
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
                // TODO: 12/03/2017 insert demo views 
                CompositeFuture.all(future1, future2).setHandler(resultHandler(ctx, ar -> {
                    if (isServer(config)) {
                        mail.sendVerificationEmail(ctx, username);
                        redirect(ctx, UI_LOGIN + verifyEmail());
                    } else {
                        redirect(ctx, UI_LOGIN + userVerified());
                    }
                }));
            } else {
                redirect(ctx, UI_FORM_REGISTER + userExists());
            }
        }));
    }

    /**
     * Returns all users in database as JSON response.
     */
    private void handleUsersAll(RoutingContext ctx) {
        database.getAllUsers().setHandler(resultHandler(ctx, jsonResponse(ctx)));
    }

    private String userExists() {
        return "?" + DISPLAY_MESSAGE + "=" + "FORM_REGISTER_EXISTS";
    }

    private String verifyEmail() {
        return "?" + DISPLAY_MESSAGE + "=" + "FORM_REGISTER_VERIFY_EMAIL";
    }
}
