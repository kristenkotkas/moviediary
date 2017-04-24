package server.ui;

import io.vertx.core.json.JsonArray;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertTrue;
import static org.openqa.selenium.By.id;
import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOf;
import static server.util.Utils.Eventbus.OPEN;
import static server.util.Utils.Eventbus.isEventbus;
import static server.util.Utils.assertGoToPage;
import static server.util.Utils.sleep;

@RunWith(VertxUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UiMoviesPageTest extends UiTest {

    private void goAndWaitForPageToLoad() {
        assertGoToPage(driver, URI + "/private/movies");
        await().until(() -> isEventbus(OPEN, driver));
    }

    private void searchForMovie() {
        driver.findElementById("search").sendKeys("ghost in the shell");
        driver.findElementByXPath("//label[@id='search-button']/i").click();
        await().ignoreExceptionsInstanceOf(NoSuchElementException.class).atMost(5, SECONDS)
                .until(() -> driver.findElementByCssSelector("div.card-content.truncate").isDisplayed());
        assertTrue(driver.findElementByCssSelector("div.card-content.truncate")
                .getText().contains("Ghost in the Shell"));
    }

    private void openDetailView() {
        driver.findElementByCssSelector("div.card-content.truncate").click();
        await().atMost(5, SECONDS)
                .until(() -> driver.findElementById("movie-title").getText().equals("Ghost in the Shell"));
    }

    @Test
    public void testSearchingMovieFindsResult() throws Exception {
        goAndWaitForPageToLoad();
        searchForMovie();
    }

    @Test
    public void testOpeningDetailView() throws Exception {
        goAndWaitForPageToLoad();
        searchForMovie();
        openDetailView();
    }

    @Test
    public void testAddingToWishlist() throws Exception {
        goAndWaitForPageToLoad();
        searchForMovie();
        openDetailView();
        WebElement addToWishlistButton = driver.findElementById("wishlist-text");
        sleep(driver, 5, visibilityOf(addToWishlistButton));
        sleep(driver, 5, elementToBeClickable(addToWishlistButton));
        addToWishlistButton.click();
        await().atMost(5, SECONDS).until(() -> localDatabase
                .queryBlocking("SELECT * FROM Wishlist WHERE MovieId = ?", new JsonArray().add(315837))
                .size() > 0);
    }

    @Test
    public void testAddingView() throws Exception {
        goAndWaitForPageToLoad();
        searchForMovie();
        openDetailView();
        WebElement addToViewsButton = driver.findElementByCssSelector("#add-watch > div.card-content");
        sleep(driver, 5, visibilityOf(addToViewsButton));
        addToViewsButton.click();
        WebElement watchStartNowButton = driver.findElementById("watchStartNow");
        sleep(driver, 5, visibilityOf(watchStartNowButton));
        watchStartNowButton.click();
        driver.findElement(id("watchEndCalculate")).click();
        driver.findElement(id("add-btn")).click();
        await().atMost(5, SECONDS).until(() -> localDatabase
                .queryBlocking("SELECT * FROM Views WHERE MovieId = ?", new JsonArray().add(315837))
                .size() > 0);
    }
}
