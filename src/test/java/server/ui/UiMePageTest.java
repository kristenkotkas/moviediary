package server.ui;

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
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import server.util.LocalDatabase;
import server.verticle.ServerVerticle;

import java.util.List;

import static io.vertx.rxjava.core.RxHelper.deployVerticle;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.openqa.selenium.By.tagName;
import static server.util.FileUtils.getConfig;
import static server.util.LocalDatabase.initializeDatabase;
import static server.util.LoginUtils.asyncFormLogin;
import static server.util.NetworkUtils.HTTP_PORT;
import static server.util.Utils.Eventbus.*;
import static server.util.Utils.createDriver;

@SuppressWarnings("Duplicates")
@RunWith(VertxUnitRunner.class)
public class UiMePageTest {
    private static final int PORT = 8082;
    private static final String URI = "http://localhost:" + PORT;

    private static Vertx vertx;
    private static JsonObject config;
    private static HtmlUnitDriver driver;
    private static LocalDatabase hsqldb;

    @BeforeClass
    public static void setUp(TestContext ctx) throws Exception {
        driver = createDriver();
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
                .awaitTerminalEvent(10, SECONDS)
                .assertCompleted();
    }

    @Test
    public void testMessageIsPushedToEverybody(TestContext ctx) throws Exception {
        String message = "Hello world!";
        String url = URI + "/private/user";
        Async async = ctx.async();
        vertx.eventBus().consumer("messenger", msg -> {
            assertEquals(message, msg.body());
            await().until(() -> driver.findElement(By.id("toast-container")).getText().contains(message));
            closeEventbus(driver);
            await().until(() -> isEventbus(CLOSED, driver));
            async.complete();
        });
        driver.get(url);
        assertEquals(url, driver.getCurrentUrl());
        await().until(() -> isEventbus(OPEN, driver));
        driver.findElement(By.id("MessageInput")).sendKeys(message);
        driver.findElement(By.id("SendMessage")).click();
    }

    @Test
    public void testMyDetailsAreCorrect() throws Exception {
        JsonObject details = hsqldb.query("SELECT * FROM Users " +
                "JOIN Settings ON Users.Username = Settings.Username " +
                "WHERE Users.Username = ?", new JsonArray()
                .add(config.getJsonObject("unit_test").getJsonObject("form_user").getString("username")))
                .rxSetHandler().toBlocking()
                .value().getJsonObject(0);
        String url = URI + "/private/user";
        driver.get(url);
        assertEquals(url, driver.getCurrentUrl());
        await().until(() -> isEventbus(OPEN, driver));
        await().until(() -> driver.findElements(tagName("td")).size() > 0);
        List<WebElement> data = driver.findElements(tagName("td"));
        assertTrue(data.get(1).getText().equals(String.valueOf(details.getInteger("ID"))));
        assertTrue(data.get(3).getText().equals(details.getString("FIRSTNAME")));
        assertTrue(data.get(5).getText().equals(details.getString("LASTNAME")));
        assertTrue(data.get(7).getText().equals(details.getString("USERNAME")));
        assertTrue(data.get(9).getText().equals(details.getString("RUNTIMETYPE")));
        assertTrue(data.get(11).getText().equals(details.getString("VERIFIED").equals("1") ? "true" : "false"));
        closeEventbus(driver);
        await().until(() -> isEventbus(CLOSED, driver));
    }

    @AfterClass
    public static void tearDown(TestContext ctx) throws Exception {
        driver.quit();
        vertx.close(ctx.asyncAssertSuccess());
    }
}
