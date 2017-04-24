package server.ui;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.openqa.selenium.By.tagName;
import static server.util.Utils.Eventbus.OPEN;
import static server.util.Utils.Eventbus.isEventbus;
import static server.util.Utils.assertGoToPage;

@RunWith(VertxUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UiMePageTest extends UiTest {

    @Test
    public void testMessageIsPushedToEverybody(TestContext ctx) throws Exception {
        String message = "Hello world!";
        Async async = ctx.async();
        vertx.eventBus().consumer("messenger", msg -> {
            assertEquals(message, msg.body());
            await().until(() -> driver.findElement(By.id("toast-container")).getText().contains(message));
            async.complete();
        });
        assertGoToPage(driver, URI + "/private/user");
        await().until(() -> isEventbus(OPEN, driver));
        driver.findElement(By.id("MessageInput")).sendKeys(message);
        driver.findElement(By.id("SendMessage")).click();
    }

    @Test
    public void testMyDetailsAreCorrect() throws Exception {
        JsonObject details = localDatabase.query("SELECT * FROM Users " +
                "JOIN Settings ON Users.Username = Settings.Username " +
                "WHERE Users.Username = ?", new JsonArray()
                .add(config.getJsonObject("unit_test").getJsonObject("form_user").getString("username")))
                .rxSetHandler().toBlocking()
                .value().getJsonObject(0);
        assertGoToPage(driver, URI + "/private/user");
        await().until(() -> isEventbus(OPEN, driver));
        await().until(() -> driver.findElements(tagName("td")).size() > 0);
        List<WebElement> data = driver.findElements(tagName("td"));
        assertTrue(data.get(1).getText().equals(String.valueOf(details.getInteger("Id"))));
        assertTrue(data.get(3).getText().equals(details.getString("Firstname")));
        assertTrue(data.get(5).getText().equals(details.getString("Lastname")));
        assertTrue(data.get(7).getText().equals(details.getString("Username")));
        assertTrue(data.get(9).getText().equals(details.getString("RuntimeType")));
        assertTrue(data.get(11).getText().equals(details.getString("Verified").equals("1") ? "true" : "false"));
    }
}
