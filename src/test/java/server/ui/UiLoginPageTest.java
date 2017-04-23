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
import static org.junit.Assert.assertTrue;
import static org.openqa.selenium.By.id;
import static org.openqa.selenium.By.tagName;
import static server.entity.Language.getString;
import static server.util.CommonUtils.ifPresent;
import static server.util.FileUtils.getConfig;
import static server.util.LocalDatabase.initializeDatabase;
import static server.util.NetworkUtils.HTTP_PORT;
import static server.util.Utils.*;

@SuppressWarnings("Duplicates")
@RunWith(VertxUnitRunner.class)
public class UiLoginPageTest {
    private static final int PORT = 8082;
    private static final String URI = "http://localhost:" + PORT;

    private static Vertx vertx;
    private static JsonObject auth;
    private static HtmlUnitDriver driver;
    private static LocalDatabase localDatabase;

    @BeforeClass
    public static void setUp(TestContext ctx) throws Exception {
        driver = createDriver(false);
        ifPresent(getWebClient(driver), c -> c.getOptions().setThrowExceptionOnScriptError(false));
        vertx = Vertx.vertx();
        JsonObject config = getConfig().put(HTTP_PORT, PORT);
        config.getJsonObject("oauth").put("localCallback", URI + "/callback");
        auth = config.getJsonObject("unit_test");
        initializeDatabase(vertx, config.getJsonObject("mysql")).rxSetHandler()
                .doOnSuccess(db -> localDatabase = db)
                .doOnError(ctx::fail)
                .flatMap(db -> deployVerticle(vertx, new ServerVerticle(), new DeploymentOptions()
                        .setConfig(config))
                        .toSingle())
                .test()
                .awaitTerminalEvent(10, SECONDS)
                .assertCompleted();
    }

    @Test
    public void testLoginPageLinks() throws Exception {
        assertGoToPage(driver, URI + "/login");
        List<WebElement> loginButtons = driver.findElements(tagName("a"));
        assertEquals(URI + "/private/home?client_name=FormClient",
                escapeHtml4(loginButtons.get(0).getAttribute("href")));
        assertEquals(URI + "/private/home?client_name=FacebookClient",
                escapeHtml4(loginButtons.get(1).getAttribute("href")));
        assertEquals(URI + "/private/home?client_name=Google2Client",
                escapeHtml4(loginButtons.get(2).getAttribute("href")));
        assertEquals(URI + "/private/home?client_name=IdCardClient",
                escapeHtml4(loginButtons.get(3).getAttribute("href")));
    }

    @Test
    public void testLoginPageButtons() throws Exception {
        assertGoToPage(driver, URI + "/login");
        List<WebElement> loginButtons = driver.findElements(tagName("a"));
        assertEquals(URI + "/login?lang=en",
                escapeHtml4(loginButtons.get(4).getAttribute("href")));
        assertEquals(URI + "/login?lang=et",
                escapeHtml4(loginButtons.get(5).getAttribute("href")));
        assertEquals(URI + "/login?lang=de",
                escapeHtml4(loginButtons.get(6).getAttribute("href")));
    }

    // FIXME: 23.04.2017 redirected to wrong page if using javascript
    @Test
    public void testFacebookLogin() throws Exception {
        driver.manage().deleteAllCookies();
        JsonObject fbAuth = auth.getJsonObject("facebook_user");
        assertGoToPage(driver, URI + "/login");
        driver.findElements(tagName("a")).get(1).click();
        assertTrue(driver.getCurrentUrl().contains("facebook"));
        driver.findElement(id("email")).sendKeys(fbAuth.getString("username"));
        driver.findElement(id("pass")).sendKeys(fbAuth.getString("password"));
        driver.findElement(id("loginbutton")).click();
        assertEquals(URI + "/private/home?client_name=FacebookClient", driver.getCurrentUrl());
        driver.manage().deleteAllCookies();
    }

    @Test
    public void testFromLoginPageCanGetToFormLoginPage() throws Exception {
        assertGoToPage(driver, URI + "/login?lang=en");
        driver.findElement(tagName("a")).click();
        assertEquals(getString("FORM_LOGIN_TITLE", "en"), driver.getTitle());
    }

    @Test
    public void testLoginPageTranslations() throws Exception {
        checkLoginPageTranslations("en");
        checkLoginPageTranslations("et");
        checkLoginPageTranslations("de");
        checkLoginPageVerifyEmailTranslation("en");
        checkLoginPageVerifyEmailTranslation("et");
        checkLoginPageVerifyEmailTranslation("de");
        checkLoginPageUnauthorizedTranslation("en");
        checkLoginPageUnauthorizedTranslation("et");
        checkLoginPageUnauthorizedTranslation("de");
    }

    private void checkLoginPageTranslations(String lang) {
        assertGoToPage(driver, URI + "/login?lang=" + lang + "&message=LOGIN_VERIFIED");
        assertEquals(getString("LOGIN_TITLE", lang), driver.getTitle());
        List<WebElement> messages = driver.findElements(tagName("h5"));
        assertEquals(getString("LOGIN_TITLE", lang), messages.get(0).getText());
        assertEquals(getString("LOGIN_VERIFIED", lang), messages.get(1).getText());
        List<WebElement> loginButtons = driver.findElements(tagName("a"));
        assertEquals(getString("LOGIN_PASSWORD", lang), loginButtons.get(0).getText());
        assertEquals(getString("LOGIN_FACEBOOK", lang), loginButtons.get(1).getText());
        assertEquals(getString("LOGIN_GOOGLE", lang), loginButtons.get(2).getText());
        assertEquals(getString("LOGIN_IDCARD", lang), loginButtons.get(3).getText());
    }

    private void checkLoginPageVerifyEmailTranslation(String lang) {
        assertGoToPage(driver, URI + "/login?lang=" + lang + "&message=FORM_REGISTER_VERIFY_EMAIL");
        assertEquals(getString("FORM_REGISTER_VERIFY_EMAIL", lang),
                driver.findElements(tagName("h5")).get(1).getText());
    }

    private void checkLoginPageUnauthorizedTranslation(String lang) {
        assertGoToPage(driver, URI + "/login?lang=" + lang + "&message=AUTHORIZER_UNAUTHORIZED");
        assertEquals(getString("AUTHORIZER_UNAUTHORIZED", lang),
                driver.findElements(tagName("h5")).get(1).getText());
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
