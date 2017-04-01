package server.util;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.openqa.selenium.By.tagName;

public class LoginUtils {

    public static void formLogin(WebDriver driver, String uri, String username, String password) {
        String url = uri + "/formlogin";
        driver.get(url);
        assertEquals(url, driver.getCurrentUrl());
        List<WebElement> fields = driver.findElements(tagName("input"));
        fields.get(0).sendKeys(username);
        fields.get(1).sendKeys(password);
        driver.findElement(tagName("button")).click();
    }
}
