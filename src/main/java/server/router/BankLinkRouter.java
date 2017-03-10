package server.router;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import server.entity.Status;
import server.service.BankLinkService;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;

import static server.entity.Status.badRequest;
import static server.entity.Status.serviceUnavailable;
import static server.util.CommonUtils.contains;
import static server.util.CommonUtils.getDerPrivateKey;
import static server.util.HandlerUtils.*;

public class BankLinkRouter extends Routable {
    private static final Logger LOG = LoggerFactory.getLogger(BankLinkRouter.class);
    private static final String API_CREATE_PAYMENT = "/api/donate";
    private static final String API_GET_PAYMENTSOLUTIONS = "/api/solutions";
    private static final String API_GET_PAYMENTSOLUTION = "/api/solutions/:solutionId";

    private static final String BANK_TYPE = "ipizza";
    private static final String RETURN_URL = "http://localhost:8083/project/k9a3bsY5Fxs4ZMQd?payment_action=success"; //TODO 23.02: define url where to return after payment completion
    private static final String CANCEL_URL = "http://localhost:8083/project/k9a3bsY5Fxs4ZMQd?payment_action=cancel"; //TODO 23.02: define url where to return after payment cancellation

    private final BankLinkService bankLink;

    public BankLinkRouter(Vertx vertx, BankLinkService bankLink) {
        super(vertx);
        this.bankLink = bankLink;
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
        String vk_snd_id = "uid100036"; // kliendi tunnuskood pangale edastamiseks kujul uid100023
        String vk_stamp = "12345"; // tehingu number, tuleks ise genereerida et üheselt tehing identifitseerida
        String vk_amount = "150"; // Makse summa
        String vk_curr = "EUR"; // Makse valuuta
        String vk_acc = "123456789"; // Saaja konto nr
        String vk_name = "alar"; // Saaja nimi
        String vk_ref = "1234561"; // Viitenumber
        String vk_lang = "EST"; //tehingu keel
        String vk_msg = "Torso Tiger"; // Toode/makse kirjeldus
        String vk_return = RETURN_URL;
        String vk_cancel = CANCEL_URL;
        String vk_encoding = "utf-8";
        String vk_datetime = "2017-03-09T14:19:37+0200";//ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"));



        String toBeSigned = String.format("%03d", vk_service.length()) + vk_service +
                String.format("%03d", vk_version.length()) + vk_version +
                String.format("%03d", vk_snd_id.length()) + vk_snd_id +
                String.format("%03d", vk_stamp.length()) + vk_stamp +
                String.format("%03d", vk_amount.length()) + vk_amount +
                String.format("%03d", vk_curr.length()) + vk_curr +
                String.format("%03d", vk_acc.length()) + vk_acc +
                String.format("%03d", vk_name.length()) + vk_name +
                String.format("%03d", vk_ref.length()) + vk_ref +
                //String.format("%03d", vk_lang.length()) + vk_lang +
                String.format("%03d", vk_msg.length()) + vk_msg +
                String.format("%03d", vk_return.length()) + vk_return +
                String.format("%03d", vk_cancel.length()) + vk_cancel +
                String.format("%03d", vk_datetime.length()) + vk_datetime;

        System.out.println(toBeSigned);
        try {

            // Pangalingi poolt genereeritud võti tuleb konverteerida der-formaati, et java seda süüa saaks.
            // Käsk selleks: openssl pkcs8 -topk8 -inform PEM -outform DER -in /pangalingi_genereeritud_rsavõti.pem/ -out /konverteeritud_rsavõti.der/ -nocrypt
            Signature instance = Signature.getInstance("SHA1withRSA");
            vertx.fileSystem().readFile("banklink_private_key.der", result ->{
                try{
                    PrivateKey key = getDerPrivateKey(result.result().getBytes(), "RSA");
                    instance.initSign(key);
                    instance.update((toBeSigned).getBytes());
                    String vk_mac = "";
                    byte[] signature = instance.sign();
                    //vk_mac = Base64.getEncoder().encode(signature).toString();
                    vk_mac = new String(Base64.getEncoder().encode(signature), StandardCharsets.UTF_8);
                    System.out.println(vk_mac);

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
                                    "&VK_LANG=" + vk_lang +
                                    "&VK_MSG=" + "Torso+Tiger" +
                                    "&VK_RETURN=" + vk_return +
                                    "&VK_CANCEL=" + vk_cancel +
                                    "&VK_DATETIME=" + vk_datetime +
                                    "&VK_ENCODING=" + vk_encoding +
                                    "&VK_MAC=" + vk_mac;

                    bankLink.createPayment(params).setHandler(ar -> {
                        if (ar.succeeded()) {
                            ctx.response().setStatusCode(200).end(ar.result());
                        } else {
                            Status.serviceUnavailable(ctx, new Throwable("BankLink createPayment failed"));
                        }
                    });
                } catch (Exception e){
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleApiGetPayments(RoutingContext ctx) { // handleb kõikide makselahenduste json päringut /payments/: -> localhost:8083/api/project
        bankLink.getPaymentSolutions().setHandler(resultHandler(ctx, jsonResponse(ctx)));
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

        bankLink.createPaymentSolution(jso)
                .setHandler(resultHandler(ctx, jsonResponse(ctx)));
    }

    private void handleApiGetPaymentSolution(RoutingContext ctx) { // handleb vastava makselahenduse json päringut /payments/:tehingu id -> localhost:8083/api/project/1234
        String id = ctx.request().getParam(parseParam(API_GET_PAYMENTSOLUTION));
        if (id == null) {
            badRequest(ctx);
            return;
        }
        bankLink.getPaymentSolutionById(id).setHandler(resultHandler(ctx, jsonResponse(ctx)));
    }


}
