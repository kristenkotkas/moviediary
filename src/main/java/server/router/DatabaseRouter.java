package server.router;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import server.entity.User;
import server.service.DatabaseService;
import server.service.DatabaseService.*;
import server.service.MailService;

import java.util.Map;
import java.util.function.BiFunction;

import static io.vertx.core.CompositeFuture.all;
import static java.lang.Integer.parseInt;
import static server.entity.Status.redirect;
import static server.entity.Status.serviceUnavailable;
import static server.router.MailRouter.userVerified;
import static server.router.UiRouter.UI_FORM_REGISTER;
import static server.router.UiRouter.UI_LOGIN;
import static server.security.FormClient.*;
import static server.service.DatabaseService.*;
import static server.util.CommonUtils.contains;
import static server.util.CommonUtils.getProfile;
import static server.util.HandlerUtils.resultHandler;
import static server.util.NetworkUtils.isServer;
import static server.util.StringUtils.*;

/**
 * Contains routes that interact with database.
 */
public class DatabaseRouter extends EventBusRoutable {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseRouter.class);
    public static final String DISPLAY_MESSAGE = "message";
    public static final String API_USER_INFO = "/private/api/v1/user";
    public static final String API_USERS_COUNT = "/private/api/v1/views/count";
    public static final String API_USERS_FORM_INSERT = "/public/api/v1/users/form/insert";

    public static final String DATABASE_USERS = "database_users";
    public static final String DATABASE_USERS_SIZE = "database_users_size";
    public static final String DATABASE_GET_HISTORY = "database_get_history";
    public static final String DATABASE_GET_MOVIE_HISTORY = "database_get_movie_history";
    public static final String DATABASE_INSERT_WISHLIST = "database_insert_wishlist";
    public static final String DATABASE_IS_IN_WISHLIST = "database_get_in_wishlist";
    public static final String DATABASE_GET_WISHLIST = "database_get_wishlist";
    public static final String DATABASE_INSERT_VIEW = "database_insert_view";
    public static final String DATABASE_GET_YEARS_DIST = "database_get_years_dist";
    public static final String DATABASE_GET_WEEKDAYS_DIST = "database_get_weekdays_dist";
    public static final String DATABASE_GET_TIME_DIST = "database_get_time_dist";
    public static final String DATABASE_GET_ALL_TIME_META = "database_get_all_time_meta";
    public static final String DATABASE_GET_HISTORY_META = "database_get_history_meta";
    public static final String DATABASE_REMOVE_VIEW = "database_remove_view";

    private final JsonObject config;
    private final DatabaseService database;
    private final MailService mail;

    public DatabaseRouter(Vertx vertx, JsonObject config, DatabaseService database, MailService mail) {
        super(vertx);
        this.config = config;
        this.database = database;
        this.mail = mail;
        listen(DATABASE_USERS, reply(param -> database.getAllUsers()));
        listen(DATABASE_USERS_SIZE, reply((user, param) -> database.getAllUsers(), (user, json) -> json.size()));
        listen(DATABASE_GET_HISTORY, reply((username, param) -> database.getViews(username, param,
                new JsonObject(param).getInteger("page")), getDatabaseHistory()));
        listen(DATABASE_GET_MOVIE_HISTORY, reply(database::getMovieViews, getDatabaseMovieHistory()));
        listen(DATABASE_INSERT_WISHLIST, (user, param) -> database.insertWishlist(user, parseInt(param)));
        listen(DATABASE_IS_IN_WISHLIST, reply((user, param) -> database.isInWishlist(user, parseInt(param)),
                (user, json) -> json));
        listen(DATABASE_GET_WISHLIST, reply((user, param) -> database.getWishlist(user), (user, json) -> json));
        listen(DATABASE_INSERT_VIEW, reply(database::insertView, (user, json) -> json));
        listen(DATABASE_GET_YEARS_DIST, reply(database::getYearsDist, (user, json) -> json));
        listen(DATABASE_GET_WEEKDAYS_DIST, reply(database::getWeekdaysDist, (user, json) -> json));
        listen(DATABASE_GET_TIME_DIST, reply(database::getTimeDist, (user, json) -> json));
        listen(DATABASE_GET_ALL_TIME_META, reply(database::getAllTimeMeta, (user, json) -> json));
        listen(DATABASE_GET_HISTORY_META, reply(database::getViewsMeta, (user, json) -> json));
        listen(DATABASE_REMOVE_VIEW, reply(database::removeView, (user, json) -> json));
    }

    @Override
    public void route(Router router) {
        router.get(API_USER_INFO).handler(this::handleUserInfo);
        router.get(API_USERS_COUNT).handler(this::handleUsersCount);
        router.post(API_USERS_FORM_INSERT).handler(this::handleUsersFormInsert);
    }

    /**
     * Based on username and JsonObject parameter -> returns database history results.
     */
    private BiFunction<String, JsonObject, Object> getDatabaseHistory() {
        return (user, json) -> {
            json.remove("results");
            JsonArray array = json.getJsonArray("rows");
            for (int i = 0; i < array.size(); i++) {
                JsonObject jsonObject = array.getJsonObject(i);
                jsonObject.put("WasFirst", getFirstSeen(jsonObject.getBoolean("WasFirst")));
                jsonObject.put("WasCinema", getCinema(jsonObject.getBoolean("WasCinema")));
                jsonObject.put("DayOfWeek", getWeekdayFromDB(jsonObject.getString("Start")));
                jsonObject.put("Time", toNormalTime(jsonObject.getString("Start")));
                jsonObject.put("Start", getNormalDTFromDB(jsonObject.getString("Start"), LONG_DATE));
            }
            return json;
        };
    }

    /**
     * Based on username and JsonObject parameter -> returns database movie history results.
     */
    private BiFunction<String, JsonObject, Object> getDatabaseMovieHistory() {
        return (user, json) -> {
            json.remove("results");
            JsonArray array = json.getJsonArray("rows");
            for (int i = 0; i < array.size(); i++) {
                JsonObject jsonObject = array.getJsonObject(i);
                jsonObject.put("WasCinema", getCinema(jsonObject.getBoolean("WasCinema")));
                jsonObject.put("Start", getNormalDTFromDB(jsonObject.getString("Start"), LONG_DATE));
            }
            return json;
        };
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
                Future<JsonObject> f1 = database.insert(Table.USERS, userMap);
                Future<JsonObject> f2 = database.insert(Table.SETTINGS, settingsMap);
                all(f1, f2).setHandler(resultHandler(ctx, ar -> {
                    if (isServer(config)) {
                        mail.sendVerificationEmail(ctx, username);
                        redirect(ctx, verifyEmail());
                    } else {
                        redirect(ctx, userVerified());
                    }
                }));
            } else {
                redirect(ctx, userExists());
            }
        }));
    }

    /**
     * Returns all users in database as JSON response.
     */
    private void handleUserInfo(RoutingContext ctx) {
        database.getUser(getProfile(ctx).getEmail()).setHandler(resultHandler(ctx, json -> {
            JsonObject user = getRows(json).getJsonObject(0);
            ctx.response().setStatusCode(200).end(toXml(new User(
                    user.getInteger("Id"),
                    user.getString("Firstname"),
                    user.getString("Lastname"),
                    user.getString("Username"),
                    user.getString("RuntimeType"),
                    user.getString("Verified").equals("1"))));
        }));
    }

    private String userExists() {
        return UI_FORM_REGISTER + "?" + DISPLAY_MESSAGE + "=" + "FORM_REGISTER_EXISTS";
    }

    private String verifyEmail() {
        return UI_LOGIN + "?" + DISPLAY_MESSAGE + "=" + "FORM_REGISTER_VERIFY_EMAIL";
    }
}
