package server.verticle;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.ext.web.Router;
import java.util.Arrays;
import server.database.DatabaseManager;
import server.router.AuthRouter;
import server.router.BankLinkRouter;
import server.router.DatabaseRouter;
import server.router.MailRouter;
import server.router.OmdbRouter;
import server.router.RecommendRouter;
import server.router.TmdbRouter;
import server.router.UiRouter;
import server.security.SecurityConfig;
import server.service.BankLinkService;
import server.service.DatabaseService;
import server.service.MailService;
import server.service.OmdbService;
import server.service.RecommendService;
import server.service.TmdbService;
import static server.router.EventBusRoutable.closeEventbus;
import static server.router.EventBusRoutable.startEventbus;
import static server.util.CommonUtils.createIfMissing;
import static server.util.NetworkUtils.DEFAULT_HOST;
import static server.util.NetworkUtils.DEFAULT_PORT;
import static server.util.NetworkUtils.HTTP_HOST;
import static server.util.NetworkUtils.HTTP_PORT;

/**
 * Main server logic.
 * Creates services, security configuration and routes.
 * Creates a HTTP server.
 */
public class ServerVerticle extends AbstractVerticle {
  private DatabaseService database;
  private TmdbService tmdb;
  private OmdbService omdb;
  private BankLinkService bankLink;
  private MailService mail;
  private RecommendService recommend;
  private SecurityConfig securityConfig;
  private DatabaseManager databaseManager;

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
    databaseManager = new DatabaseManager(config()).start();
    database = createIfMissing(database, () -> DatabaseService.create(vertx, config()));
    tmdb = createIfMissing(tmdb, () -> TmdbService.create(vertx, config(), database));
    omdb = createIfMissing(omdb, () -> OmdbService.create(vertx, config(), database));
    bankLink = createIfMissing(bankLink, () -> BankLinkService.create(vertx, config()));
    mail = createIfMissing(mail, () -> MailService.create(vertx, database, config()));
    recommend = createIfMissing(recommend, () -> RecommendService.create(vertx));
    securityConfig = createIfMissing(securityConfig, () -> new SecurityConfig(vertx, config(), database));
    Arrays.asList(
        new AuthRouter(vertx, config(), securityConfig),
        new TmdbRouter(vertx, tmdb),
        new OmdbRouter(vertx, omdb),
        new BankLinkRouter(vertx, bankLink),
        new DatabaseRouter(vertx, config(), securityConfig, database, mail),
        new MailRouter(vertx, mail),
        new RecommendRouter(recommend),
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

  @Override
  public void stop() throws Exception {
    closeEventbus();
    if (databaseManager != null) {
      databaseManager.stop();
    }
  }
}