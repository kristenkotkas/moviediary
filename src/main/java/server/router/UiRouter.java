package server.router;

import eu.kyngas.template.engine.HandlebarsTemplateEngine;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import org.pac4j.core.profile.CommonProfile;
import server.security.FormClient;
import server.security.IdCardClient;
import server.security.SecurityConfig;
import server.service.DatabaseService;
import server.service.DatabaseService.Column;
import server.service.DatabaseService.Table;
import server.template.ui.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.pac4j.core.util.CommonHelper.addParameter;
import static server.router.AuthRouter.AUTH_LOGOUT;
import static server.router.AuthRouter.LANGUAGE;
import static server.router.DatabaseRouter.API_USERS_FORM_INSERT;
import static server.router.DatabaseRouter.DISPLAY_MESSAGE;
import static server.security.DatabaseAuthorizer.URL;
import static server.security.SecurityConfig.AuthClient.*;
import static server.security.SecurityConfig.CLIENT_CERTIFICATE;
import static server.security.SecurityConfig.CLIENT_VERIFIED_STATE;
import static server.service.DatabaseService.createDataMap;
import static server.util.CommonUtils.getProfile;
import static server.util.FileUtils.isRunningFromJar;

/**
 * Contains routes which user interface is handled on.
 */
public class UiRouter extends Routable {
    private static final Logger log = LoggerFactory.getLogger(UiRouter.class);
    private static final Path RESOURCES = Paths.get("src/main/resources");
    private static final String STATIC_PATH = "/static/*";
    private static final String STATIC_FOLDER = "static";

    public static final String UI_INDEX = "/";
    public static final String UI_USER = "/private/user";
    public static final String UI_HOME = "/private/home";
    public static final String UI_MOVIES = "/private/movies";
    public static final String UI_HISTORY = "/private/history";
    public static final String UI_STATISTICS = "/private/statistics";
    public static final String UI_WISHLIST = "/private/wishlist";
    public static final String UI_LOGIN = "/login";
    public static final String UI_FORM_LOGIN = "/formlogin";
    public static final String UI_FORM_REGISTER = "/formregister";
    public static final String UI_IDCARDLOGIN = "/idcardlogin";

    private static final String TEMPL_USER = "templates/user.hbs";
    private static final String TEMPL_HOME = "templates/home.hbs";
    private static final String TEMPL_MOVIES = "templates/movies.hbs";
    private static final String TEMPL_HISTORY = "templates/history.hbs";
    private static final String TEMPL_STATISTICS = "templates/statistics.hbs";
    private static final String TEMPL_WISHLIST = "templates/wishlist.hbs";
    private static final String TEMPL_LOGIN = "templates/login.hbs";
    private static final String TEMPL_FORM_LOGIN = "templates/formlogin.hbs";
    private static final String TEMPL_FORM_REGISTER = "templates/formregister.hbs";
    private static final String TEMPL_IDCARDLOGIN = "templates/idcardlogin.hbs";
    private static final String TEMPL_NOTFOUND = "templates/notfound.hbs";

    private final HandlebarsTemplateEngine engine;
    private final SecurityConfig securityConfig;
    private final DatabaseService database;

    public UiRouter(Vertx vertx, SecurityConfig securityConfig, DatabaseService database) throws Exception {
        super(vertx);
        this.engine = HandlebarsTemplateEngine.create(isRunningFromJar() ? null : RESOURCES);
        this.securityConfig = securityConfig;
        this.database = database;
    }

    public static Handler<AsyncResult<Buffer>> endHandler(RoutingContext ctx) {
        return ar -> {
            if (ar.succeeded()) {
                ctx.response().end(ar.result());
            } else {
                ctx.fail(ar.cause());
            }
        };
    }

    @Override
    public void route(Router router) {
        router.get(UI_INDEX).handler(this::handleLogin);
        router.get(UI_HOME).handler(this::handleHome);
        router.get(UI_LOGIN).handler(this::handleLogin);
        router.get(UI_FORM_LOGIN).handler(this::handleFormLogin);
        router.get(UI_FORM_REGISTER).handler(this::handleFormRegister);
        router.get(UI_IDCARDLOGIN).handler(this::handleIdCardLogin);

        router.get(UI_USER).handler(this::handleUser);
        router.get(UI_MOVIES).handler(this::handleMovies);
        router.get(UI_HISTORY).handler(this::handleHistory);
        router.get(UI_STATISTICS).handler(this::handleStatistics);
        router.get(UI_WISHLIST).handler(this::handleWishlist);

        router.route(STATIC_PATH).handler(StaticHandler.create(isRunningFromJar() ?
                STATIC_FOLDER : RESOURCES.resolve(STATIC_FOLDER).toString())
                .setCachingEnabled(true)
                .setIncludeHidden(false));
        router.route().last().handler(this::handleNotFound);
    }

    private void handleUser(RoutingContext ctx) {
        UserTemplate template = getSafe(ctx, TEMPL_USER, UserTemplate.class);
        String lang = ctx.request().getParam(LANGUAGE);
        if (lang != null) {
            Map<Column, String> map = createDataMap(getProfile(ctx).getEmail());
            map.put(Column.LANGUAGE, lang);
            database.update(Table.SETTINGS, map);
            ctx.session().data().put(LANGUAGE, lang);
            template.setLang(lang);
        }
        engine.render(template, endHandler(ctx));
    }

    private void handleHome(RoutingContext ctx) {
        engine.render(getSafe(ctx, TEMPL_HOME, HomeTemplate.class), endHandler(ctx));
    }

    private void handleMovies(RoutingContext ctx) {
        engine.render(getSafe(ctx, TEMPL_MOVIES, MoviesTemplate.class), endHandler(ctx));
    }

    private void handleHistory(RoutingContext ctx) {
        engine.render(getSafe(ctx, TEMPL_HISTORY, HistoryTemplate.class), endHandler(ctx));
    }

    private void handleStatistics(RoutingContext ctx) {
        engine.render(getSafe(ctx, TEMPL_STATISTICS, StatisticsTemplate.class), endHandler(ctx));
    }

    private void handleWishlist(RoutingContext ctx) {
        engine.render(getSafe(ctx, TEMPL_WISHLIST, WhislistTemplate.class), endHandler(ctx));
    }

    private void handleLogin(RoutingContext ctx) {
        engine.render(getSafe(ctx, TEMPL_LOGIN, LoginTemplate.class)
                .setDisplayMessage(ctx.request().getParam(DISPLAY_MESSAGE))
                .setFormUrl(UI_HOME + FORM.getClientNamePrefixed())
                .setFacebookUrl(UI_HOME + FACEBOOK.getClientNamePrefixed())
                .setGoogleUrl(UI_HOME + GOOGLE.getClientNamePrefixed())
                .setIdCardUrl(UI_HOME + IDCARD.getClientNamePrefixed()), endHandler(ctx));
    }

    private void handleFormLogin(RoutingContext ctx) {
        engine.render(getSafe(ctx, TEMPL_FORM_LOGIN, FormLoginTemplate.class)
                .setRegisterUrl(UI_FORM_REGISTER)
                .setCallbackUrl(securityConfig.getPac4jConfig()
                        .getClients()
                        .findClient(FormClient.class)
                        .getCallbackUrl()), endHandler(ctx));
    }

    private void handleFormRegister(RoutingContext ctx) {
        engine.render(getSafe(ctx, TEMPL_FORM_REGISTER, FormRegisterTemplate.class)
                .setDisplayMessage(ctx.request().getParam(DISPLAY_MESSAGE))
                .setRegisterRestUrl(API_USERS_FORM_INSERT), endHandler(ctx));
    }

    private void handleIdCardLogin(RoutingContext ctx) {
        engine.render(getSafe(ctx, TEMPL_IDCARDLOGIN, IdCardLoginTemplate.class)
                .setClientVerifiedHeader(CLIENT_VERIFIED_STATE)
                .setClientCertificateHeader(CLIENT_CERTIFICATE)
                .setClientVerifiedState(ctx.request().getHeader(CLIENT_VERIFIED_STATE))
                .setClientCertificate(ctx.request().getHeader(CLIENT_CERTIFICATE))
                .setCallbackUrl(securityConfig.getPac4jConfig()
                        .getClients()
                        .findClient(IdCardClient.class)
                        .getCallbackUrl()), endHandler(ctx));
    }

    private void handleNotFound(RoutingContext ctx) {
        ctx.response().setStatusCode(404);
        engine.render(getSafe(ctx, TEMPL_NOTFOUND, NotFoundTemplate.class), endHandler(ctx));
    }

    private <S extends BaseTemplate> S getSafe(RoutingContext ctx, String fileName, Class<S> type) {
        S baseTemplate = engine.getSafeTemplate(ctx, fileName, type);
        baseTemplate.setLang((String) ctx.session().data().get(LANGUAGE));
        baseTemplate.setLogoutUrl(addParameter(AUTH_LOGOUT, URL, UI_LOGIN));
        baseTemplate.setUserPage(UI_USER);
        baseTemplate.setHomePage(UI_HOME);
        baseTemplate.setMoviesPage(UI_MOVIES);
        baseTemplate.setHistoryPage(UI_HISTORY);
        baseTemplate.setStatisticsPage(UI_STATISTICS);
        baseTemplate.setWishlistPage(UI_WISHLIST);
        CommonProfile profile = getProfile(ctx);
        if (profile != null) {
            baseTemplate.setUserName(profile.getFirstName() + " " + profile.getFamilyName());
            baseTemplate.setUserFirstName(profile.getFirstName());
        }
        return baseTemplate;
    }
}