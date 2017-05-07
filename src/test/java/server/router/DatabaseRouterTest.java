package server.router;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import server.util.LocalDatabase;
import server.verticle.ServerVerticle;

import static io.vertx.rxjava.core.MultiMap.caseInsensitiveMultiMap;
import static io.vertx.rxjava.core.RxHelper.deployVerticle;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.fail;
import static server.util.FileUtils.getConfig;
import static server.util.LocalDatabase.initializeDatabase;
import static server.util.NetworkUtils.HTTP_PORT;

@RunWith(VertxUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DatabaseRouterTest {
    private static final int PORT = 8082;
    private static final String URI = "http://localhost:" + PORT;
    private static Vertx vertx;
    private static JsonObject config;
    private static LocalDatabase localDatabase;

    private WebClient client;

    @BeforeClass
    public static void setUp(TestContext ctx) throws Exception {
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
    public void setUp() throws Exception {
        localDatabase.resetCleanStateBlocking();
        client = WebClient.create(vertx, new WebClientOptions().setDefaultHost("localhost").setDefaultPort(PORT));
        JsonObject user = config.getJsonObject("unit_test").getJsonObject("form_user");
        HttpResponse res = client.post("/callback")
                .followRedirects(true)
                .rxSendForm(caseInsensitiveMultiMap()
                        .set("username", user.getString("username"))
                        .set("password", user.getString("password"))
                        .set("client_name", "FormClient"))
                .doOnError(err -> fail(err.getMessage()))
                .toBlocking()
                .value();
        // TODO: 07/05/2017 backend redirects to home -> blocked -> doesnt follow?
        System.out.println(res.statusCode());
        System.out.println(res.statusMessage());
        System.out.println(res.cookies());

    }

    @After
    public void tearDown() throws Exception {
        client.close();
    }

    @Test
    public void testGetHistory(TestContext ctx) throws Exception {
        System.out.println("Hello");
    }
}
