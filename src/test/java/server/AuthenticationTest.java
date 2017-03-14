package server;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import server.verticle.ServerVerticle;

import static io.vertx.core.http.HttpHeaders.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static server.entity.Status.FOUND;
import static server.entity.Status.OK;
import static server.router.DatabaseRouter.API_USERS_ALL;
import static server.router.UiRouter.UI_FORM_LOGIN;
import static server.util.FileUtils.getConfig;
import static server.util.NetworkUtils.DEFAULT_HOST;
import static server.util.NetworkUtils.HTTP_PORT;

@RunWith(VertxUnitRunner.class)
public class AuthenticationTest {
    public static final String SESSION_COOKIE = "vertx-web.session";
    private static final int PORT = 8082;
    private static final String URI = "http://localhost:" + PORT;
    private Vertx vertx;

    @Before
    public void setUp(TestContext ctx) throws Exception {
        vertx = Vertx.vertx();
        JsonObject config = getConfig(null).put(HTTP_PORT, PORT);
        vertx.deployVerticle(new ServerVerticle(), new DeploymentOptions().setConfig(config), ctx.asyncAssertSuccess());
    }

    @Test(timeout = 3000L)
    public void testUnauthorisedAccess(TestContext ctx) throws Exception {
        HttpClient client = vertx.createHttpClient();
        Async async = ctx.async();
        client.getNow(PORT, DEFAULT_HOST, API_USERS_ALL, response -> {
            ctx.assertNotEquals(OK, response.statusCode());
            client.close();
            async.complete();
        });
    }

    @Test(timeout = 3000L)
    public void testRedirectToFormLogin(TestContext ctx) throws Exception {
        HttpClient client = vertx.createHttpClient();
        Async async = ctx.async();
        client.getNow(PORT, DEFAULT_HOST, API_USERS_ALL, response -> {
            ctx.assertEquals(FOUND, response.statusCode());
            ctx.assertEquals("/login", response.getHeader(LOCATION));
            client.close();
            async.complete();
        });
    }

    @Test(timeout = 3000L)
    public void testFormLoginResponds(TestContext ctx) throws Exception {
        HttpClient client = vertx.createHttpClient();
        Async async = ctx.async();
        client.getNow(PORT, DEFAULT_HOST, UI_FORM_LOGIN, response -> {
            ctx.assertEquals(OK, response.statusCode());
            response.bodyHandler(body -> {
                String b = body.toString(UTF_8);
                System.out.println(b);
                ctx.assertTrue(b.contains("<!DOCTYPE html>"));
                ctx.assertTrue(b.contains("Form Login"));
                client.close();
                async.complete();
            });
        });
    }

    @Test(timeout = 3000L)
    public void testFormLoginLogin(TestContext ctx) throws Exception {
        HttpClient client = vertx.createHttpClient();
        Async async = ctx.async();
        client.getNow(PORT, DEFAULT_HOST, UI_FORM_LOGIN, response -> {
            ctx.assertEquals(OK, response.statusCode());
            client.post(PORT, DEFAULT_HOST, "/callback?client_name=FormClient", res -> {
                System.out.println(res.statusCode());
                System.out.println(res.statusMessage());
                res.headers().forEach(System.out::println);
                ctx.assertEquals(FOUND, res.statusCode());
                ctx.assertEquals("/private/home", res.getHeader(LOCATION));
                client.close();
                async.complete();
            }).putHeader(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED)
                    .end("username=kristjan@kyngas.eu&password=parool1");
            // TODO: 02/03/2017 make sure database contains this user -> mock
        });
    }

    @Test(timeout = 3000L)
    public void testAuthorizedAfterFormLogin(TestContext ctx) throws Exception {
        HttpClient client = vertx.createHttpClient();
        Async async = ctx.async();
        client.post(PORT, DEFAULT_HOST, "/callback?client_name=FormClient", res -> {
            ctx.assertEquals(FOUND, res.statusCode());
            ctx.assertEquals("/private/home", res.getHeader(LOCATION));
            client.get(PORT, DEFAULT_HOST, API_USERS_ALL, res2 -> {
                ctx.assertEquals(OK, res2.statusCode());
                client.close();
                async.complete();
            }).putHeader(COOKIE, getSession(res.headers())).end();
        }).putHeader(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED)
                .end("username=kristjan@kyngas.eu&password=parool1");
    }

    private String getSession(MultiMap headers) {
        return headers.getAll(SET_COOKIE).stream()
                .filter(s -> s.contains(SESSION_COOKIE))
                .findFirst()
                .orElse(null);
    }

    @After
    public void tearDown(TestContext ctx) throws Exception {
        vertx.close(ctx.asyncAssertSuccess());
    }
}