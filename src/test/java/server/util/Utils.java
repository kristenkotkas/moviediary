package server.util;

import io.vertx.core.json.JsonObject;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static java.io.File.separator;
import static org.junit.Assert.assertEquals;
import static org.openqa.selenium.By.id;
import static org.openqa.selenium.By.tagName;
import static org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOf;

public class Utils {

    public static void formLogin(WebDriver driver, String uri, JsonObject config) {
        JsonObject user = config.getJsonObject("unit_test").getJsonObject("form_user");
        Utils.assertGoToPage(driver, uri + "/formlogin");
        List<WebElement> fields = driver.findElements(tagName("input"));
        sleep(driver, 5, invisibilityOf(driver.findElement(id("loader-wrapper"))));
        fields.get(0).sendKeys(user.getString("username"));
        fields.get(1).sendKeys(user.getString("password"));
        driver.findElement(tagName("button")).click();
    }

    public static ChromeDriverService createDriverService() throws IOException {
        ChromeDriverService service = new ChromeDriverService.Builder()
                .usingDriverExecutable(new File(System.getProperty("user.dir") + separator + "misc" +
                        separator + "chromedriver.exe"))
                .usingAnyFreePort()
                .build();
        service.start();
        return service;
    }

    public static <T> void sleep(WebDriver driver, long timeoutInSeconds, ExpectedCondition<T> condition) {
        new WebDriverWait(driver, timeoutInSeconds).until(condition);
    }

    public static Dimension getNexus5XScreenSize() {
        return new Dimension(412, 732);
    }

    public static void assertGoToPage(WebDriver driver, String page) {
        driver.get(page);
        assertEquals(page, driver.getCurrentUrl());
    }

    public enum Eventbus {
        CONNECTING(0),
        OPEN(1),
        CLOSING(2),
        CLOSED(3);

        private final int state;

        Eventbus(int state) {
            this.state = state;
        }

        public static boolean isEventbus(Eventbus state, ChromeDriver driver) {
            return state.state == ((long) driver.executeScript("return eventbus.state"));
        }
    }
}
