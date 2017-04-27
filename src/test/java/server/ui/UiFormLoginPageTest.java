package server.ui;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.List;

import static java.util.Locale.ENGLISH;
import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;
import static org.junit.Assert.assertEquals;
import static org.openqa.selenium.By.tagName;
import static server.entity.Language.getString;
import static server.util.Utils.assertGoToPage;
import static server.util.Utils.formLogin;

@RunWith(VertxUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UiFormLoginPageTest extends UiTest {

    @Before
    @Override
    public void setUp() throws Exception {
        localDatabase.resetCleanStateBlocking();
        driver = new ChromeDriver(service);
    }

    @Test
    public void testLoginUnauthorized() throws Exception {
        driver.manage().deleteAllCookies();
        formLogin(driver, URI, new JsonObject().put("unit_test", new JsonObject().put("form_user",
                new JsonObject().put("username", "megalamp@test.ee").put("password", "ultrateam3000"))));
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
        assertGoToPage(driver, URI + "/formlogin");
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
        assertGoToPage(driver, URI + "/login?lang=" + lang);
        assertGoToPage(driver, URI + "/formlogin");
        assertEquals(getString("FORM_LOGIN_TITLE", lang), driver.getTitle());
        assertEquals(getString("LOGIN_TITLE", lang), driver.findElement(tagName("h5")).getText());
        List<WebElement> textFields = driver.findElements(tagName("label"));
        assertEquals(getString("FORM_LOGIN_EMAIL", lang), textFields.get(0).getText());
        assertEquals(getString("FORM_LOGIN_PASSWORD", lang), textFields.get(1).getText());
        assertEquals(getString("LOGIN_TITLE", lang).toUpperCase(ENGLISH),
                driver.findElement(tagName("button")).getText());
        assertEquals(getString("FORM_LOGIN_REGISTER", lang).toUpperCase(ENGLISH),
                driver.findElement(tagName("a")).getText());
    }
}
