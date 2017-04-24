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
import static org.junit.Assert.assertTrue;
import static org.openqa.selenium.By.id;
import static org.openqa.selenium.By.tagName;
import static server.entity.Language.getString;
import static server.util.Utils.assertGoToPage;

@RunWith(VertxUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UiLoginPageTest extends UiTest {

    @Before
    @Override
    public void setUp() throws Exception {
        localDatabase.resetCleanStateBlocking();
        driver = new ChromeDriver(service);
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

    @Test
    public void testFacebookLogin() throws Exception {
        driver.manage().deleteAllCookies();
        JsonObject fbAuth = config.getJsonObject("unit_test").getJsonObject("facebook_user");
        assertGoToPage(driver, URI + "/login");
        driver.findElements(tagName("a")).get(1).click();
        assertTrue(driver.getCurrentUrl().contains("facebook"));
        driver.findElement(id("email")).sendKeys(fbAuth.getString("username"));
        driver.findElement(id("pass")).sendKeys(fbAuth.getString("password"));
        driver.findElement(id("loginbutton")).click();
        assertEquals(URI + "/private/home?client_name=FacebookClient#_=_", driver.getCurrentUrl());
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
        assertEquals(getString("LOGIN_PASSWORD", lang).toUpperCase(ENGLISH), loginButtons.get(0).getText());
        assertEquals(getString("LOGIN_FACEBOOK", lang).toUpperCase(ENGLISH), loginButtons.get(1).getText());
        assertEquals(getString("LOGIN_GOOGLE", lang).toUpperCase(ENGLISH), loginButtons.get(2).getText());
        assertEquals(getString("LOGIN_IDCARD", lang).toUpperCase(ENGLISH), loginButtons.get(3).getText());
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
}
