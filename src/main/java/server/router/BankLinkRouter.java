package server.router;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.codec.binary.Base64;
import server.service.BankLinkService;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import static server.entity.Status.OK;
import static server.entity.Status.badRequest;
import static server.entity.Status.serviceUnavailable;
import static server.util.CommonUtils.getPemPrivateKey;
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
        String vk_service = "1011"; // default=1011
        String vk_version = "008"; // default=008
        String vk_snd_id = ""; // kliendi tunnuskood pangale edastamiseks kujul uid100023
        String vk_stamp = ""; // tehingu number, tuleks ise genereerida et üheselt tehing identifitseerida
        String vk_amount = ctx.getBodyAsJson().getString("amount"); // Makse summa
        String vk_curr = "EUR"; // Makse valuuta
        String vk_acc = ctx.getBodyAsJson().getString("account_nr"); // Saaja konto nr
        String vk_name = ""; // Maksja nimi
        String vk_ref = ""; // Viitenumber
        String vk_lang = "EST"; //tehingu keel
        String vk_msg = ctx.getBodyAsJson().getString("product"); // Toode/makse kirjeldus
        String vk_return = RETURN_URL;
        String vk_cancel = CANCEL_URL;
        String vk_datetime = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"));
        String vk_encoding = "utf-8";

        String toBeSigned = "004" + vk_service + vk_version + vk_snd_id + vk_stamp + vk_amount +
                vk_curr + vk_acc + vk_name + vk_ref + vk_lang + vk_msg + vk_return + vk_cancel +
                vk_datetime;

        try {
            Signature instance = Signature.getInstance("SHA1withRSA");
            vertx.fileSystem().readFile("path", result ->{
                try{
                    instance.initSign(getPemPrivateKey(result.result().toString(StandardCharsets.UTF_8), "RSA"));

                }
                catch (Exception e){
                    e.printStackTrace();
                }
            });
            instance.update((toBeSigned).getBytes());
            String vk_mac = "";
            byte[] signature = instance.sign();
            for (byte b : signature) {
                vk_mac += b;
            }

            String params =
                    "VK_SERVICE=" + vk_service+
                    "&VK_VERSION=" + vk_version +
                    "&VK_SND_ID=" + vk_snd_id +
                    "&VK_STAMP=" + vk_stamp +
                    "&VK_AMOUNT=" + vk_amount +
                    "&VK_CURR=" + vk_curr +
                    "&VK_ACC=" + vk_acc +
                    "&VK_NAME=" + vk_name +
                    "&VK_REF=" + vk_ref +
                    "&VK_MSG=" + vk_msg +
                    "&VK_RETURN=" + vk_return +
                    "&VK_CANCEL=" + vk_cancel +
                    "&VK_DATETIME=" + vk_datetime +
                    "&VK_ENCODING=" + vk_encoding +
                    "&VK_MAC=" + vk_mac;

            bls.createPayment(params);



        } catch (Exception e) {
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
