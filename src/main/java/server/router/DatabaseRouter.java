package server.router;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.CompositeFuture;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import server.entity.JsonObj;
import server.entity.User;
import server.security.SecurityConfig;
import server.service.DatabaseService;
import server.service.DatabaseService.Column;
import server.service.DatabaseService.Table;
import server.service.MailService;

import java.util.function.BiFunction;

import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.toList;
import static server.entity.Status.redirect;
import static server.entity.Status.serviceUnavailable;
import static server.router.UiRouter.UI_FORM_REGISTER;
import static server.router.UiRouter.UI_LOGIN;
import static server.security.FormClient.*;
import static server.security.SecurityConfig.CSRF_TOKEN;
import static server.service.DatabaseService.createDataMap;
import static server.service.DatabaseService.getRows;
import static server.util.CommonUtils.*;
import static server.util.HandlerUtils.jsonResponse;
import static server.util.HandlerUtils.resultHandler;
import static server.util.StringUtils.*;

/**
 * Contains routes that interact with database.
 */
public class DatabaseRouter extends EventBusRoutable {
    public static final String DISPLAY_MESSAGE = "message";
    public static final String API_USERS_FORM_INSERT = "/public/api/v1/users/form/insert";

    private static final String API_USER_INFO = "/private/api/v1/user/info";
    public static final String API_HISTORY = "/private/api/v1/history";

    private static final String GET_HISTORY = "database_get_history";
    private static final String GET_MOVIE_HISTORY = "database_get_movie_history";
    private static final String INSERT_VIEW = "database_insert_view";
    private static final String GET_YEARS_DIST = "database_get_years_dist";
    private static final String GET_WEEKDAYS_DIST = "database_get_weekdays_dist";
    private static final String GET_TIME_DIST = "database_get_time_dist";
    private static final String GET_ALL_TIME_META = "database_get_all_time_meta";
    private static final String GET_HISTORY_META = "database_get_history_meta";
    private static final String REMOVE_VIEW = "database_remove_view";
    private static final String INSERT_EPISODE = "database_insert_episode";
    private static final String GET_SEEN_EPISODES = "database_get_seen_episodes";
    private static final String REMOVE_EPISODE = "database_remove_episode";
    private static final String GET_WATCHING_SERIES = "database_get_watching_series";
    private static final String GET_LAST_VIEWS = "database_get_last_views";
    private static final String GET_TOP_MOVIES = "database_get_top_movies";
    private static final String GET_TOTAL_MOVIE_COUNT = "database_get_total_movie_count";
    private static final String GET_NEW_MOVIE_COUNT = "database_get_new_movie_count";
    private static final String GET_TOTAL_RUNTIME = "database_get_total_runtime";
    private static final String GET_DISTINCT_MOVIE_COUNT = "database_get_distinct_movie_count";
    private static final String GET_TOTAL_CINEMA_COUNT = "database_get_total_cinema_count";
    private static final String GET_TOP_MOVIES_STAT = "database_get_top_movies_stat";
    private static final String GET_MONTH_YEAR_DISTRIBUTION = "database_get_month_year_distribution";
    private static final String REMOVE_SEASON_VIEWS = "database_remove_season_views";
    private static final String INSERT_NEW_LIST = "database_insert_new_list";
    private static final String GET_LISTS = "database_get_lists";
    private static final String INSERT_INTO_LISTS = "database_insert_into_lists";
    private static final String REMOVE_FROM_LIST = "database_remove_from_list";
    private static final String GET_IN_LIST = "database_get_in_list";
    private static final String GET_LIST_ENTRIES = "database_get_list_entries";
    private static final String CHANGE_LIST_NAME = "database_change_list_name";
    private static final String DELETE_LIST = "database_delete_list";
    private static final String GET_LIST_SEEN_MOVIES = "database_get_list_seen_movies";
    private static final String GET_LIST_NAME = "database_get_list_name";
    private static final String GET_LAST_LISTS_HOME = "database_get_last_lists_home";
    private static final String GET_DELETED_LISTS = "database_get_deleted_lists";
    private static final String RESTORE_DELETED_LIST = "database_restore_deleted_list";
    private static final String GET_LISTS_SIZE = "database_get_lists_size";
    private static final String INSERT_USER_SERIES_INFO = "database_insert_user_series_info";
    private static final String CHANGE_SERIES_INACTIVE = "database_change_series_inactive";
    private static final String GET_INACTIVE_SERIES = "database_get_inactive_series";
    private static final String CHANGE_SERIES_ACTIVE = "database_change_series_active";
    private static final String GET_TODAY_IN_HISTORY = "database_get_today_in_history";
    private static final String GET_HOME_STATISTICS = "database_get_home_statistics";
    private static final String GET_OSCAR_AWARDS = "database_get_oscar_awards";

    private final JsonObject config;
    private final SecurityConfig securityConfig;
    private final DatabaseService database;
    private final MailService mail;

    public DatabaseRouter(Vertx vertx, JsonObject config, SecurityConfig securityConfig,
                          DatabaseService database, MailService mail) {
        super(vertx);
        this.config = config;
        this.securityConfig = securityConfig;
        this.database = database;
        this.mail = mail;
        listen(GET_HISTORY, reply(database::getViews, transformDatabaseHistory()));
        listen(GET_MOVIE_HISTORY, reply(database::getMovieViews, getDatabaseMovieHistory()));
        listen(INSERT_VIEW, reply(database::insertView));
        listen(GET_YEARS_DIST, reply(database::getYearsDistribution));
        listen(GET_WEEKDAYS_DIST, reply(database::getWeekdaysDistribution));
        listen(GET_TIME_DIST, reply(database::getTimeDistribution));
        listen(GET_MONTH_YEAR_DISTRIBUTION,
                reply(database::getMonthYearDistribution, transformMonthYearDistribution()));
        listen(GET_ALL_TIME_META, reply(database::getAllTimeMeta));
        listen(GET_HISTORY_META, reply(database::getViewsMeta));
        listen(REMOVE_VIEW, reply(database::removeView));
        listen(INSERT_EPISODE, reply(database::insertEpisodeView));
        listen(REMOVE_EPISODE, reply(database::removeEpisode));
        listen(GET_SEEN_EPISODES,
                reply((user, param) -> database.getSeenEpisodes(user, parseInt(param)), getSeenEpisodes()));
        listen(GET_WATCHING_SERIES, reply((user, param) -> database.getWatchingSeries(user)));
        listen(GET_LAST_VIEWS, reply((user, param) -> database.getLastMoviesHome(user), getDatabaseHomeViews()));
        listen(GET_TOP_MOVIES, reply((user, param) -> database.getTopMoviesHome(user)));
        listen(GET_TOTAL_MOVIE_COUNT, reply((user, param) -> database.getTotalMovieCount(user)));
        listen(GET_NEW_MOVIE_COUNT, reply((user, param) -> database.getNewMovieCount(user)));
        listen(GET_TOTAL_RUNTIME, reply((user, param) -> database.getTotalRuntime(user)));
        listen(GET_TOTAL_CINEMA_COUNT, reply((user, param) -> database.getTotalCinemaCount(user)));
        listen(GET_DISTINCT_MOVIE_COUNT, reply((user, param) -> database.getTotalDistinctMoviesCount(user)));
        listen(GET_TOP_MOVIES_STAT, reply(database::getTopMoviesStat));
        listen(REMOVE_SEASON_VIEWS, reply(database::removeSeasonViews));
        listen(INSERT_NEW_LIST, reply(database::insertList));
        listen(GET_LISTS, reply((user, param) -> database.getLists(user)));
        listen(INSERT_INTO_LISTS, reply(database::insertIntoList));
        listen(REMOVE_FROM_LIST, reply(database::removeFromList));
        listen(GET_IN_LIST, reply(database::getInLists));
        listen(GET_LIST_ENTRIES, reply(database::getListEntries));
        listen(CHANGE_LIST_NAME, reply(database::changeListName));
        listen(DELETE_LIST, reply(database::deleteList));
        listen(GET_LIST_SEEN_MOVIES, reply(database::getListSeenMovies));
        listen(GET_LIST_NAME, reply(database::getListName));
        listen(GET_LAST_LISTS_HOME, reply((user, param) -> database.getLastListsHome(user)));
        listen(GET_DELETED_LISTS, reply((user, param) -> database.getDeletedLists(user)));
        listen(RESTORE_DELETED_LIST, reply(database::restoreDeletedList));
        listen(GET_LISTS_SIZE, reply((user, param) -> database.getListsSize(user)));
        listen(INSERT_USER_SERIES_INFO, reply(database::insertUserSeriesInfo));
        listen(CHANGE_SERIES_INACTIVE, reply(database::changeSeriesToInactive));
        listen(GET_INACTIVE_SERIES, reply((user, param) -> database.getInactiveSeries(user)));
        listen(CHANGE_SERIES_ACTIVE, reply(database::changeSeriesToActive));
        listen(GET_TODAY_IN_HISTORY, reply((user, param) -> database.getTodayInHistory(user)));
        listen(GET_HOME_STATISTICS, reply((user, param) -> database.getHomeStatistics(user)));
        listen(GET_OSCAR_AWARDS, reply((user, param) -> database.getOscarAwards(param)));
    }

    @Override
    public void route(Router router) {
        router.get(API_USER_INFO).handler(this::handleUserInfo);
        router.post(API_USERS_FORM_INSERT).handler(this::handleUsersFormInsert);
        router.get(API_HISTORY).handler(this::handleGetHistory);
    }

    private void handleGetHistory(RoutingContext ctx) { // TODO: 07/05/2017 test
        if (ctx.getBody().length() == 0) {
            serviceUnavailable(ctx, new Throwable("Missing parameters for query."));
            return;
        }
        String username = getUsername(ctx, securityConfig);
        database.getViews(username, ctx.getBodyAsString()).rxSetHandler()
                .doOnError(err -> serviceUnavailable(ctx, err))
                .subscribe(j -> jsonResponse(ctx).accept(transformDatabaseHistory().apply(username, j)));
    }

    private BiFunction<String, JsonObject, Object> getSeenEpisodes() {
        return (String user, JsonObject json) -> new JsonObject()
                .put("episodes", json.getJsonArray("rows").stream()
                        .map(obj -> (JsonObject) obj)
                        .map(j -> j.getInteger("EpisodeId"))
                        .collect(toList())); // TODO: 19.05.2017 should collect to jsonArray instead?
    }

    /**
     * Transforms database history results for easier consuming in frontend.
     */
    private BiFunction<String, JsonObject, Object> transformDatabaseHistory() {
        return (user, json) -> {
            json.remove("results");
            //System.out.println(json.encodePrettily());
            json.getJsonArray("rows").stream()
                    .map(JsonObj::fromParent)
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

    private BiFunction<String, JsonObject, Object> getDatabaseHomeViews() {
        return (user, json) -> {
            json.remove("results");
            json.getJsonArray("rows").stream()
                    .map(obj -> (JsonObject) obj)
                    .forEach(jsonObj -> jsonObj
                            .put("Start", getNormalDTFromDB(jsonObj.getString("Start"), LONG_DATE)));
            return json;
        };
    }

    private BiFunction<String, JsonObject, Object> transformMonthYearDistribution() {
        return (user, json) -> {
            JsonObj result = new JsonObj();
            getRows(json).stream()
                    .map(JsonObj::fromParent)
                    .peek(j -> result.putIfAbsent(j.getString("Year"), new JsonObj()))
                    .forEach(j -> result.getJsonObject(j.getString("Year"))
                            .put(j.getString("Month"), j.getInteger("Count")));
            return result;
        };
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
        String csrfToken = ctx.removeCookie(CSRF_TOKEN).getValue();
        String sessionCsrfToken = ctx.session().remove(CSRF_TOKEN);
        if (!nonNull(username, password, firstname, lastname) ||
                contains("", username, password, firstname, lastname)) {
            serviceUnavailable(ctx, new Throwable("All fields must be filled!"));
            return;
        }
        if (!nonNull(csrfToken, sessionCsrfToken) || !csrfToken.equals(sessionCsrfToken)) {
            serviceUnavailable(ctx, new Throwable("Csrf check failed."));
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
                    .put(Column.VERIFIED, "0")
                    .build());
            CompositeFuture.all(f1, f2).setHandler(resultHandler(ctx, ar -> {
                mail.sendVerificationEmail(ctx, username);
                redirect(ctx, verifyEmail());
            }));
        }, () -> redirect(ctx, userExists()))));
    }

    /**
     * Returns user info as XML response.
     */
    private void handleUserInfo(RoutingContext ctx) {
        database.getUser(getUsername(ctx, securityConfig)).rxSetHandler()
                .doOnError(err -> serviceUnavailable(ctx, err))
                .subscribe(json -> ctx.response()
                        .setStatusCode(200)
                        .end(toXml(new User(getRows(json).getJsonObject(0)))));
    }

    private String userExists() {
        return UI_FORM_REGISTER + "?" + DISPLAY_MESSAGE + "=" + "FORM_REGISTER_EXISTS";
    }

    private String verifyEmail() {
        return UI_LOGIN + "?" + DISPLAY_MESSAGE + "=" + "FORM_REGISTER_VERIFY_EMAIL";
    }
}
