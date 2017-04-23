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
import server.util.LocalDatabase;
import server.verticle.ServerVerticle;

import static io.vertx.rxjava.core.RxHelper.deployVerticle;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.openqa.selenium.By.tagName;
import static server.entity.Language.getString;
import static server.util.FileUtils.getConfig;
import static server.util.LocalDatabase.initializeDatabase;
import static server.util.LoginUtils.asyncFormLogin;
import static server.util.NetworkUtils.HTTP_PORT;
import static server.util.Utils.createDriver;

@SuppressWarnings("Duplicates")
@RunWith(VertxUnitRunner.class)
public class UiHomePageTest {
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

    @Test
    public void testHomePageTranslations() throws Exception {
        checkHomePageTranslations("en");
        checkHomePageTranslations("et");
        checkHomePageTranslations("de");
    }

    private void checkHomePageTranslations(String lang) {
        JsonObject formAuth = config.getJsonObject("unit_test").getJsonObject("form_user");
        String url = URI + "/login?lang=" + lang;
        driver.get(url);
        assertEquals(url, driver.getCurrentUrl());
        url = URI + "/private/home";
        driver.get(url);
        assertEquals(url, driver.getCurrentUrl());
        assertEquals(getString("HOME_HELLO", lang) + " " + formAuth.getString("firstname"),
                driver.findElement(tagName("h3")).getText());
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
}
