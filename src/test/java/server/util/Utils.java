package server.util;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.Callable;

import static java.io.File.separator;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

public class Utils {

    public static WebClient getWebClient(HtmlUnitDriver driver) {
        try {
            Field client = driver.getClass().getDeclaredField("webClient");
            client.setAccessible(true);
            return ((WebClient) client.get(driver));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ChromeDriverService createDriverService() throws IOException {
        ChromeDriverService service = new ChromeDriverService.Builder()
                .usingDriverExecutable(new File(System.getProperty("user.dir") + separator + "misc" +
                        separator + "chromedriver.exe"))
                .usingAnyFreePort()
                .build();
        service.start();
        /*HtmlUnitDriver driver = new HtmlUnitDriver(BrowserVersion.CHROME, enableJavascript);
        driver.manage().timeouts().pageLoadTimeout(10, SECONDS);*/
        return service;
    }

    public static HtmlUnitDriver createDriver(boolean enableJavascript) {
        HtmlUnitDriver driver = new HtmlUnitDriver(BrowserVersion.CHROME, enableJavascript);
        driver.manage().timeouts().pageLoadTimeout(10, SECONDS);
        return driver;
    }

    public static <T> void sleep(WebDriver driver, long timeoutInSeconds, ExpectedCondition<T> condition) {
        new WebDriverWait(driver, timeoutInSeconds).until(condition);
    }

    public static Dimension getNexus5XScreenSize() {
        return new Dimension(412, 732);
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

        public static boolean isEventbus(Eventbus state, HtmlUnitDriver driver) {
            long s = ((long) driver.executeScript("return eventbus.state"));
            return s == state.state;
        }

        public static boolean isEventbus(Eventbus state, ChromeDriver driver) {
            long s = ((long) driver.executeScript("return eventbus.state"));
            return s == state.state;
        }

        public static void closeEventbus(HtmlUnitDriver driver) {
            driver.executeScript("eventbus.close();");
        }

        public static void closeEventbusAwait(ChromeDriver driver, Callable<Boolean> callable) {
            driver.executeScript("eventbus.close();");
            await().until(callable);
        }
    }

    public static void assertGoToPage(WebDriver driver, String page) {
        driver.get(page);
        assertEquals(page, driver.getCurrentUrl());
    }
}
