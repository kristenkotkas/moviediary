package server.demo;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import server.ui.UiTest;

import java.util.List;

import static java.util.Locale.ENGLISH;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.openqa.selenium.By.id;
import static org.openqa.selenium.By.tagName;
import static org.openqa.selenium.support.ui.ExpectedConditions.*;
import static server.entity.Language.getString;
import static server.util.Utils.Eventbus.OPEN;
import static server.util.Utils.Eventbus.isEventbus;
import static server.util.Utils.*;

@SuppressWarnings("Duplicates")
@RunWith(VertxUnitRunner.class)
public class DemoUiTest extends UiTest {

    @Before
    public void setUp() throws Exception {
        localDatabase.resetCleanStateBlocking();
        driver = new ChromeDriver(service);
        driver.manage().window().setPosition(new Point(200, 0));
        formLogin(driver, URI, config);
    }

    @Test
    public void testNavBarLinksCanGetToPages() throws Exception {
        assertGoToPage(driver, URI + "/private/home");
        driver.manage().window().maximize();
        //top
        checkCanGetToPage(driver.findElements(tagName("a")).get(0), URI + "/private/user");
        checkCanGetToPage(driver.findElements(tagName("a")).get(1), URI + "/private/home");
        checkCanGetToPage(driver.findElements(tagName("a")).get(2), URI + "/private/movies");
        checkCanGetToPage(driver.findElements(tagName("a")).get(3), URI + "/private/series");
        checkCanGetToPage(driver.findElements(tagName("a")).get(4), URI + "/private/history");
        checkCanGetToPage(driver.findElements(tagName("a")).get(5), URI + "/private/statistics");
        checkCanGetToPage(driver.findElements(tagName("a")).get(6), URI + "/private/wishlist");
        //mobile
        driver.manage().window().setSize(getNexus5XScreenSize());
        checkCanGetToMobilePage(driver.findElements(tagName("a")).get(7), URI + "/private/user");
        checkCanGetToMobilePage(driver.findElements(tagName("a")).get(8), URI + "/private/home");
        checkCanGetToMobilePage(driver.findElements(tagName("a")).get(9), URI + "/private/movies");
        checkCanGetToMobilePage(driver.findElements(tagName("a")).get(10), URI + "/private/series");
        checkCanGetToMobilePage(driver.findElements(tagName("a")).get(11), URI + "/private/history");
        checkCanGetToMobilePage(driver.findElements(tagName("a")).get(12), URI + "/private/statistics");
        checkCanGetToMobilePage(driver.findElements(tagName("a")).get(13), URI + "/private/wishlist");
    }

    private void checkCanGetToPage(WebElement link, String urlToCheck) {
        sleep(driver, 5, invisibilityOf(driver.findElementById("loader-wrapper")));
        sleep(driver, 5, elementToBeClickable(link));
        link.click();
        assertEquals(urlToCheck, driver.getCurrentUrl());
    }

    private void checkCanGetToMobilePage(WebElement link, String urlToCheck) {
        sleep(driver, 5, invisibilityOf(driver.findElementById("loader-wrapper")));
        driver.findElementByCssSelector("i.fa.fa-bars").click();
        sleep(driver, 5, visibilityOf(link));
        await().until(() -> link.getLocation().getX() >= 0);
        link.click();
        assertEquals(urlToCheck, driver.getCurrentUrl());
    }

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
        driver.findElement(By.cssSelector("img.series-poster")).click();
        await().atMost(5, SECONDS)
                .until(() -> driver.findElementById("series-title").getText().equals("Black Mirror"));
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

    private void goAndWaitForPageToLoadMovies() {
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
    public void testAddingView() throws Exception {
        goAndWaitForPageToLoadMovies();
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

    @Test
    public void testLoginPageTranslations() throws Exception {
        checkLoginPageTranslations("en");
        checkLoginPageTranslations("et");
        checkLoginPageTranslations("de");
        checkLoginPageVerifyEmailTranslation("en");
        checkLoginPageVerifyEmailTranslation("et");
        checkLoginPageVerifyEmailTranslation("de");
        checkLoginPageUnauthorizedTranslation("en");
        checkLoginPageUnauthorizedTranslation("et");
        checkLoginPageUnauthorizedTranslation("de");
    }

    private void checkLoginPageTranslations(String lang) {
        assertGoToPage(driver, URI + "/login?lang=" + lang + "&message=LOGIN_VERIFIED");
        assertEquals(getString("LOGIN_TITLE", lang), driver.getTitle());
        List<WebElement> messages = driver.findElements(tagName("h5"));
        assertEquals(getString("LOGIN_TITLE", lang), messages.get(0).getText());
        assertEquals(getString("LOGIN_VERIFIED", lang), messages.get(1).getText());
        List<WebElement> loginButtons = driver.findElements(tagName("a"));
        assertEquals(getString("LOGIN_PASSWORD", lang).toUpperCase(ENGLISH), loginButtons.get(0).getText());
        assertEquals(getString("LOGIN_FACEBOOK", lang).toUpperCase(ENGLISH), loginButtons.get(1).getText());
        assertEquals(getString("LOGIN_GOOGLE", lang).toUpperCase(ENGLISH), loginButtons.get(2).getText());
        assertEquals(getString("LOGIN_IDCARD", lang).toUpperCase(ENGLISH), loginButtons.get(3).getText());
    }

    private void checkLoginPageVerifyEmailTranslation(String lang) {
        assertGoToPage(driver, URI + "/login?lang=" + lang + "&message=FORM_REGISTER_VERIFY_EMAIL");
        assertEquals(getString("FORM_REGISTER_VERIFY_EMAIL", lang),
                driver.findElements(tagName("h5")).get(1).getText());
    }

    private void checkLoginPageUnauthorizedTranslation(String lang) {
        assertGoToPage(driver, URI + "/login?lang=" + lang + "&message=AUTHORIZER_UNAUTHORIZED");
        assertEquals(getString("AUTHORIZER_UNAUTHORIZED", lang),
                driver.findElements(tagName("h5")).get(1).getText());
    }

    @Test
    public void testFacebookLogin() throws Exception {
        driver.manage().deleteAllCookies();
        JsonObject fbAuth = config.getJsonObject("unit_test").getJsonObject("facebook_user");
        assertGoToPage(driver, URI + "/login");
        sleep(driver, 5, invisibilityOf(driver.findElement(id("loader-wrapper"))));
        driver.findElements(tagName("a")).get(1).click();
        assertTrue(driver.getCurrentUrl().contains("facebook"));
        driver.findElement(id("email")).sendKeys(fbAuth.getString("username"));
        driver.findElement(id("pass")).sendKeys(fbAuth.getString("password"));
        driver.findElement(id("loginbutton")).click();
        assertEquals(URI + "/private/home?client_name=FacebookClient#_=_", driver.getCurrentUrl());
        driver.manage().deleteAllCookies();
    }
}
