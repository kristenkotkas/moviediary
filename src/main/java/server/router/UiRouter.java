package server.router;

import eu.kyngas.template.engine.HandlebarsTemplateEngine;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import server.template.ui.BaseTemplate;
import server.template.ui.IndexTemplate;

import java.nio.file.Path;
import java.nio.file.Paths;

import static server.util.FileUtils.isRunningFromJar;

public class UiRouter extends Routable {
    private static final Logger log = LoggerFactory.getLogger(UiRouter.class);
    private static final Path RESOURCES = Paths.get("src/main/resources");
    private static final String STATIC_PATH = "/static/*";
    private static final String STATIC_FOLDER = "static";

    private static final String INDEX = "/";

    private static final String TEMPL_INDEX = "templates/index.hbs";

    private final HandlebarsTemplateEngine engine;
    private final JsonObject config;

    public UiRouter(Vertx vertx, JsonObject config) throws Exception {
        super(vertx);
        this.engine = HandlebarsTemplateEngine.create(isRunningFromJar() ? null : RESOURCES);
        this.config = config;
    }

    @Override
    public void route(Router router) {
        router.get(INDEX).handler(this::handleIndex); // lehe / kuvamise jaoks kutsume tema handlerit

        //jagab staatilisi ressursse
        router.route(STATIC_PATH).handler(StaticHandler.create(isRunningFromJar() ?
                STATIC_FOLDER : RESOURCES.resolve(STATIC_FOLDER).toString())
                .setCachingEnabled(true)
                .setIncludeHidden(false));
    }

    private void handleIndex(RoutingContext ctx) {
        //loome templatei sisu
        JsonObject card1 = new JsonObject().put("card", "kaart 1").put("sisu", "sisu 1");
        JsonObject card2 = new JsonObject().put("card", "kaart 2").put("sisu", "sisu 2");
        JsonObject card3 = new JsonObject().put("card", "kaart 3").put("sisu", "sisu 3");
        JsonObject card4 = new JsonObject().put("card", "kaart 4").put("sisu", "sisu 4");
        JsonArray array = new JsonArray().add(card1).add(card2).add(card3).add(card4);
        engine.render(getSafe(ctx, TEMPL_INDEX, IndexTemplate.class)
                .setPealkiri("Mega pealkiri") //lisa muutujaid
                .setCards(array), endHandler(ctx));
    }

    //tagastab templaadi mis on type param tüüpi, peab olema BaseTemplate alamklass
    private <S extends BaseTemplate> S getSafe(RoutingContext ctx, String fileName, Class<S> type) {
        S baseTemplate = engine.getSafeTemplate(ctx, fileName, type);
        //siia saad panna baseTemplatei muutujaid kui vajadust peaks olema
        return baseTemplate;
    }

    public static Handler<AsyncResult<Buffer>> endHandler(RoutingContext ctx) { // viib valmis html vastuse kliendini
        return ar -> {
            if (ar.succeeded()) {
                ctx.response().end(ar.result());
            } else {
                ctx.fail(ar.cause());
            }
        };
    }
}
