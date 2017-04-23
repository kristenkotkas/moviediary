package server.router;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.rxjava.core.CompositeFuture;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import server.entity.User;
import server.service.DatabaseService;
import server.service.DatabaseService.Column;
import server.service.DatabaseService.Table;
import server.service.MailService;

import java.util.function.BiFunction;

import static io.vertx.core.logging.LoggerFactory.getLogger;
import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.toList;
import static server.entity.Status.redirect;
import static server.entity.Status.serviceUnavailable;
import static server.router.MailRouter.userVerified;
import static server.router.UiRouter.UI_FORM_REGISTER;
import static server.router.UiRouter.UI_LOGIN;
import static server.security.FormClient.*;
import static server.service.DatabaseService.createDataMap;
import static server.service.DatabaseService.getRows;
import static server.util.CommonUtils.*;
import static server.util.HandlerUtils.resultHandler;
import static server.util.NetworkUtils.isServer;
import static server.util.StringUtils.*;

/**
 * Contains routes that interact with database.
 */
public class DatabaseRouter extends EventBusRoutable {
    public static final String DISPLAY_MESSAGE = "message";
    public static final String API_USER_INFO = "/private/api/v1/user/info";
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
    public static final String DATABASE_INSERT_EPISODE = "database_insert_episode";
    public static final String DATABASE_GET_SEEN_EPISODES = "database_get_seen_episodes";
    public static final String DATABASE_REMOVE_EPISODE = "database_remove_episode";
    public static final String DATABASE_GET_WATCHING_SERIES = "database_get_watching_series";
    public static final String DATABASE_REMOVE_WISHLIST = "database_remove_wishlist";
    private static final Logger LOG = getLogger(DatabaseRouter.class);
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
        listen(DATABASE_IS_IN_WISHLIST, reply((user, param) -> database.isInWishlist(user, parseInt(param))));
        listen(DATABASE_GET_WISHLIST, reply((user, param) -> database.getWishlist(user)));
        listen(DATABASE_INSERT_VIEW, reply(database::insertView));
        listen(DATABASE_GET_YEARS_DIST, reply(database::getYearsDist));
        listen(DATABASE_GET_WEEKDAYS_DIST, reply(database::getWeekdaysDist));
        listen(DATABASE_GET_TIME_DIST, reply(database::getTimeDist));
        listen(DATABASE_GET_ALL_TIME_META, reply(database::getAllTimeMeta));
        listen(DATABASE_GET_HISTORY_META, reply(database::getViewsMeta));
        listen(DATABASE_REMOVE_VIEW, reply(database::removeView));
        listen(DATABASE_INSERT_EPISODE, reply(database::insertEpisodeView));
        listen(DATABASE_REMOVE_EPISODE, reply(database::removeEpisode));
        listen(DATABASE_GET_SEEN_EPISODES, reply((user, param) -> database.getSeenEpisodes(user, parseInt(param)),
                getSeenEpisodes()));
        listen(DATABASE_GET_WATCHING_SERIES, reply((user, param) -> database.getWatchingSeries(user)));
        listen(DATABASE_REMOVE_WISHLIST, reply(database::removeFromWishlist));
    }

    @Override
    public void route(Router router) {
        router.get(API_USER_INFO).handler(this::handleUserInfo);
        router.get(API_USERS_COUNT).handler(this::handleUsersCount);
        router.post(API_USERS_FORM_INSERT).handler(this::handleUsersFormInsert);
    }

    private BiFunction<String, JsonObject, Object> getSeenEpisodes() {
        return (String user, JsonObject json) -> new JsonObject()
                .put("episodes", json.getJsonArray("rows").stream()
                        .map(obj -> (JsonObject) obj)
                        .map(j -> j.getInteger("EpisodeId"))
                        .collect(toList()));
    }

    /**
     * Based on username and JsonObject parameter -> returns database history results.
     */
    private BiFunction<String, JsonObject, Object> getDatabaseHistory() {
        return (user, json) -> {
            json.remove("results");
            json.getJsonArray("rows").stream()
                    .map(obj -> (JsonObject) obj)
                    .forEach(jsonObj -> jsonObj
                            .put("WasFirst", getFirstSeen(jsonObj.getBoolean("WasFirst")))
                            .put("WasCinema", getCinema(jsonObj.getBoolean("WasCinema")))
                            .put("DayOfWeek", getWeekdayFromDB(jsonObj.getString("Start")))
                            .put("Time", toNormalTime(jsonObj.getString("Start")))
                            .put("Start", getNormalDTFromDB(jsonObj.getString("Start"), LONG_DATE)));
            return json;
        };
    }

    /**
     * Based on username and JsonObject parameter -> returns database movie history results.
     */
    private BiFunction<String, JsonObject, Object> getDatabaseMovieHistory() {
        return (user, json) -> {
            json.remove("results");
            json.getJsonArray("rows").stream()
                    .map(obj -> (JsonObject) obj)
                    .forEach(jsonObj -> jsonObj
                            .put("WasCinema", getCinema(jsonObj.getBoolean("WasCinema")))
                            .put("Start", getNormalDTFromDB(jsonObj.getString("Start"), LONG_DATE)));
            return json;
        };
    }

    /**
     * Returns current users count in database as String response.
     */
    private void handleUsersCount(RoutingContext ctx) {
        database.getUsersCount()
                .rxSetHandler()
                .doOnError(err -> serviceUnavailable(ctx, err))
                .subscribe(count -> ctx.response().end(count));
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
        database.getUser(username).setHandler(resultHandler(ctx, result -> check(getRows(result).stream()
                .map(obj -> (JsonObject) obj)
                .noneMatch(json -> json.getString(Column.USERNAME.getName()).equals(username)), () -> {
            String salt = genString();
            Future<JsonObject> f1 = database.insert(Table.USERS, mapBuilder(createDataMap(username))
                    .put(Column.FIRSTNAME, firstname)
                    .put(Column.LASTNAME, lastname)
                    .put(Column.PASSWORD, hash(password, salt))
                    .put(Column.SALT, salt)
                    .build());
            Future<JsonObject> f2 = database.insert(Table.SETTINGS, mapBuilder(createDataMap(username))
                    .put(Column.VERIFIED, isServer(config) ? "0" : "1")
                    .build());
            CompositeFuture.all(f1, f2).setHandler(resultHandler(ctx, ar -> check(isServer(config), () -> {
                mail.sendVerificationEmail(ctx, username);
                redirect(ctx, verifyEmail());
            }, () -> redirect(ctx, userVerified()))));
        }, () -> redirect(ctx, userExists()))));
    }

    /**
     * Returns all users in database as JSON response.
     */
    private void handleUserInfo(RoutingContext ctx) {
        database.getUser(getProfile(ctx).getEmail()).setHandler(resultHandler(ctx,
                json -> ctx.response().setStatusCode(200).end(toXml(new User(getRows(json).getJsonObject(0))))));
    }

    private String userExists() {
        return UI_FORM_REGISTER + "?" + DISPLAY_MESSAGE + "=" + "FORM_REGISTER_EXISTS";
    }

    private String verifyEmail() {
        return UI_LOGIN + "?" + DISPLAY_MESSAGE + "=" + "FORM_REGISTER_VERIFY_EMAIL";
    }
}
