package server.ui;

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
import static server.util.LoginUtils.formLogin;
import static server.util.Utils.assertGoToPage;

@RunWith(VertxUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UiFormRegisterPageTest extends UiTest {
    // TODO: 23.04.2017 register test

    @Before
    @Override
    public void setUp() throws Exception {
        localDatabase.resetCleanStateBlocking();
        driver = new ChromeDriver(service);
        formLogin(driver, URI, config);
    }

    @Test
    public void testFormRegisterPageLinks() throws Exception {
        assertGoToPage(driver, URI + "/formregister");
        assertEquals(URI + "/public/api/v1/users/form/insert",
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
        assertEquals(getString("FORM_LOGIN_REGISTER", lang).toUpperCase(ENGLISH),
                driver.findElement(tagName("button")).getText());
    }
}
