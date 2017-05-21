package server.router;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.core.MultiMap;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpClientRequest;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import server.entity.Status;
import server.util.LocalDatabase;
import server.verticle.ServerVerticle;

import static io.vertx.core.http.HttpHeaders.*;
import static io.vertx.rxjava.core.MultiMap.caseInsensitiveMultiMap;
import static io.vertx.rxjava.core.RxHelper.deployVerticle;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static server.router.DatabaseRouter.API_HISTORY;
import static server.service.DatabaseService.getRows;
import static server.util.FileUtils.getConfig;
import static server.util.LocalDatabase.*;
import static server.util.NetworkUtils.HTTP_PORT;
import static server.util.Utils.getAuthenticationData;
import static server.util.Utils.getSession;

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
        // TODO: 20.05.2017 csrf check will fail as intended
        // TODO: 20.05.2017 need to load formlogin first -> get token -> post callback with token as data
        client.post("/callback", res -> {
            cookies.add(COOKIE.toString(), getSession(res.headers()));
            async.complete();
        }).putHeader(CONTENT_TYPE.toString(), APPLICATION_X_WWW_FORM_URLENCODED.toString())
                .end(getAuthenticationData(config.getJsonObject("unit_test").getJsonObject("form_user")));
    }

    @After
    public void tearDown() throws Exception {
        client.close();
        cookies.clear();
    }

    @Test(timeout = 5000L)
    public void testGetHistory(TestContext ctx) throws Exception {
        localDatabase.updateOrInsertBlocking(SQL_INSERT_MOVIES_HOBBIT, null);
        localDatabase.updateOrInsertBlocking(SQL_INSERT_MOVIES_GHOST, null);
        localDatabase.updateOrInsertBlocking(SQL_INSERT_VIEW_HOBBIT, null);
        localDatabase.updateOrInsertBlocking(SQL_INSERT_VIEW_GHOST, null);
        Async async = ctx.async();
        HttpClientRequest req = client.get(API_HISTORY, res -> {
            assertThat(res.statusCode(), is(Status.OK));
            res.bodyHandler(body -> {
                JsonObject json = body.toJsonObject();
                assertThat(json.containsKey("rows"), is(true));
                JsonArray results = getRows(json);
                assertThat(results.size(), is(2));
                // TODO: 19.05.2017 assertions
                System.out.println(results); //ok
                async.complete();
                // TODO: 19.05.2017 syncresult is broken?
                // TODO: 19.05.2017 rewrite with webclient
                // TODO: 19.05.2017 for rx -> check examples
            });
        });
        req.headers().addAll(cookies);
        req.end(new JsonObject()
                .put("is-first", false)
                .put("is-cinema", false)
                .put("start", "1 January, 2017")
                .put("end", "31 December, 2017")
                .put("page", 0).encode());
    }

    @Test
    public void testHandleUserInfo() throws Exception {
        // TODO: 18/05/2017
    }
}
