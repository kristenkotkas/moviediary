package server.ui;

import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.openqa.selenium.WebElement;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.openqa.selenium.By.cssSelector;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;
import static server.util.LocalDatabase.SQL_INSERT_MOVIES_HOBBIT;
import static server.util.LocalDatabase.SQL_INSERT_WISHLIST_HOBBIT;
import static server.util.Utils.Eventbus.OPEN;
import static server.util.Utils.Eventbus.isEventbus;
import static server.util.Utils.assertGoToPage;
import static server.util.Utils.sleep;

@RunWith(VertxUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UiWishlistPageTest extends UiTest {

  // TODO: 20.06.2017 needs fixing?

  private WebElement goAndWaitTillPosterVisible() {
    localDatabase.updateOrInsertBlocking(SQL_INSERT_MOVIES_HOBBIT, null);
    localDatabase.updateOrInsertBlocking(SQL_INSERT_WISHLIST_HOBBIT, null);
    assertGoToPage(driver, URI + "/private/wishlist");
    await().until(() -> isEventbus(OPEN, driver));
    sleep(driver, 5,
        visibilityOfElementLocated(cssSelector("img.series-poster.search-object-series")));
    return driver.findElementByCssSelector("img.series-poster.search-object-series");
  }

  @Test
  public void testWishlistMoviesAreLoaded() throws Exception {
    goAndWaitTillPosterVisible();
  }

  @Test
  public void testClickingMovieRedirectsToMoviePage() throws Exception {
    goAndWaitTillPosterVisible().click();
    assertEquals(URI + "/private/movies/?id=49051", driver.getCurrentUrl());
  }
}
