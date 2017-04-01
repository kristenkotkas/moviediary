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
public class UiFormLoginPageTest {
    private static final int PORT = 8082;
    private static final String URI = "http://localhost:" + PORT;

    private Vertx vertx;
    private JsonObject config;
    private WebDriver driver;

    @Before
    public void setUp(TestContext ctx) throws Exception {
        driver = new HtmlUnitDriver();
        vertx = Vertx.vertx();
        config = getConfig().put(HTTP_PORT, PORT);
        config.getJsonObject("oauth").put("localCallback", URI + "/callback");
        vertx.deployVerticle(new ServerVerticle(), new DeploymentOptions().setConfig(config), ctx.asyncAssertSuccess());
    }

    @Test
    public void testLoginUnauthorized() throws Exception {
        login("megalamp", "ultrateam3000");
        assertEquals(URI + "/login?message=AUTHORIZER_UNAUTHORIZED", driver.getCurrentUrl());
    }

    @Test
    public void testLoginAuthorizedToHomePage() throws Exception {
        JsonObject user = config.getJsonObject("unit_test");
        login(user.getString("username"), user.getString("password"));
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

    private void login(String username, String password) {
        String url = URI + "/formlogin";
        driver.get(url);
        assertEquals(url, driver.getCurrentUrl());
        List<WebElement> fields = driver.findElements(tagName("input"));
        fields.get(0).sendKeys(username);
        fields.get(1).sendKeys(password);
        driver.findElement(tagName("button")).click();
    }

    public void testFormLoginPageLanguage(String lang) {
        String url = URI + "/login?lang=" + lang;
        driver.get(url);
        assertEquals(url, driver.getCurrentUrl());
        checkFormLoginPageLanguage("en");
        checkFormLoginPageLanguage("et");
        checkFormLoginPageLanguage("de");
    }

    private void checkFormLoginPageLanguage(String lang) {
        String url = URI + "/formlogin?";
        driver.get(url);
        assertEquals(url, driver.getCurrentUrl());
        assertEquals(getString("FORM_LOGIN_TITLE", lang), driver.getTitle());
        assertEquals(getString("FORM_LOGIN_TITLE", lang), driver.findElement(tagName("h5")).getText());
        List<WebElement> textFields = driver.findElements(tagName("label"));
        assertEquals(getString("FORM_LOGIN_EMAIL", lang), textFields.get(0).getText());
        assertEquals(getString("FORM_LOGIN_PASSWORD", lang), textFields.get(1).getText());
        assertEquals(getString("FORM_LOGIN_REGISTER", lang), driver.findElement(tagName("a")).getText());
    }

    @After
    public void tearDown(TestContext ctx) throws Exception {
        driver.quit();
        vertx.close(ctx.asyncAssertSuccess());
    }
}
