package server.ui;

import io.vertx.core.DeploymentOptions;
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
import static org.junit.Assert.assertEquals;
import static org.openqa.selenium.By.cssSelector;
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
public class UiWishlistPageTest {
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
    public void testWishlistMoviesAreLoaded() throws Exception {
        assertGoToPage(driver, URI + "/private/wishlist");
        await().until(() -> isEventbus(OPEN, driver));
        new WebDriverWait(driver, 5)
                .until(visibilityOfElementLocated(cssSelector("img.wishlist-object.responsive-img")));
        closeEventbus(driver);
        await().until(() -> isEventbus(CLOSED, driver));
    }

    @Test
    public void testClickingMovieRedirectsToMoviePage() throws Exception {
        assertGoToPage(driver, URI + "/private/wishlist");
        await().until(() -> isEventbus(OPEN, driver));
        new WebDriverWait(driver, 5)
                .until(visibilityOfElementLocated(cssSelector("img.wishlist-object.responsive-img")));
        closeEventbus(driver);
        await().until(() -> isEventbus(CLOSED, driver));
        driver.findElement(cssSelector("img.wishlist-object.responsive-img")).click();
        assertEquals(URI + "/private/movies/?id=49051", driver.getCurrentUrl());
        await().until(() -> isEventbus(OPEN, driver));
        closeEventbus(driver);
        await().until(() -> isEventbus(CLOSED, driver));
    }
}
