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
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import server.util.LocalDatabase;
import server.verticle.ServerVerticle;

import static io.vertx.rxjava.core.RxHelper.deployVerticle;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertTrue;
import static org.openqa.selenium.By.*;
import static org.openqa.selenium.support.ui.ExpectedConditions.textToBe;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;
import static server.util.FileUtils.getConfig;
import static server.util.LocalDatabase.initializeDatabase;
import static server.util.LoginUtils.asyncFormLogin;
import static server.util.NetworkUtils.HTTP_PORT;
import static server.util.Utils.Eventbus.*;
import static server.util.Utils.assertGoToPage;
import static server.util.Utils.createDriver;

@SuppressWarnings("Duplicates")
@RunWith(VertxUnitRunner.class)
public class UiMoviesPageTest {
    private static final int PORT = 8082;
    private static final String URI = "http://localhost:" + PORT;

    private static Vertx vertx;
    private static JsonObject config;
    private static HtmlUnitDriver driver;
    private static LocalDatabase localDatabase;

    @BeforeClass
    public static void setUp(TestContext ctx) throws Exception {
        driver = createDriver(true);
        vertx = Vertx.vertx();
        config = getConfig().put(HTTP_PORT, PORT);
        config.getJsonObject("oauth").put("localCallback", URI + "/callback");
        initializeDatabase(vertx, config.getJsonObject("mysql")).rxSetHandler()
                .doOnSuccess(db -> localDatabase = db)
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

    @AfterClass
    public static void tearDown(TestContext ctx) throws Exception {
        Async async = ctx.async();
        localDatabase.dropAll().setHandler(ar -> {
            if (ar.succeeded()) {
                driver.quit();
                vertx.close(ctx.asyncAssertSuccess());
                async.complete();
            } else {
                ctx.fail(ar.cause());
            }
        });
    }

    @Test
    public void testSearchingMovieFindsResult() throws Exception {
        assertGoToPage(driver, URI + "/private/movies");
        await().until(() -> isEventbus(OPEN, driver));
        driver.findElement(id("search")).sendKeys("ghost in the shell");
        driver.findElement(xpath("//label[@id='search-button']/i")).click();
        new WebDriverWait(driver, 5)
                .until(visibilityOfElementLocated(cssSelector("div.card-content.truncate")));
        assertTrue(driver.findElement(cssSelector("div.card-content.truncate"))
                .getText().contains("Ghost in the Shell"));
        closeEventbus(driver);
        await().until(() -> isEventbus(CLOSED, driver));
    }

    @Test
    public void testOpeningDetailView() throws Exception {
        assertGoToPage(driver, URI + "/private/movies");
        await().until(() -> isEventbus(OPEN, driver));
        driver.findElement(id("search")).sendKeys("ghost in the shell");
        driver.findElement(xpath("//label[@id='search-button']/i")).click();
        new WebDriverWait(driver, 5)
                .until(visibilityOfElementLocated(cssSelector("div.card-content.truncate")));
        driver.findElement(cssSelector("div.card-content.truncate")).click();
        new WebDriverWait(driver, 5)
                .until(textToBe(id("movie-title"), "Ghost in the Shell"));
        closeEventbus(driver);
        await().until(() -> isEventbus(CLOSED, driver));
    }

    @Test
    public void testAddingToWishlist() throws Exception {
        localDatabase.resetCleanState().rxSetHandler().toBlocking().value();
        assertGoToPage(driver, URI + "/private/movies");
        await().until(() -> isEventbus(OPEN, driver));
        driver.findElement(id("search")).sendKeys("ghost in the shell");
        driver.findElement(xpath("//label[@id='search-button']/i")).click();
        new WebDriverWait(driver, 5)
                .until(visibilityOfElementLocated(cssSelector("div.card-content.truncate")));
        driver.findElement(cssSelector("div.card-content.truncate")).click();
        new WebDriverWait(driver, 5)
                .until(textToBe(id("movie-title"), "Ghost in the Shell"));
        driver.findElement(id("wishlist-text")).click();
        await().atMost(5, SECONDS).until(() -> localDatabase
                .query("SELECT * FROM Wishlist WHERE MovieId = ?", new JsonArray().add(315837))
                .rxSetHandler()
                .toBlocking()
                .value().size() > 0);
        closeEventbus(driver);
        await().until(() -> isEventbus(CLOSED, driver));
    }

    @Test
    public void testAddingView() throws Exception {
        localDatabase.resetCleanState().rxSetHandler().toBlocking().value();
        assertGoToPage(driver, URI + "/private/movies");
        await().until(() -> isEventbus(OPEN, driver));
        driver.findElement(id("search")).sendKeys("ghost in the shell");
        driver.findElement(xpath("//label[@id='search-button']/i")).click();
        new WebDriverWait(driver, 5)
                .until(visibilityOfElementLocated(cssSelector("div.card-content.truncate")));
        driver.findElement(cssSelector("div.card-content.truncate")).click();
        new WebDriverWait(driver, 5)
                .until(textToBe(id("movie-title"), "Ghost in the Shell"));
        driver.findElement(cssSelector("#add-watch > div.card-content")).click();
        driver.findElement(id("watchStartNow")).click();
        driver.findElement(id("watchEndCalculate")).click();
        driver.executeScript("document.getElementById('watchStartTime').value = '12:00'");
        driver.executeScript("document.getElementById('watchStartTime').setAttribute('data-submit', '12:00:00')");
        driver.executeScript("document.getElementById('watchEndTime').value = '13:46'");
        driver.executeScript("document.getElementById('watchEndTime').setAttribute('data-submit', '13:46:00')");
        driver.findElement(id("add-btn")).click();
        await().atMost(5, SECONDS).until(() -> localDatabase
                .query("SELECT * FROM Views WHERE MovieId = ?", new JsonArray().add(315837))
                .rxSetHandler()
                .toBlocking()
                .value().size() > 0);
        closeEventbus(driver);
        await().until(() -> isEventbus(CLOSED, driver));
    }
}
