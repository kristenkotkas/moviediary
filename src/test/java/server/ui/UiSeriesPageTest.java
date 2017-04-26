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
import static org.junit.Assert.assertEquals;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOf;
import static server.util.LocalDatabase.SQL_INSERT_SERIES_EPISODE;
import static server.util.LocalDatabase.SQL_INSERT_SERIES_INFO;
import static server.util.Utils.Eventbus.OPEN;
import static server.util.Utils.Eventbus.isEventbus;
import static server.util.Utils.assertGoToPage;
import static server.util.Utils.sleep;

@RunWith(VertxUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UiSeriesPageTest extends UiTest {

    private void goAndWaitForPageToLoad() {
        assertGoToPage(driver, URI + "/private/series");
        await().until(() -> isEventbus(OPEN, driver));
    }

    private void searchForSeries() {
        driver.findElementById("series-search").sendKeys("black mirror");
        driver.findElementByXPath("//label[@id='series-search-button']/i").click();
        await().ignoreExceptionsInstanceOf(NoSuchElementException.class).atMost(5, SECONDS)
                .until(() -> driver.findElementByCssSelector("span.truncate.series-search-title").isDisplayed());
        assertEquals("Black Mirror",
                driver.findElementByCssSelector("span.truncate.series-search-title").getText());
    }

    private void openSeasonView() {
        driver.findElementByCssSelector("img.series-poster").click();
        await().atMost(5, SECONDS)
                .until(() -> driver.findElementById("series-title").getText().equals("Black Mirror"));
    }

    @Test
    public void testSearchingSeriesFindsResult() throws Exception {
        goAndWaitForPageToLoad();
        searchForSeries();
    }

    @Test
    public void testClickingSeriesOpensSeasonsView() throws Exception {
        goAndWaitForPageToLoad();
        searchForSeries();
        openSeasonView();
    }

    @Test
    public void testAddingEpisodeView() throws Exception {
        goAndWaitForPageToLoad();
        searchForSeries();
        openSeasonView();
        WebElement seasonRow = driver.findElementByCssSelector("div.row.last-row");
        sleep(driver, 5, visibilityOf(seasonRow));
        seasonRow.click();
        WebElement episodeCard = driver.findElementByXPath("//div[@id='s0e1']/div/div[2]");
        sleep(driver, 5, visibilityOf(episodeCard));
        episodeCard.click();
        await().atMost(5, SECONDS).until(() -> localDatabase
                .queryBlocking("SELECT * FROM Series WHERE SeriesId = ?", new JsonArray().add(42009))
                .size() > 0);
    }

    @Test
    public void testHavingAtLeastSingleViewShowsSeriesUnderCurrentlyWatching() throws Exception {
        localDatabase.updateOrInsertBlocking(SQL_INSERT_SERIES_INFO, null);
        localDatabase.updateOrInsertBlocking(SQL_INSERT_SERIES_EPISODE, null);
        goAndWaitForPageToLoad();
        sleep(driver, 5, visibilityOf(driver.findElementById("title_42009")));
    }
}
