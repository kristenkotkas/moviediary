package server.router;

import eu.kyngas.template.engine.HandlebarsTemplateEngine;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.StaticHandler;
import org.pac4j.core.profile.CommonProfile;
import server.security.FormClient;
import server.security.IdCardClient;
import server.security.SecurityConfig;
import server.template.ui.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static io.vertx.rxjava.ext.web.Cookie.cookie;
import static java.util.concurrent.TimeUnit.DAYS;
import static org.pac4j.core.util.CommonHelper.addParameter;
import static server.entity.Language.*;
import static server.entity.Status.redirect;
import static server.router.AuthRouter.AUTH_LOGOUT;
import static server.router.BankLinkRouter.*;
import static server.router.DatabaseRouter.API_USERS_FORM_INSERT;
import static server.router.DatabaseRouter.DISPLAY_MESSAGE;
import static server.security.DatabaseAuthorizer.UNAUTHORIZED;
import static server.security.DatabaseAuthorizer.URL;
import static server.security.RedirectClient.REDIRECT_URL;
import static server.security.SecurityConfig.AuthClient.*;
import static server.security.SecurityConfig.*;
import static server.util.CommonUtils.check;
import static server.util.CommonUtils.getProfile;
import static server.util.FileUtils.isRunningFromJar;
import static server.util.HandlerUtils.endHandler;
import static server.util.StringUtils.RANDOM;

/**
 * Contains routes which user interface is handled on.
 */
public class UiRouter extends EventBusRoutable {
    private static final Path RESOURCES = Paths.get("src/main/resources");
    private static final String STATIC_PATH = "/static/*";
    private static final String STATIC_FOLDER = "static";
    private static final String[] POSTERS = {"alien", "forrest-gump", "pulp-fiction", "titanic", "avatar"};

    public static final String UI_INDEX = "/";
    public static final String UI_USER = "/private/user";
    public static final String UI_HOME = "/private/home";
    public static final String UI_MOVIES = "/private/movies";
    public static final String UI_SERIES = "/private/series";
    public static final String UI_HISTORY = "/private/history";
    public static final String UI_STATISTICS = "/private/statistics";
    public static final String UI_WISHLIST = "/private/wishlist";
    public static final String UI_DISCOVER = "/private/discover";
    public static final String UI_LOGIN = "/login";
    public static final String UI_FORM_LOGIN = "/formlogin";
    public static final String UI_FORM_REGISTER = "/formregister";
    public static final String UI_IDCARDLOGIN = "/idcardlogin";
    public static final String UI_DONATE_SUCCESS = "/private/success";
    public static final String UI_DONATE_FAILURE = "/private/failure";

    private static final String TEMPL_USER = "templates/user.hbs";
    private static final String TEMPL_HOME = "templates/home.hbs";
    private static final String TEMPL_MOVIES = "templates/movies.hbs";
    private static final String TEMPL_SERIES = "templates/series.hbs";
    private static final String TEMPL_HISTORY = "templates/history.hbs";
    private static final String TEMPL_STATISTICS = "templates/statistics.hbs";
    private static final String TEMPL_WISHLIST = "templates/wishlist.hbs";
    private static final String TEMPL_DISCOVER = "templates/discover.hbs";
    private static final String TEMPL_LOGIN = "templates/login.hbs";
    private static final String TEMPL_FORM_LOGIN = "templates/formlogin.hbs";
    private static final String TEMPL_FORM_REGISTER = "templates/formregister.hbs";
    private static final String TEMPL_IDCARDLOGIN = "templates/idcardlogin.hbs";
    private static final String TEMPL_NOTFOUND = "templates/notfound.hbs";
    private static final String TEMPL_DONATE_SUCCESS = "templates/donateSuccess.hbs";
    private static final String TEMPL_DONATE_FAILURE = "templates/donateFailure.hbs";

    private final HandlebarsTemplateEngine engine;
    private final SecurityConfig securityConfig;

    public UiRouter(Vertx vertx, SecurityConfig securityConfig) throws Exception {
        super(vertx);
        this.engine = HandlebarsTemplateEngine.create(isRunningFromJar() ? null : RESOURCES);
        this.securityConfig = securityConfig;
    }

    /**
     * Enables UI routes.
     * Enables static files.
     */
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
        router.get(UI_SERIES).handler(this::handleSeries);
        router.get(UI_HISTORY).handler(this::handleHistory);
        router.get(UI_STATISTICS).handler(this::handleStatistics);
        router.get(UI_WISHLIST).handler(this::handleWishlist);
        router.get(UI_DISCOVER).handler(this::handleDiscover);
        router.post(UI_DONATE_SUCCESS).handler(this::handleDonateSuccess);
        router.post(UI_DONATE_FAILURE).handler(this::handleDonateFailure);

        router.get(STATIC_PATH).handler(StaticHandler.create(isRunningFromJar() ?
                STATIC_FOLDER : RESOURCES.resolve(STATIC_FOLDER).toString())
                .setCachingEnabled(true)
                .setMaxAgeSeconds(DAYS.toSeconds(7))
                .setIncludeHidden(false));

        router.route("/fail").handler(ctx -> ctx.fail(new Throwable("500: Oh noes it crashed"))); // TODO: 20.05.2017 remove
        router.route().failureHandler(this::handleFailure);
        router.get().last().handler(this::handleNotFound);
    }

    private void handleUser(RoutingContext ctx) {
        engine.render(getSafe(ctx, TEMPL_USER, UserTemplate.class)
                .setVK_SERVICE(vk_service)
                .setVK_VERSION(vk_version)
                .setVK_SND_ID(vk_snd_id)
                .setVK_STAMP(vk_stamp)
                .setVK_AMOUNT(vk_amount)
                .setVK_CURR(vk_curr)
                .setVK_ACC(vk_acc)
                .setVK_NAME(vk_name)
                .setVK_REF(vk_ref)
                .setVK_LANG(vk_lang)
                .setVK_MSG(vk_msg)
                .setVK_RETURN(vk_return)
                .setVK_CANCEL(vk_cancel)
                .setVK_ENCODING(vk_encoding)
                .setVK_MAC(createMac())
                .setVK_DATETIME(vk_datetime), endHandler(ctx));
    }

    /**
     * Renders home page.
     * Redirects user to some other page if session contains specified url.
     */
    private void handleHome(RoutingContext ctx) {
        if (ctx.session().getDelegate().data().containsKey(REDIRECT_URL)) {
            redirect(ctx, (String) ctx.session().getDelegate().data().remove(REDIRECT_URL));
            return;
        }
        engine.render(getSafe(ctx, TEMPL_HOME, HomeTemplate.class), endHandler(ctx));
    }

    private void handleMovies(RoutingContext ctx) {
        engine.render(getSafe(ctx, TEMPL_MOVIES, MoviesTemplate.class), endHandler(ctx));
    }

    private void handleSeries(RoutingContext ctx) {
        engine.render(getSafe(ctx, TEMPL_SERIES, SeriesTemplate.class), endHandler(ctx));
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

    private void handleDiscover(RoutingContext ctx) {
        engine.render(getSafe(ctx, TEMPL_DISCOVER, DiscoverTemplate.class), endHandler(ctx));
    }

    /**
     * Renders login page.
     * Changes users language if user pressed any of language buttons.
     * Displays message if one is specified.
     */
    private void handleLogin(RoutingContext ctx) {
        LoginTemplate template = getSafe(ctx, TEMPL_LOGIN, LoginTemplate.class);
        String lang = ctx.request().getParam(LANGUAGE);
        if (lang != null) {
            ctx.removeCookie(LANGUAGE);
            ctx.addCookie(cookie(LANGUAGE, lang).setMaxAge(DAYS.toSeconds(30)));
            template.setLang(lang);
        }
        String key = ctx.request().getParam(DISPLAY_MESSAGE);
        engine.render(template.setDisplayMessage(getString(key, ctx))
                .setFormUrl(UI_HOME + FORM.getClientNamePrefixed())
                .setFacebookUrl(UI_HOME + FACEBOOK.getClientNamePrefixed())
                .setGoogleUrl(UI_HOME + GOOGLE.getClientNamePrefixed())
                .setIdCardUrl(UI_HOME + IDCARD.getClientNamePrefixed()), endHandler(ctx));
    }

    private void handleFormLogin(RoutingContext ctx) {
        String token = UUID.randomUUID().toString();
        ctx.session().put(CSRF_TOKEN, token);
        engine.render(getSafe(ctx, TEMPL_FORM_LOGIN, FormLoginTemplate.class)
                .setCsrfToken(token)
                .setRegisterUrl(UI_FORM_REGISTER)
                .setCallbackUrl(securityConfig.getPac4jConfig()
                        .getClients()
                        .findClient(FormClient.class)
                        .getCallbackUrl()), endHandler(ctx));
    }

    private void handleFormRegister(RoutingContext ctx) {
        String key = ctx.request().getParam(DISPLAY_MESSAGE);
        String token = UUID.randomUUID().toString();
        ctx.session().put(CSRF_TOKEN, token);
        engine.render(getSafe(ctx, TEMPL_FORM_REGISTER, FormRegisterTemplate.class)
                .setCsrfToken(token)
                .setDisplayMessage(getString(key, ctx))
                .setRegisterRestUrl(API_USERS_FORM_INSERT), endHandler(ctx));
    }

    private void handleIdCardLogin(RoutingContext ctx) {
        String state = ctx.request().getHeader(CLIENT_VERIFIED_STATE);
        String cert = ctx.request().getHeader(CLIENT_CERTIFICATE);
        String url = "https://movies.kyngas.eu" + addParameter(UI_LOGIN, DISPLAY_MESSAGE, UNAUTHORIZED);
        check("NONE".equals(state) || cert == null,
                () -> redirect(ctx, url),
                () -> engine.render(getSafe(ctx, TEMPL_IDCARDLOGIN, IdCardLoginTemplate.class)
                        .setClientVerifiedHeader(CLIENT_VERIFIED_STATE)
                        .setClientCertificateHeader(CLIENT_CERTIFICATE)
                        .setClientVerifiedState(state)
                        .setClientCertificate(cert)
                        .setCallbackUrl(securityConfig.getPac4jConfig()
                                .getClients()
                                .findClient(IdCardClient.class)
                                .getCallbackUrl()), endHandler(ctx)));
    }

    private void handleFailure(RoutingContext ctx) {
        ctx.response().setStatusCode(500);
        engine.render(getSafe(ctx, TEMPL_NOTFOUND, NotFoundTemplate.class)
                //.setErrorMessage(ctx.failure().getMessage())
                .setNotFoundName(POSTERS[RANDOM.nextInt(POSTERS.length)]), endHandler(ctx));
    }

    private void handleNotFound(RoutingContext ctx) {
        ctx.response().setStatusCode(404);
        engine.render(getSafe(ctx, TEMPL_NOTFOUND, NotFoundTemplate.class)
                .setNotFoundName(POSTERS[RANDOM.nextInt(POSTERS.length)]), endHandler(ctx));
    }

    private void handleDonateSuccess(RoutingContext ctx) {
        engine.render(getSafe(ctx, TEMPL_DONATE_SUCCESS, DonateSuccessTemplate.class), endHandler(ctx));
    }

    private void handleDonateFailure(RoutingContext ctx) {
        engine.render(getSafe(ctx, TEMPL_DONATE_FAILURE, DonateFailureTemplate.class), endHandler(ctx));
    }

    /**
     * Gets template from TemplateEngine that is at least instance of BaseTemplate.
     * Sets parameters to template that are available to all subclasses of BaseTemplate.
     *
     * @param ctx      to use
     * @param fileName to load
     * @param type     of template to load
     * @return template of specified type
     */
    private <S extends BaseTemplate> S getSafe(RoutingContext ctx, String fileName, Class<S> type) {
        S base = engine.getSafeTemplate(ctx.getDelegate(), fileName, type);
        base.setLang(ctx.getCookie(LANGUAGE) != null ? ctx.getCookie(LANGUAGE).getValue() :
                ENGLISH.getLocale().getLanguage());
        base.setLogoutUrl(addParameter(AUTH_LOGOUT, URL, UI_LOGIN));
        base.setLoginPage(UI_LOGIN);
        base.setUserPage(UI_USER);
        base.setHomePage(UI_HOME);
        base.setMoviesPage(UI_MOVIES);
        base.setSeriesPage(UI_SERIES);
        base.setHistoryPage(UI_HISTORY);
        base.setStatisticsPage(UI_STATISTICS);
        base.setWishlistPage(UI_WISHLIST);
        base.setDiscoverPage(UI_DISCOVER);
        CommonProfile profile = getProfile(ctx);
        if (profile != null) {
            base.setUserName(profile.getFirstName() + " " + profile.getFamilyName());
            base.setUserFirstName(profile.getFirstName());
        }
        return base;
    }
}