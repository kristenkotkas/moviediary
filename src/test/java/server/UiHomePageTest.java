package server;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import server.verticle.ServerVerticle;

import static org.junit.Assert.assertEquals;
import static org.openqa.selenium.By.tagName;
import static server.entity.Language.getString;
import static server.util.FileUtils.getConfig;
import static server.util.LoginUtils.formLogin;
import static server.util.NetworkUtils.HTTP_PORT;

@RunWith(VertxUnitRunner.class)
public class UiHomePageTest {
    private static final int PORT = 8082;
    private static final String URI = "http://localhost:" + PORT;

    private Vertx vertx;
    private JsonObject auth;
    private WebDriver driver;

    @SuppressWarnings("Duplicates")
    @Before
    public void setUp(TestContext ctx) throws Exception {
        driver = new HtmlUnitDriver();
        vertx = Vertx.vertx();
        JsonObject config = getConfig().put(HTTP_PORT, PORT);
        config.getJsonObject("oauth").put("localCallback", URI + "/callback");
        auth = config.getJsonObject("unit_test_user");
        vertx.deployVerticle(new ServerVerticle(), new DeploymentOptions().setConfig(config), ctx.asyncAssertSuccess());
        formLogin(driver, URI, auth.getString("username"), auth.getString("password"));
    }

    @Test
    public void testHomePageTranslations() throws Exception {
        checkHomePageTranslations("en");
        checkHomePageTranslations("et");
        checkHomePageTranslations("de");
    }

    private void checkHomePageTranslations(String lang) {
        String url = URI + "/login?lang=" + lang;
        driver.get(url);
        assertEquals(url, driver.getCurrentUrl());
        url = URI + "/private/home";
        driver.get(url);
        assertEquals(url, driver.getCurrentUrl());
        assertEquals(getString("HOME_HELLO", lang) + " " + auth.getString("firstname"),
                driver.findElement(tagName("h3")).getText());
    }


    @After
    public void tearDown(TestContext ctx) throws Exception {
        driver.quit();
        vertx.close(ctx.asyncAssertSuccess());
    }
}
