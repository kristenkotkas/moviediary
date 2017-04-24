package server.ui;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.rxjava.core.Vertx;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import server.util.LocalDatabase;
import server.verticle.ServerVerticle;

import static io.vertx.rxjava.core.RxHelper.deployVerticle;
import static java.util.concurrent.TimeUnit.SECONDS;
import static server.util.CommonUtils.check;
import static server.util.FileUtils.getConfig;
import static server.util.LocalDatabase.initializeDatabase;
import static server.util.LoginUtils.formLogin;
import static server.util.NetworkUtils.HTTP_PORT;
import static server.util.Utils.createDriverService;

public abstract class UiTest {
    protected static final int PORT = 8082;
    protected static final String URI = "http://localhost:" + PORT;

    protected static Vertx vertx;
    protected static JsonObject config;
    protected static ChromeDriverService service;
    protected static LocalDatabase localDatabase;

    protected ChromeDriver driver;

    @BeforeClass
    public static void setUp(TestContext ctx) throws Exception {
        service = createDriverService();
        vertx = Vertx.vertx();
        config = getConfig().put(HTTP_PORT, PORT);
        config.getJsonObject("oauth").put("localCallback", URI + "/callback");
        initializeDatabase(vertx, config.getJsonObject("mysql")).rxSetHandler()
                .doOnSuccess(db -> localDatabase = db)
                .doOnError(ctx::fail)
                .flatMap(db -> deployVerticle(vertx, new ServerVerticle(), new DeploymentOptions()
                        .setConfig(config))
                        .toSingle())
                .test()
                .awaitTerminalEvent(10, SECONDS)
                .assertCompleted();
        /*        initializeDatabase(vertx, config.getJsonObject("mysql")).rxSetHandler()
                .doOnSuccess(db -> database = db)
                .doOnError(ctx::fail)
                .toCompletable()
                .andThen(deployVerticle(vertx, new ServerVerticle(), new DeploymentOptions().setConfig(config)))
                .test()
                .awaitTerminalEvent(5, SECONDS)
                .assertCompleted();*/
    }

    @AfterClass
    public static void tearDown(TestContext ctx) throws Exception {
        service.stop();
        Async async = ctx.async();
        localDatabase.dropAll().setHandler(ar -> check(ar.succeeded(), () -> {
            vertx.close(ctx.asyncAssertSuccess());
            async.complete();
        }, () -> ctx.fail(ar.cause())));
    }

    @Before
    public void setUp() throws Exception {
        localDatabase.resetCleanStateBlocking();
        driver = new ChromeDriver(service);
        formLogin(driver, URI, config);
    }

    @After
    public void tearDown() throws Exception {
        driver.quit();
    }
}