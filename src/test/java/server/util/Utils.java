package server.util;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import java.lang.reflect.Field;

import static java.util.concurrent.TimeUnit.SECONDS;
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

    public static HtmlUnitDriver createDriver(boolean enableJavascript) {
        HtmlUnitDriver driver = new HtmlUnitDriver(BrowserVersion.CHROME, enableJavascript);
        driver.manage().timeouts().pageLoadTimeout(10, SECONDS);
        return driver;
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

        public static void closeEventbus(HtmlUnitDriver driver) {
            driver.executeScript("eventbus.close();");
        }
    }

    public static void assertGoToPage(WebDriver driver, String page) {
        driver.get(page);
        assertEquals(page, driver.getCurrentUrl());
    }
}
