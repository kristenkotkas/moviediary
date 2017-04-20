package server.ui;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.core.Vertx;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import server.util.LocalDatabase;
import server.verticle.ServerVerticle;

import java.util.List;

import static io.vertx.core.logging.LoggerFactory.getLogger;
import static io.vertx.rxjava.core.RxHelper.deployVerticle;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.openqa.selenium.By.tagName;
import static server.entity.Language.getString;
import static server.util.FileUtils.getConfig;
import static server.util.LocalDatabase.initializeDatabase;
import static server.util.LoginUtils.formLogin;
import static server.util.NetworkUtils.HTTP_PORT;

@SuppressWarnings("Duplicates")
@RunWith(VertxUnitRunner.class)
public class UiFormLoginPageTest {
    private static final Logger LOG = getLogger(UiFormLoginPageTest.class);
    private static final int PORT = 8082;
    private static final String URI = "http://localhost:" + PORT;

    private Vertx vertx;
    private JsonObject config;
    private WebDriver driver;
    private LocalDatabase database;

    @Before
    public void setUp() throws Exception {
        driver = new HtmlUnitDriver();
        vertx = Vertx.vertx();
        config = getConfig().put(HTTP_PORT, PORT);
        config.getJsonObject("oauth").put("localCallback", URI + "/callback");
        initializeDatabase(vertx, config.getJsonObject("mysql")).rxSetHandler()
                .doOnSuccess(db -> database = db)
                .doOnError(err -> fail(err.getMessage()))
                .toCompletable()
                .andThen(deployVerticle(vertx, new ServerVerticle(), new DeploymentOptions().setConfig(config)))
                .test()
                .awaitTerminalEvent(5, SECONDS)
                .assertCompleted();
    }

    @Test
    public void testLoginUnauthorized() throws Exception {
        driver.manage().deleteAllCookies();
        formLogin(driver, URI, new JsonObject().put("unit_test", new JsonObject().put("form_user",
                new JsonObject().put("username", "megalamp").put("password", "ultrateam3000"))));
        assertEquals(URI + "/login?message=AUTHORIZER_UNAUTHORIZED", driver.getCurrentUrl());
    }

    @Test
    public void testLoginAuthorizedToHomePage() throws Exception {
        driver.manage().deleteAllCookies();
        formLogin(driver, URI, config);
        assertEquals(URI + "/private/home", driver.getCurrentUrl());
    }

    @Test
    public void testFormLoginPageLinks() throws Exception {
        String url = URI + "/formlogin";
        driver.get(url);
        assertEquals(url, driver.getCurrentUrl());
        assertEquals(URI + "/callback?client_name=FormClient",
                escapeHtml4(driver.findElement(tagName("form")).getAttribute("action")));
        assertEquals(URI + "/formregister", driver.findElement(tagName("a")).getAttribute("href"));
    }

    @Test
    public void testFormLoginPageTranslations() throws Exception {
        checkFormLoginPageTranslations("en");
        checkFormLoginPageTranslations("et");
        checkFormLoginPageTranslations("de");
    }

    private void checkFormLoginPageTranslations(String lang) {
        String url = URI + "/login?lang=" + lang;
        driver.get(url);
        assertEquals(url, driver.getCurrentUrl());
        url = URI + "/formlogin";
        driver.get(url);
        assertEquals(url, driver.getCurrentUrl());
        assertEquals(getString("FORM_LOGIN_TITLE", lang), driver.getTitle());
        assertEquals(getString("LOGIN_TITLE", lang), driver.findElement(tagName("h5")).getText());
        List<WebElement> textFields = driver.findElements(tagName("label"));
        assertEquals(getString("FORM_LOGIN_EMAIL", lang), textFields.get(0).getText());
        assertEquals(getString("FORM_LOGIN_PASSWORD", lang), textFields.get(1).getText());
        assertEquals(getString("FORM_LOGIN_REGISTER", lang), driver.findElement(tagName("a")).getText());
    }

    @After
    public void tearDown() throws Exception {
        driver.quit();
        vertx.rxClose()
                .test()
                .awaitTerminalEvent(5, SECONDS)
                .assertCompleted();
    }
}
