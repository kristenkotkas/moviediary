package server.router;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.core.MultiMap;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.HttpClient;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import server.entity.JsonObj;
import server.entity.Status;
import server.util.CommonUtils;
import server.util.LocalDatabase;
import server.util.Utils;
import server.verticle.ServerVerticle;

import static io.vertx.core.http.HttpHeaders.APPLICATION_X_WWW_FORM_URLENCODED;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static io.vertx.rxjava.core.MultiMap.caseInsensitiveMultiMap;
import static io.vertx.rxjava.core.RxHelper.deployVerticle;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static server.router.DatabaseRouter.API_HISTORY;
import static server.util.CommonUtils.check;
import static server.util.FileUtils.getConfig;
import static server.util.LocalDatabase.*;
import static server.util.NetworkUtils.HTTP_PORT;
import static server.util.Utils.*;

@RunWith(VertxUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DatabaseRouterTest {
  private static final int PORT = 8082;
  private static final String URI = "http://localhost:" + PORT;
  private static Vertx vertx;
  private static JsonObject config;
  private static LocalDatabase localDatabase;

  private HttpClient client;
  private MultiMap cookies = caseInsensitiveMultiMap();

  @BeforeClass
  public static void setUpClass(TestContext ctx) throws Exception {
    vertx = Vertx.vertx();
    config = getConfig().put(HTTP_PORT, PORT);
    config.getJsonObject("oauth").put("localCallback", URI + "/callback");
    initializeDatabase(vertx, config.getJsonObject("mysql")).rxSetHandler()
        .doOnSuccess(db -> localDatabase = db)
        .doOnError(ctx::fail)
        .toCompletable()
        .andThen(deployVerticle(vertx, new ServerVerticle(), new DeploymentOptions().setConfig(config)))
        .test()
        .awaitTerminalEvent(10, SECONDS)
        .assertCompleted();
  }

  @AfterClass
  public static void tearDown(TestContext ctx) throws Exception {
    Async async = ctx.async();
    localDatabase.close();
    vertx.rxClose().subscribe(v -> async.complete(), ctx::fail);
  }

  @Before
  public void setUp(TestContext ctx) throws Exception {
    localDatabase.resetCleanStateBlocking();
    client = vertx.createHttpClient(new HttpClientOptions().setDefaultHost("localhost").setDefaultPort(PORT));
    Async async = ctx.async();
    doRequest(client.get("/formlogin"), ctx, cookies)
        .flatMap(res -> doRequest(client.post("/callback"), ctx, cookies, headers()
                .add(CONTENT_TYPE.toString(), APPLICATION_X_WWW_FORM_URLENCODED.toString()),
            getAuthenticationData(config.getJsonObject("unit_test").getJsonObject("form_user"))))
        .subscribe(res -> async.complete());
  }

  @After
  public void tearDown() throws Exception {
    client.close();
    cookies.clear();
  }

  @Test
  public void testGetHistory(TestContext ctx) throws Exception {
    localDatabase.updateOrInsertBlocking(SQL_INSERT_MOVIES_HOBBIT, null);
    localDatabase.updateOrInsertBlocking(SQL_INSERT_MOVIES_GHOST, null);
    localDatabase.updateOrInsertBlocking(SQL_INSERT_VIEW_HOBBIT, null);
    localDatabase.updateOrInsertBlocking(SQL_INSERT_VIEW_GHOST, null);
    Async async = ctx.async();
    doRequest(client.get(API_HISTORY), ctx, cookies, new JsonObject()
        .put("is-first", false)
        .put("is-cinema", false)
        .put("start", "1 January, 2017")
        .put("end", "31 December, 2017")
        .put("page", 0).encode())
        .doOnNext(res -> assertThat(res.statusCode(), is(Status.OK)))
        .flatMap(Utils::bodyToObservable)
        .map(Buffer::toJsonObject)
        .map(CommonUtils::getRows)
        .doOnNext(results -> assertThat(results.size(), is(2)))
        .doOnNext(results -> results.stream()
            .map(JsonObj::fromParent)
            .forEach(view -> check(view.getInteger("Id") == 1,
                () -> assertHobbitView(view),
                () -> assertGhostView(view))))
        .subscribe(results -> async.complete());
  }

  private void assertHobbitView(JsonObject view) {
    assertThat(view.getInteger("MovieId"), is(49051));
    assertThat(view.getString("Title"), is("The Hobbit: An Unexpected Journey"));
    assertThat(view.getString("Start"), is("23 April 2017"));
    assertThat(view.getString("WasFirst"), is("fa fa-eye"));
    assertThat(view.getString("WasCinema"), is(""));
    assertThat(view.getString("Image"), is("/w29Guo6FX6fxzH86f8iAbEhQEFC.jpg"));
    assertThat(view.getString("Comment"), is("random"));
    assertThat(view.getInteger("Runtime"), is(106));
    assertThat(view.getString("DayOfWeek"), is("SUNDAY"));
    assertThat(view.getString("Time"), is("14:58"));
  }

  public void assertGhostView(JsonObject view) {
    assertThat(view.getInteger("MovieId"), is(315837));
    assertThat(view.getString("Title"), is("Ghost in the Shell"));
    assertThat(view.getString("Start"), is("17 March 2017"));
    assertThat(view.getString("WasFirst"), is(""));
    assertThat(view.getString("WasCinema"), is("fa fa-ticket new"));
    assertThat(view.getString("Image"), is("/si1ZyELNHdPUZw4pXR5KjMIIsBF.jpg"));
    assertThat(view.getString("Comment"), is("lamp"));
    assertThat(view.getInteger("Runtime"), is(106));
    assertThat(view.getString("DayOfWeek"), is("FRIDAY"));
    assertThat(view.getString("Time"), is("15:58"));
  }
}
