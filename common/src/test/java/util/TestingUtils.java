package util;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.rxjava.core.MultiMap;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpClientResponse;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import rx.Emitter;
import rx.Observable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static java.io.File.separator;
import static util.ConditionUtils.check;
import static util.ConditionUtils.ifPresent;

public class TestingUtils {

  public static MultiMap headers() {
    return MultiMap.caseInsensitiveMultiMap();
  }

  public static void formLogin(WebDriver driver, String uri, JsonObject config) {
    JsonObject user = config.getJsonObject("unit_test").getJsonObject("form_user");
    assertGoToPage(driver, uri + "/formlogin");
    List<WebElement> fields = driver.findElements(By.tagName("input"));
    sleep(driver, 5, ExpectedConditions.invisibilityOf(driver.findElement(By.id("loader-wrapper"))));
    fields.get(0).sendKeys(user.getString("username"));
    fields.get(1).sendKeys(user.getString("password"));
    driver.findElement(By.tagName("button")).click();
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
    Assert.assertEquals(page, driver.getCurrentUrl());
  }

  public static Observable<HttpClientResponse> doRequest(HttpClientRequest req, TestContext ctx, MultiMap cookies,
                                                         MultiMap headers, String data) {
    Objects.requireNonNull(req);
    Objects.requireNonNull(ctx);
    Objects.requireNonNull(cookies);
    return Observable.<HttpClientResponse>create(emitter -> {
      req.putHeader("cookie", getCookieString(cookies));
      req.handler(emitter::onNext);
      req.exceptionHandler(emitter::onError);
      ifPresent(headers, h -> req.headers().addAll(h));
      check(data == null, (Runnable) req::end, () -> req.end(data));
    }, Emitter.BackpressureMode.ERROR)
        .doOnError(ctx::fail)
        .doOnNext(res -> updateCookies(res.cookies(), cookies));
  }

  private static void updateCookies(List<String> cookies, MultiMap previous) {
    cookies.stream()
        .map(cookie -> cookie.split("=", 2))
        .filter(array -> !previous.contains(array[0]) || !previous.get(array[0]).equals(array[1]))
        .forEach(array -> previous.set(array[0], array[1]));
  }

  private static String getCookieString(MultiMap cookies) {
    StringBuilder sb = new StringBuilder();
    cookies.getDelegate().forEach(e -> sb.append(e.getKey()).append("=").append(e.getValue()).append(";"));
    return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";
  }

  public static Observable<HttpClientResponse> doRequest(HttpClientRequest req, TestContext ctx, MultiMap cookies,
                                                         MultiMap headers) {
    return doRequest(req, ctx, cookies, headers, null);
  }

  public static Observable<HttpClientResponse> doRequest(HttpClientRequest req, TestContext ctx, MultiMap cookies,
                                                         String data) {
    return doRequest(req, ctx, cookies, null, data);
  }

  public static Observable<HttpClientResponse> doRequest(HttpClientRequest req, TestContext ctx, MultiMap cookies) {
    return doRequest(req, ctx, cookies, null, null);
  }

  public static String getAuthenticationData(JsonObject user) {
    return "username=" + user.getString("username") +
        "&password=" + user.getString("password") +
        "&client_name=FormClient";
  }

  public static Observable<Buffer> bodyToObservable(HttpClientResponse res) {
    return Observable.create(emitter -> res.bodyHandler(emitter::onNext), Emitter.BackpressureMode.NONE);
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
