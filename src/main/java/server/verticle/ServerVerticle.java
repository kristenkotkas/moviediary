package server.verticle;

import database.DatabaseService;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.ext.web.Router;
import mail.MailService;
import omdb.OmdbService;
import server.router.*;
import server.security.SecurityConfig;
import tmdb.TmdbService;

import java.util.Arrays;

import static server.router.EventBusRoutable.closeEventbus;
import static server.router.EventBusRoutable.startEventbus;
import static util.ConditionUtils.createIfMissing;
import static util.NetworkUtils.*;

/**
 * Main server logic.
 * Creates services, security configuration and routes.
 * Creates a HTTP server.
 */
public class ServerVerticle extends AbstractVerticle {
  private DatabaseService database;
  private TmdbService tmdb;
  private OmdbService omdb;
  private MailService mail;
  private SecurityConfig securityConfig;

  // TODO: 1.06.2017 separate services
  // TODO: 1.06.2017 each service will get verticle for communicating with their purpose -> etc mysql
  // TODO: 1.06.2017 each service will get verticle for communicating with each other -> other services, clients
  // TODO: 1.06.2017 service/api impl

  // TODO: 1.06.2017 baseverticle for all -> service discovery, http rest / eventbus methods
  // TODO: 1.06.2017 all in separate modules

  // TODO: 1.06.2017 metrics

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
    database = createIfMissing(database, () -> DatabaseService.createProxy(vertx.getDelegate(), DatabaseService.SERVICE_ADDRESS));
    tmdb = createIfMissing(tmdb, () -> TmdbService.createProxy(vertx.getDelegate(), TmdbService.SERVICE_ADDRESS));
    omdb = createIfMissing(omdb, () -> OmdbService.createProxy(vertx.getDelegate(), OmdbService.SERVICE_ADDRESS));
    mail = createIfMissing(mail, () -> MailService.createProxy(vertx.getDelegate(), MailService.SERVICE_ADDRESS));
    securityConfig = createIfMissing(securityConfig, () -> new SecurityConfig(vertx, config(), database));
    Arrays.asList(
        new AuthRouter(vertx, config(), securityConfig),
        new TmdbRouter(vertx, tmdb),
        new OmdbRouter(vertx, omdb),
        new DatabaseRouter(vertx, config(), securityConfig, database, mail),
        new MailRouter(vertx, mail),
        new UiRouter(vertx, securityConfig))
        .forEach(routable -> routable.route(router));
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
  }
}