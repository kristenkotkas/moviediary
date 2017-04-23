package server.util;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

import static io.vertx.rxjava.core.Future.succeededFuture;
import static org.openqa.selenium.By.tagName;

public class LoginUtils {

    public static void formLogin(WebDriver driver, String uri, JsonObject config) {
        JsonObject user = config.getJsonObject("unit_test").getJsonObject("form_user");
        Utils.assertGoToPage(driver, uri + "/formlogin");
        List<WebElement> fields = driver.findElements(tagName("input"));
        fields.get(0).sendKeys(user.getString("username"));
        fields.get(1).sendKeys(user.getString("password"));
        driver.findElement(tagName("button")).click();
    }

    public static Future<Void> asyncFormLogin(WebDriver driver, String uri, JsonObject config) {
        formLogin(driver, uri, config);
        return succeededFuture();
    }
}
