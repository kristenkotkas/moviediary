package server.router;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import server.service.BankLinkService;

import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.time.LocalDateTime;

import static server.entity.Status.badRequest;
import static server.entity.Status.serviceUnavailable;
import static server.util.HandlerUtils.*;

import static server.util.CommonUtils.contains;

public class BankLinkRouter extends Routable {
    private static final Logger log = LoggerFactory.getLogger(BankLinkRouter.class);
    private static final String API_CREATE_PAYMENT = "/api/solutions/pay";
    private static final String API_GET_PAYMENTSOLUTIONS = "/api/solutions";
    private static final String API_GET_PAYMENTSOLUTION = "/api/solutions/:solutionId";

    private static final String BANK_TYPE = "ipizza";
    private static final String RETURN_URL = ""; //TODO 23.02: define url where to return after payment completion
    private static final String CANCEL_URL = ""; //TODO 23.02: define url where to return after payment cancellation

    private final BankLinkService bls;

    public BankLinkRouter(Vertx vertx, BankLinkService bls) {
        super(vertx);
        this.bls = bls;
    }

    @Override
    public void route(Router router) {
        router.get(API_GET_PAYMENTSOLUTIONS).handler(this::handleApiGetPayments);
        router.post(API_CREATE_PAYMENT).handler(this::handleApiCreatePayment);
        router.post(API_GET_PAYMENTSOLUTIONS).handler(this::handleApiCreatePaymentSolution);
        router.get(API_GET_PAYMENTSOLUTION).handler(this::handleApiGetPaymentSolution);
    }

    private void handleApiCreatePayment(RoutingContext ctx){
        String vk_service = ctx.getBodyAsJson().getString("name");
        String vk_version = ctx.getBodyAsJson().getString("description");
        String vk_snd_id = ctx.getBodyAsJson().getString("account_owner");
        String vk_stamp = ctx.getBodyAsJson().getString("account_nr");
        String vk_amount = ctx.getBodyAsJson().getString("amount");
        String vk_curr = "";
        String vk_acc = "";
        String vk_name = "";
        String vk_ref = "";
        String vk_lang = "EST";
        String vk_msg = ctx.getBodyAsJson().getString("product");
        String vk_return = RETURN_URL;
        String vk_cancel = CANCEL_URL;
        String vk_datetime = LocalDateTime.now().toString();
        String vk_encoding = "utf-8";

        try{
            Signature instance = Signature.getInstance("SHA1withRSA");
            //instance.initSign();//TODO:implement getting key
        }
        catch (NoSuchAlgorithmException e){
            e.printStackTrace();
        }




    }

    private void handleApiGetPayments(RoutingContext ctx) { // handleb kõikide makselahenduste json päringut /payments/: -> localhost:8083/api/project
        bls.getPaymentSolutions().setHandler(resultHandler(ctx, jsonResponse(ctx)));
    }

    private void handleApiCreatePaymentSolution(RoutingContext ctx){ // handleb makselahenduse loomist
        String payment_name = ctx.getBodyAsJson().getString("name"); // Our company
        String payment_description = ctx.getBodyAsJson().getString("description"); // can be optional (purpose of the solution)
        String account_owner = ctx.getBodyAsJson().getString("account_owner"); // We, the developers
        String account_nr = ctx.getBodyAsJson().getString("account_nr"); // Our account where client's payments are received

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

        bls.createPaymentSolution(jso)
                .setHandler(resultHandler(ctx, jsonResponse(ctx)));
    }

    private void handleApiGetPaymentSolution(RoutingContext ctx) { // handleb vastava makselahenduse json päringut /payments/:tehingu id -> localhost:8083/api/project/1234
        String id = ctx.request().getParam(parseParam(API_GET_PAYMENTSOLUTION));
        if (id == null) {
            badRequest(ctx);
            return;
        }
        bls.getPaymentSolutionById(id).setHandler(resultHandler(ctx, jsonResponse(ctx)));
    }


}
