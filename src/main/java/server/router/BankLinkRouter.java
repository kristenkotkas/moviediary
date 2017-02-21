package server.router;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import server.service.BankLinkService;

import static server.entity.Status.badRequest;
import static server.entity.Status.serviceUnavailable;
import static server.util.HandlerUtils.*;

import static server.util.CommonUtils.contains;

public class BankLinkRouter extends Routable {
    private static final Logger log = LoggerFactory.getLogger(BankLinkRouter.class);
    private static final String API_GET_PAYMENTS = "/api/payments";
    private static final String API_GET_PAYMENT = "/api/payments/:paymentId";

    private static final String BANK_TYPE = "ipizza";
    private static final String RETURN_URL = ""; //TODO: define

    private final BankLinkService bls;

    public BankLinkRouter(Vertx vertx, BankLinkService bls) {
        super(vertx);
        this.bls = bls;
    }

    @Override
    public void route(Router router) {
        router.get(API_GET_PAYMENTS).handler(this::handleApiGetPayments);
        router.post(API_GET_PAYMENTS).handler(this::handleApiCreatePayment);
        router.get(API_GET_PAYMENT).handler(this::handleApiGetPayment);
    }

    private void handleApiGetPayments(RoutingContext ctx) { // handleb kõikide maksete json päringut /payments/: -> localhost:8083/api/project
        bls.getPayments().setHandler(resultHandler(ctx, jsonResponse(ctx)));
    }

    private void handleApiCreatePayment(RoutingContext ctx){
        String payment_name = ctx.getBodyAsJson().getString("name");
        String payment_description = ctx.getBodyAsJson().getString("description"); // can be optional
        String account_owner = ctx.getBodyAsJson().getString("account_owner");
        String account_nr = ctx.getBodyAsJson().getString("account_nr");

        if (contains("", payment_name, account_owner, account_nr)){
            serviceUnavailable(ctx, new Throwable("Fields Payment name, Account owner, Account number must be filled!"));
        }

        JsonObject jso = new JsonObject()
                .put("type",BANK_TYPE)
                .put("name", payment_name)
                .put("description", payment_description)
                .put("account_owner", account_owner)
                .put("account_nr", account_nr)
                .put("return url", RETURN_URL);

        bls.createPayment(jso)
                .setHandler(resultHandler(ctx, jsonResponse(ctx)));
    }

    private void handleApiGetPayment(RoutingContext ctx) { // handleb vastava makse json päringut /payments/:tehingu id -> localhost:8083/api/project/1234
        String id = ctx.request().getParam(parseParam(API_GET_PAYMENT));
        if (id == null) {
            badRequest(ctx);
            return;
        }
        bls.getPaymentById(id).setHandler(resultHandler(ctx, jsonResponse(ctx)));
    }


}
