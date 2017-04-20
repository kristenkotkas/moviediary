package server.service;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.core.Vertx;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Observable;
import server.service.DatabaseService.Column;
import server.ui.UiFormLoginPageTest;
import server.util.LocalDatabase;
import server.verticle.ServerVerticle;

import static io.vertx.core.logging.LoggerFactory.getLogger;
import static io.vertx.rxjava.core.RxHelper.deployVerticle;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static server.util.FileUtils.getConfig;
import static server.util.LocalDatabase.initializeDatabase;
import static server.util.NetworkUtils.HTTP_PORT;

@SuppressWarnings("Duplicates")
@RunWith(VertxUnitRunner.class)
public class DatabaseServiceTest {
    private static final Logger LOG = getLogger(UiFormLoginPageTest.class);
    private static final int PORT = 8082;
    private static final String URI = "http://localhost:" + PORT;

    private Vertx vertx;
    private JsonObject config;
    private DatabaseService database;
    private LocalDatabase hsqldb;

    @Before
    public void setUp(TestContext ctx) throws Exception {
        vertx = Vertx.vertx();
        config = getConfig().put(HTTP_PORT, PORT);
        config.getJsonObject("oauth").put("localCallback", URI + "/callback");
        initializeDatabase(vertx, config.getJsonObject("mysql")).rxSetHandler()
                .doOnSuccess(db -> hsqldb = db)
                .doOnError(ctx::fail)
                .toCompletable()
                .andThen(Observable.just(DatabaseService.create(vertx, config)))
                .toSingle()
                .doOnSuccess(db -> database = db)
                .flatMap(db -> deployVerticle(vertx, new ServerVerticle()
                        .setDatabase(db), new DeploymentOptions()
                        .setConfig(config))
                        .toSingle())
                .test()
                .awaitTerminalEvent(5, SECONDS)
                .assertCompleted();
    }

    @Test
    public void testGetAllUsers(TestContext ctx) throws Exception {
        JsonArray users = hsqldb.reset().rxSetHandler()
                .doOnError(ctx::fail)
                .flatMap(v -> database.getAllUsers().rxSetHandler())
                .doOnError(ctx::fail)
                .map(DatabaseService::getRows)
                .toBlocking()
                .value();
        assertThat(users.size(), is(1));
        JsonObject user = users.getJsonObject(0);
        assertThat(user.getInteger("ID"), is(1));
        assertThat(user.getString(Column.FIRSTNAME.getName()), is("Form"));
        assertThat(user.getString(Column.LASTNAME.getName()), is("Tester"));
        assertThat(user.getString(Column.USERNAME.getName()), is("unittest@kyngas.eu"));
        assertThat(user.getString(Column.PASSWORD.getName()),
                is("d7d1b328a56a8c8bfd6dd4e3d9006365d3496b523ac4ff37bc68679b5433b486"));
        assertThat(user.getString(Column.SALT.getName()), is("f44a65de25274188"));
        assertThat(user.getString(Column.RUNTIMETYPE.getName()), is("default"));
        assertThat(user.getString(Column.VERIFIED.getName()), is("1"));
    }

    @Test
    public void testGetUsersCount(TestContext ctx) throws Exception {
        String count = hsqldb.reset().rxSetHandler()
                .doOnError(ctx::fail)
                .flatMap(v -> database.getUsersCount().rxSetHandler())
                .doOnError(ctx::fail)
                .toBlocking()
                .value();
        assertThat(count, is("1"));
    }

    @After
    public void tearDown() throws Exception {
        vertx.rxClose()
                .test()
                .awaitTerminalEvent(5, SECONDS)
                .assertCompleted();
    }
}
