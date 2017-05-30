package server.ui;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import static org.junit.Assert.assertEquals;
import static org.openqa.selenium.By.tagName;
import static server.entity.Language.getString;
import static server.util.Utils.assertGoToPage;

@RunWith(VertxUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UiHomePageTest extends UiTest {

    @Test
    public void testHomePageTranslations() throws Exception {
        checkHomePageTranslations("en");
        checkHomePageTranslations("et");
        checkHomePageTranslations("de");
    }

    private void checkHomePageTranslations(String lang) {
        JsonObject formAuth = config.getJsonObject("unit_test").getJsonObject("form_user");
        assertGoToPage(driver, URI + "/login?lang=" + lang);
        assertGoToPage(driver, URI + "/private/home");
        assertEquals(getString("HOME_HELLO", lang) + " " + formAuth.getString("firstname"),
                driver.findElement(tagName("h3")).getText());
    }

    // TODO: 30.05.2017 tests for cards
}