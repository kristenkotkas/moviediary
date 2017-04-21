package server.util;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

import static io.vertx.rxjava.core.Future.future;
import static org.junit.Assert.assertEquals;
import static org.openqa.selenium.By.tagName;

public class LoginUtils {

    public static void formLogin(WebDriver driver, String uri, JsonObject config) {
        JsonObject formAuth = config.getJsonObject("unit_test").getJsonObject("form_user");
        String url = uri + "/formlogin";
        driver.get(url);
        assertEquals(url, driver.getCurrentUrl());
        List<WebElement> fields = driver.findElements(tagName("input"));
        fields.get(0).sendKeys(formAuth.getString("username"));
        fields.get(1).sendKeys(formAuth.getString("password"));
        driver.findElement(tagName("button")).click();
    }

    public static Future<Void> asyncFormLogin(WebDriver driver, String uri, JsonObject config) {
        return future(fut -> {
            formLogin(driver, uri, config);
            fut.complete();
        });
    }
}
