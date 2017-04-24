package server.util;

import io.vertx.core.json.JsonObject;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.openqa.selenium.By.id;
import static org.openqa.selenium.By.tagName;
import static org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOf;
import static server.util.Utils.sleep;

public class LoginUtils {

    public static void formLogin(WebDriver driver, String uri, JsonObject config) {
        JsonObject user = config.getJsonObject("unit_test").getJsonObject("form_user");
        Utils.assertGoToPage(driver, uri + "/formlogin");
        List<WebElement> fields = driver.findElements(tagName("input"));
        sleep(driver, 5, invisibilityOf(driver.findElement(id("loader-wrapper"))));
        fields.get(0).sendKeys(user.getString("username"));
        fields.get(1).sendKeys(user.getString("password"));
        driver.findElement(tagName("button")).click();
    }
}
