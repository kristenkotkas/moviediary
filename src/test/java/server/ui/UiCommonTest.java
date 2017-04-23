package server.ui;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.core.Vertx;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import server.util.LocalDatabase;
import server.verticle.ServerVerticle;

import java.util.List;

import static io.vertx.rxjava.core.RxHelper.deployVerticle;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.openqa.selenium.By.tagName;
import static server.entity.Language.getString;
import static server.util.FileUtils.getConfig;
import static server.util.LocalDatabase.initializeDatabase;
import static server.util.LoginUtils.asyncFormLogin;
import static server.util.LoginUtils.formLogin;
import static server.util.NetworkUtils.HTTP_PORT;
import static server.util.Utils.assertGoToPage;
import static server.util.Utils.createDriver;

@SuppressWarnings("Duplicates")
@RunWith(VertxUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UiCommonTest {
    private static final int PORT = 8082;
    private static final String URI = "http://localhost:" + PORT;

    private static io.vertx.rxjava.core.Vertx vertx;
    private static JsonObject config;
    private static HtmlUnitDriver driver;
    private static LocalDatabase hsqldb;

    @BeforeClass
    public static void setUp(TestContext ctx) throws Exception {
        driver = createDriver(true);
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
    public void testUnauthorizedIsRedirectedToLoginAndBackAfterLogin() throws Exception {
        driver.manage().deleteAllCookies();
        String url = URI + "/private/movies";
        driver.get(url);
        assertEquals(URI + "/login", driver.getCurrentUrl());
        formLogin(driver, URI, config);
        assertEquals(url, driver.getCurrentUrl());
    }

    @Test
    public void testNavBarLinks() throws Exception {
        assertGoToPage(driver, URI + "/private/home");
        List<WebElement> links = driver.findElements(tagName("a"));
        //top
        assertEquals(URI + "/private/user", links.get(0).getAttribute("href"));
        assertEquals(URI + "/private/home", links.get(1).getAttribute("href"));
        assertEquals(URI + "/private/movies", links.get(2).getAttribute("href"));
        assertEquals(URI + "/private/history", links.get(3).getAttribute("href"));
        assertEquals(URI + "/private/statistics", links.get(4).getAttribute("href"));
        assertEquals(URI + "/private/wishlist", links.get(5).getAttribute("href"));
        //mobile
        assertEquals(URI + "/private/user", links.get(6).getAttribute("href"));
        assertEquals(URI + "/private/home", links.get(7).getAttribute("href"));
        assertEquals(URI + "/private/movies", links.get(8).getAttribute("href"));
        assertEquals(URI + "/private/history", links.get(9).getAttribute("href"));
        assertEquals(URI + "/private/statistics", links.get(10).getAttribute("href"));
        assertEquals(URI + "/private/wishlist", links.get(11).getAttribute("href"));
    }

    @Test
    public void testNavBarLinksCanGetToPages() throws Exception {
        assertGoToPage(driver, URI + "/private/home");
        //top
        checkCanGetToPage(driver.findElements(tagName("a")).get(0), URI + "/private/user");
        checkCanGetToPage(driver.findElements(tagName("a")).get(1), URI + "/private/home");
        checkCanGetToPage(driver.findElements(tagName("a")).get(2), URI + "/private/movies");
        checkCanGetToPage(driver.findElements(tagName("a")).get(3), URI + "/private/history");
        checkCanGetToPage(driver.findElements(tagName("a")).get(4), URI + "/private/statistics");
        checkCanGetToPage(driver.findElements(tagName("a")).get(5), URI + "/private/wishlist");
        //mobile
        checkCanGetToPage(driver.findElements(tagName("a")).get(6), URI + "/private/user");
        checkCanGetToPage(driver.findElements(tagName("a")).get(7), URI + "/private/home");
        checkCanGetToPage(driver.findElements(tagName("a")).get(8), URI + "/private/movies");
        checkCanGetToPage(driver.findElements(tagName("a")).get(9), URI + "/private/history");
        checkCanGetToPage(driver.findElements(tagName("a")).get(10), URI + "/private/statistics");
        checkCanGetToPage(driver.findElements(tagName("a")).get(11), URI + "/private/wishlist");
    }

    private void checkCanGetToPage(WebElement link, String urlToCheck) {
        link.click();
        assertEquals(urlToCheck, driver.getCurrentUrl());
    }

    // TODO: 23.04.2017 switch to tooltips
    @Test
    public void testNavBarTranslations() throws Exception {
        assertGoToPage(driver, URI + "/private/home");
        JsonObject formAuth = config.getJsonObject("unit_test").getJsonObject("form_user");
        assertEquals(formAuth.getString("firstname") + " " + formAuth.getString("lastname"),
                driver.findElements(tagName("a")).get(6).getText());
        checkNavBarTranslations("en");
        checkNavBarTranslations("et");
        checkNavBarTranslations("de");
    }

    private void checkNavBarTranslations(String lang) {
        assertGoToPage(driver, URI + "/login?lang=" + lang);
        assertGoToPage(driver, URI + "/private/home");
        List<WebElement> links = driver.findElements(tagName("a"));
        //top
        assertEquals(getString("NAVBAR_ME", lang), links.get(0).getText());
        assertEquals(getString("NAVBAR_HOME", lang), links.get(1).getText());
        assertEquals(getString("NAVBAR_MOVIES", lang), links.get(2).getText());
        assertEquals(getString("NAVBAR_HISTORY", lang), links.get(3).getText());
        assertEquals(getString("NAVBAR_STATISTICS", lang), links.get(4).getText());
        assertEquals(getString("NAVBAR_WISHLIST", lang), links.get(5).getText());
        //mobile
        assertEquals(getString("NAVBAR_HOME", lang), links.get(7).getText());
        assertEquals(getString("NAVBAR_MOVIES", lang), links.get(8).getText());
        assertEquals(getString("NAVBAR_HISTORY", lang), links.get(9).getText());
        assertEquals(getString("NAVBAR_STATISTICS", lang), links.get(10).getText());
        assertEquals(getString("NAVBAR_WISHLIST", lang), links.get(11).getText());
    }

    @Test
    public void testNotFoundPageLinks() throws Exception {
        assertGoToPage(driver, URI + "/somethingRandom");
        assertEquals(URI + "/private/home", driver.findElement(tagName("a")).getAttribute("href"));
    }

    @Test
    public void testNotFoundPage() throws Exception {
        checkNotFoundPageTranslations("en");
        checkNotFoundPageTranslations("et");
        checkNotFoundPageTranslations("de");
    }

    private void checkNotFoundPageTranslations(String lang) {
        assertGoToPage(driver, URI + "/login?lang=" + lang);
        assertGoToPage(driver, URI + "/somethingRandom");
        assertEquals(getString("NOTFOUND_TITLE", lang), driver.getTitle());
        assertEquals("404: " + getString("NOTFOUND_TITLE", lang),
                driver.findElement(tagName("h3")).getText());
        assertEquals(getString("NOTFOUND_RETURN", lang), driver.findElement(tagName("a")).getText());
    }

    @AfterClass
    public static void tearDown(TestContext ctx) throws Exception {
        driver.quit();
        vertx.close(ctx.asyncAssertSuccess());
    }
}
