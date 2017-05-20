package server.router;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.core.MultiMap;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpClientResponse;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import server.util.LocalDatabase;
import server.verticle.ServerVerticle;

import static io.vertx.core.http.HttpHeaders.*;
import static io.vertx.rxjava.core.MultiMap.caseInsensitiveMultiMap;
import static io.vertx.rxjava.core.RxHelper.deployVerticle;
import static java.util.concurrent.TimeUnit.SECONDS;
import static server.router.DatabaseRouter.API_HISTORY;
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
        HttpClientResponse res = doRequest(client.post("/callback")
                        .putHeader(CONTENT_TYPE.toString(), APPLICATION_X_WWW_FORM_URLENCODED.toString()), null,
                getAuthenticationData(config.getJsonObject("unit_test").getJsonObject("form_user")));
        cookies.add(COOKIE.toString(), getSession(res.headers()));
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
        HttpClientResponse res = doRequest(client.get(API_HISTORY), cookies, new JsonObject()
                .put("is-first", false)
                .put("is-cinema", false)
                .put("start", "1 January, 2017")
                .put("end", "31 December, 2017")
                .put("page", 0).encode());
        res.bodyHandler(body -> System.out.println(body.toJsonObject().encodePrettily()));
        System.out.println(res.statusCode());
        System.out.println(res.statusMessage());
        System.out.println(res.cookies());
        // TODO: 18/05/2017 assertions
    }

    @Test
    public void testHandleUserInfo() throws Exception {
        // TODO: 18/05/2017
    }
}
