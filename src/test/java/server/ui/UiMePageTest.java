package server.ui;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.core.Vertx;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import server.util.LocalDatabase;
import server.verticle.ServerVerticle;

import java.util.concurrent.atomic.AtomicBoolean;

import static io.vertx.rxjava.core.RxHelper.deployVerticle;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static server.util.FileUtils.getConfig;
import static server.util.LocalDatabase.initializeDatabase;
import static server.util.LoginUtils.asyncFormLogin;
import static server.util.NetworkUtils.HTTP_PORT;

@SuppressWarnings("Duplicates")
@RunWith(VertxUnitRunner.class)
public class UiMePageTest {
    private static final int PORT = 8082;
    private static final String URI = "http://localhost:" + PORT;

    private Vertx vertx;
    private JsonObject config;
    private WebDriver driver;
    private LocalDatabase hsqldb;

    @Before
    public void setUp(TestContext ctx) throws Exception {
        driver = new HtmlUnitDriver();
        vertx = Vertx.vertx();
        config = getConfig().put(HTTP_PORT, PORT);
        config.getJsonObject("oauth").put("localCallback", URI + "/callback");
        initializeDatabase(vertx, config.getJsonObject("mysql")).rxSetHandler()
                .doOnSuccess(db -> hsqldb = db)
                .doOnError(ctx::fail)
                .flatMap(db -> deployVerticle(vertx, new ServerVerticle(), new DeploymentOptions()
                        .setConfig(config))
                        .toSingle())
                .flatMap(s -> asyncFormLogin(driver, URI, config).rxSetHandler())
                .doOnError(ctx::fail)
                .test()
                .awaitTerminalEvent(15, SECONDS)
                .assertCompleted();
    }

    @Test
    public void testMePageMessengerMessageIsPushed(TestContext ctx) throws Exception {
        String sendable = "Hello world!";
        AtomicBoolean bool = new AtomicBoolean(false);
        String url = URI + "/private/user";
        driver.get(url);
        assertEquals(url, driver.getCurrentUrl());
        vertx.eventBus().consumer("messenger", msg -> {
            bool.set(true);
            ctx.assertEquals(sendable, msg.body());
        });
        driver.findElement(By.id("MessageInput")).sendKeys(sendable);
        driver.findElement(By.id("SendMessage")).click();
        await().untilAtomic(bool, is(true));
    }

    @After
    public void tearDown(TestContext ctx) throws Exception {
        driver.quit();
        vertx.close(ctx.asyncAssertSuccess());
    }
}
