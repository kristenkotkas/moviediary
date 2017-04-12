package server;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
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

@RunWith(VertxUnitRunner.class)
public class UiFormRegisterPageTest {
    private static final int PORT = 8082;
    private static final String URI = "http://localhost:" + PORT;

    private Vertx vertx;
    private WebDriver driver;

    @Before
    public void setUp(TestContext ctx) throws Exception {
        driver = new HtmlUnitDriver();
        vertx = Vertx.vertx();
        vertx.deployVerticle(new ServerVerticle(), new DeploymentOptions().setConfig(getConfig().put(HTTP_PORT, PORT)),
                ctx.asyncAssertSuccess());
    }

    @Test
    public void testFormRegisterPageLinks() throws Exception {
        String url = URI + "/formregister";
        driver.get(url);
        assertEquals(url, driver.getCurrentUrl());
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
        String url = URI + "/login?lang=" + lang;
        driver.get(url);
        assertEquals(url, driver.getCurrentUrl());
        url = URI + "/formregister?message=FORM_REGISTER_EXISTS";
        driver.get(url);
        assertEquals(url, driver.getCurrentUrl());
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

    @After
    public void tearDown(TestContext ctx) throws Exception {
        driver.quit();
        vertx.close(ctx.asyncAssertSuccess());
    }
}
