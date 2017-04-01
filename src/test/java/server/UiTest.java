package server;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import server.verticle.ServerVerticle;

import java.util.List;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;
import static org.junit.Assert.assertEquals;
import static org.openqa.selenium.By.tagName;
import static server.entity.Language.getString;
import static server.util.FileUtils.getConfig;
import static server.util.NetworkUtils.HTTP_PORT;

/**
 * @author <a href="https://bitbucket.org/kristjanhk/">Kristjan Hendrik Küngas</a>
 */
@RunWith(VertxUnitRunner.class)
public class UiTest {
    public static final String SESSION_COOKIE = "vertx-web.session";
    private static final int PORT = 8082;
    private static final String URI = "http://localhost:" + PORT;

    private Vertx vertx;
    private WebDriver driver;

    @Before
    public void setUp(TestContext ctx) throws Exception {
        driver = new HtmlUnitDriver();
        vertx = Vertx.vertx();
        JsonObject config = getConfig(null).put(HTTP_PORT, PORT);
        vertx.deployVerticle(new ServerVerticle(), new DeploymentOptions().setConfig(config), ctx.asyncAssertSuccess());
    }

    @Test
    public void testLoginPage() throws Exception {
        driver.get(URI + "/login");
        List<WebElement> loginButtons = driver.findElements(tagName("a"));

        //login client urls
        assertEquals(URI + "/private/home?client_name=FormClient",
                escapeHtml4(loginButtons.get(0).getAttribute("href")));
        assertEquals(URI + "/private/home?client_name=FacebookClient",
                escapeHtml4(loginButtons.get(1).getAttribute("href")));
        assertEquals(URI + "/private/home?client_name=Google2Client",
                escapeHtml4(loginButtons.get(2).getAttribute("href")));
        assertEquals(URI + "/private/home?client_name=IdCardClient",
                escapeHtml4(loginButtons.get(3).getAttribute("href")));

        //language buttons
        assertEquals(URI + "/login?lang=en",
                escapeHtml4(loginButtons.get(4).getAttribute("href")));
        assertEquals(URI + "/login?lang=et",
                escapeHtml4(loginButtons.get(5).getAttribute("href")));
        assertEquals(URI + "/login?lang=de",
                escapeHtml4(loginButtons.get(6).getAttribute("href")));
        testLoginPageLangugage("en");
        testLoginPageLangugage("et");
        testLoginPageLangugage("de");
    }

    public void testLoginPageLangugage(String lang) {
        driver.get(URI + "/login?lang=" + lang + "&message=LOGIN_VERIFIED");
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

    @After
    public void tearDown(TestContext ctx) throws Exception {
        driver.quit();
        vertx.close(ctx.asyncAssertSuccess());
    }

    private void login(Handler handler) {
        // TODO: 1.04.2017 log in and then call handler
    }
}
