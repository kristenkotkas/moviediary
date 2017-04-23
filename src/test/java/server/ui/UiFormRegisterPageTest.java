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
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import server.util.LocalDatabase;
import server.verticle.ServerVerticle;

import java.util.List;

import static io.vertx.rxjava.core.RxHelper.deployVerticle;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;
import static org.junit.Assert.assertEquals;
import static org.openqa.selenium.By.tagName;
import static server.entity.Language.getString;
import static server.util.FileUtils.getConfig;
import static server.util.LocalDatabase.initializeDatabase;
import static server.util.LoginUtils.asyncFormLogin;
import static server.util.NetworkUtils.HTTP_PORT;
import static server.util.Utils.assertGoToPage;
import static server.util.Utils.createDriver;

@SuppressWarnings("Duplicates")
@RunWith(VertxUnitRunner.class)
public class UiFormRegisterPageTest {
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

    // TODO: 23.04.2017 register test

    @Test
    public void testFormRegisterPageLinks() throws Exception {
        assertGoToPage(driver, URI + "/formregister");
        assertEquals("/public/api/v1/users/form/insert",
                escapeHtml4(driver.findElement(tagName("form")).getAttribute("action")));
    }

    @Test
    public void testFormRegisterPageTranslations() throws Exception {
        checkFormRegisterPageTranslations("en");
        checkFormRegisterPageTranslations("et");
        checkFormRegisterPageTranslations("de");
    }

    private void checkFormRegisterPageTranslations(String lang) {
        assertGoToPage(driver, URI + "/login?lang=" + lang);
        assertGoToPage(driver, URI + "/formregister?message=FORM_REGISTER_EXISTS");
        assertEquals(getString("FORM_REGISTER_TITLE", lang), driver.getTitle());
        List<WebElement> headers = driver.findElements(tagName("h5"));
        assertEquals(getString("FORM_REGISTER_SIGNUP", lang), headers.get(0).getText());
        assertEquals(getString("FORM_REGISTER_EXISTS", lang), headers.get(1).getText());
        List<WebElement> textFields = driver.findElements(tagName("label"));
        assertEquals(getString("FORM_REGISTER_FIRSTNAME", lang), textFields.get(0).getText());
        assertEquals(getString("FORM_REGISTER_LASTNAME", lang), textFields.get(1).getText());
        assertEquals(getString("FORM_LOGIN_EMAIL", lang), textFields.get(2).getText());
        assertEquals(getString("FORM_LOGIN_PASSWORD", lang), textFields.get(3).getText());
        assertEquals(getString("FORM_LOGIN_REGISTER", lang), driver.findElement(tagName("button")).getText());
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
