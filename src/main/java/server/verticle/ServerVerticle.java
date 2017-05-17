package server.verticle;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.ext.web.Router;
import server.router.*;
import server.security.SecurityConfig;
import server.service.BankLinkService;
import server.service.DatabaseService;
import server.service.MailService;
import server.service.TmdbService;

import java.util.Arrays;

import static server.router.EventBusRoutable.closeEventbus;
import static server.router.EventBusRoutable.startEventbus;
import static server.util.CommonUtils.createIfMissing;
import static server.util.NetworkUtils.*;

/**
 * Main server logic.
 * Creates services, security configuration and routes.
 * Creates a HTTP server.
 */
public class ServerVerticle extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(ServerVerticle.class);

    private DatabaseService database;
    private TmdbService tmdb;
    private BankLinkService bankLink;
    private MailService mail;
    private SecurityConfig securityConfig;

    /**
     * Creates service for interacting with database.
     * Creates service for interacting with TheMovieDatabase API.
     * Creates service for interacting with bankLink application.
     * Creates service for interacting with mail server.
     * Creates Pac4j security engine configuration.
     * <p>
     * Creates authentication for routes.
     * Creates external API for TheMovieDatabase services.
     * Creates external API for bankLink services.
     * Creates Eventbus (aka Websocket for browsers) addresses for various services.
     * Creates external API for database services.
     * Creates external API for mail services.
     * Creates routes for UI rendering.
     * <p>
     * Starts the HTTP server.
     */
    @Override
    public void start(Future<Void> future) throws Exception {
        Router router = Router.router(vertx);
        database = createIfMissing(database, () -> DatabaseService.create(vertx, config(), this));
        tmdb = createIfMissing(tmdb, () -> TmdbService.create(vertx, config(), database));
        bankLink = createIfMissing(bankLink, () -> BankLinkService.create(vertx, config()));
        mail = createIfMissing(mail, () -> MailService.create(vertx, database));
        securityConfig = createIfMissing(securityConfig, () -> new SecurityConfig(config(), database));
        Arrays.asList(
                new AuthRouter(vertx, config(), securityConfig),
                new TmdbRouter(vertx, tmdb),
                new BankLinkRouter(vertx, bankLink),
                new DatabaseRouter(vertx, config(), database, mail),
                new MailRouter(vertx, mail),
                new UiRouter(vertx, securityConfig)).forEach(routable -> routable.route(router));
        startEventbus(router, vertx);
        vertx.createHttpServer(new HttpServerOptions()
                .setCompressionSupported(true)
                .setCompressionLevel(3))
                .requestHandler(router::accept)
                .rxListen(config().getInteger(HTTP_PORT, DEFAULT_PORT), config().getString(HTTP_HOST, DEFAULT_HOST))
                .subscribe(res -> future.complete(), future::fail);
    }

    public ServerVerticle setDatabase(DatabaseService database) {
        this.database = database;
        return this;
    }

    public ServerVerticle setTmdb(TmdbService tmdb) {
        this.tmdb = tmdb;
        return this;
    }

    public ServerVerticle setBankLink(BankLinkService bankLink) {
        this.bankLink = bankLink;
        return this;
    }

    public ServerVerticle setMail(MailService mail) {
        this.mail = mail;
        return this;
    }

    public ServerVerticle setSecurityConfig(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
        return this;
    }

    public DatabaseService getDatabase() {
        return database;
    }

    public TmdbService getTmdb() {
        return tmdb;
    }

    public BankLinkService getBankLink() {
        return bankLink;
    }

    public MailService getMail() {
        return mail;
    }

    public SecurityConfig getSecurityConfig() {
        return securityConfig;
    }

    @Override
    public void stop() throws Exception {
        closeEventbus();
    }
}