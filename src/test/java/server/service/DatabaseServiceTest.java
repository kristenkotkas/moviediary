package server.service;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.core.Vertx;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Observable;
import server.service.DatabaseService.Column;
import server.util.LocalDatabase;
import server.verticle.ServerVerticle;

import static io.vertx.rxjava.core.RxHelper.deployVerticle;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static server.util.FileUtils.getConfig;
import static server.util.LocalDatabase.initializeDatabase;
import static server.util.NetworkUtils.HTTP_PORT;

@SuppressWarnings("Duplicates")
@RunWith(VertxUnitRunner.class)
public class DatabaseServiceTest {
    private static final int PORT = 8082;

    private static Vertx vertx;
    private static JsonObject config;
    private static DatabaseService database;
    private static LocalDatabase localDatabase;

    @BeforeClass
    public static void setUp(TestContext ctx) throws Exception {
        vertx = Vertx.vertx();
        config = getConfig().put(HTTP_PORT, PORT);
        initializeDatabase(vertx, config.getJsonObject("mysql")).rxSetHandler()
                .doOnSuccess(db -> localDatabase = db)
                .doOnError(ctx::fail)
                .flatMap(db -> Observable.just(DatabaseService.create(vertx, config)).toSingle())
                .doOnSuccess(db -> database = db)
                .doOnError(ctx::fail)
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
        JsonArray users = localDatabase.clearTables().rxSetHandler()
                .doOnError(ctx::fail)
                .flatMap(v -> database.getAllUsers().rxSetHandler())
                .doOnError(ctx::fail)
                .map(DatabaseService::getRows)
                .toBlocking()
                .value();
        assertThat(users.size(), is(2));
        JsonObject user = users.getJsonObject(0);
        assertThat(user.getInteger("Id"), is(3));
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
        String count = localDatabase.clearTables().rxSetHandler()
                .doOnError(ctx::fail)
                .flatMap(v -> database.getUsersCount().rxSetHandler())
                .doOnError(ctx::fail)
                .toBlocking()
                .value();
        assertThat(count, is("2"));
    }

    @AfterClass
    public static void tearDown(TestContext ctx) throws Exception {
        Async async = ctx.async();
        localDatabase.dropAll().setHandler(ar -> {
            if (ar.succeeded()) {
                vertx.close(ctx.asyncAssertSuccess());
                async.complete();
            } else {
                ctx.fail(ar.cause());
            }
        });
    }
}
