package server.ui;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.openqa.selenium.By;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
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
}
