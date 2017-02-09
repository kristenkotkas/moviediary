package sys.router;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import sys.template.engine.HandlebarsTemplateEngine;
import sys.template.ui.BaseTemplate;
import sys.template.ui.IndexTemplate;

public class UiRouter extends Routable {
    private static final String INDEX = "/";

    private static final String TEMPL_INDEX = "templates/index.hbs";

    private static final String STATIC_PATH = "/static/*";
    private static final String STATIC_FOLDER = "static";

    private final HandlebarsTemplateEngine engine;

    public UiRouter(Vertx vertx) throws Exception {
        super(vertx);
        this.engine = HandlebarsTemplateEngine.create();
    }

    @Override
    public void route(Router router) {
        router.get(INDEX).handler(this::handleIndex); // lehe / kuvamise jaoks kutsume tema handlerit

        router.route(STATIC_PATH).handler(StaticHandler.create(STATIC_FOLDER) //jagab staatilisi ressursse
                .setCachingEnabled(true)
                .setIncludeHidden(false));
    }

    private void handleIndex(RoutingContext ctx) {
        //loome templatei sisu
        JsonObject card1 = new JsonObject().put("card", "kaart 1").put("sisu", "sisu 1");
        JsonObject card2 = new JsonObject().put("card", "kaart 2").put("sisu", "sisu 2");
        JsonObject card3 = new JsonObject().put("card", "kaart 3").put("sisu", "sisu 3");
        JsonArray array = new JsonArray().add(card1).add(card2).add(card3);
        engine.render(getSafe(ctx, TEMPL_INDEX, IndexTemplate.class)
                .setPealkiri("Mega pealkiri") //lisa muutujaid
                .setCards(array), endHandler(ctx));
    }

    //tagastab templaadi mis on type param tüüpi, peab olema BaseTemplate alamklass
    private <S extends BaseTemplate> S getSafe(RoutingContext ctx, String fileName, Class<S> type) {
        //saad panna baseTemplatei muutujaid kui vajadust peaks olema
        return engine.getSafeTemplate(ctx, fileName, type); //hangib handlebarsist typesafe templaadi
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
