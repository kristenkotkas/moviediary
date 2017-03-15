package server.router;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import server.service.BankLinkService;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import static server.entity.Status.badRequest;
import static server.entity.Status.serviceUnavailable;
import static server.util.CommonUtils.contains;
import static server.util.CommonUtils.getDerPrivateKey;
import static server.util.HandlerUtils.*;

public class BankLinkRouter extends EventBusRoutable {
    private static final Logger LOG = LoggerFactory.getLogger(BankLinkRouter.class);
    private static final String API_GET_PAYMENTSOLUTIONS = "/private/v1/api/solutions";
    private static final String API_GET_PAYMENTSOLUTION = "/private/v1/api/solutions/:solutionId";

    private static final String BANK_TYPE = "ipizza";

    public static String vk_service = "1011"; // default=1011
    public static String vk_version = "008"; // default=008
    public static String vk_snd_id = "uid100036"; // kliendi tunnuskood pangale edastamiseks kujul uid100023
    public static String vk_stamp = "12345"; // tehingu number, tuleks ise genereerida et üheselt tehing identifitseerida
    public static String vk_amount = "5"; // Makse summa
    public static String vk_curr = "EUR"; // Makse valuuta
    public static String vk_acc = "EE371600000123456789"; // Saaja (arendajate) konto nr
    public static String vk_name = "MovieDiary MTÜ"; // Saaja (arendajate) nimi
    public static String vk_ref = "1234561"; // Viitenumber
    public static String vk_lang = "EST"; //tehingu keel
    public static String vk_msg = "Donation"; // Toode/makse kirjeldus
    public static String vk_return = "http://localhost:8081/private/success";
    public static String vk_cancel = "http://localhost:8081/private/failure";
    public static String vk_encoding = "utf-8";
    public static String vk_datetime = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"));
    public static String vk_mac = "";

    private final BankLinkService bankLink;

    public BankLinkRouter(Vertx vertx, BankLinkService bankLink) {
        super(vertx);
        this.bankLink = bankLink;
        createMac();
    }

    @Override
    public void route(Router router) {
        router.get(API_GET_PAYMENTSOLUTIONS).handler(this::handleApiGetPayments);
        router.post(API_GET_PAYMENTSOLUTIONS).handler(this::handleApiCreatePaymentSolution);
        router.get(API_GET_PAYMENTSOLUTION).handler(this::handleApiGetPaymentSolution);
    }

    private void createMac(){
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
        try {

            // Pangalingi poolt genereeritud võti tuleb konverteerida der-formaati, et java seda süüa saaks.
            // Käsk selleks: openssl pkcs8 -topk8 -inform PEM -outform DER -in banklink_private_key.pem -out banklink_private_key.der -nocrypt
            Signature instance = Signature.getInstance("SHA1withRSA");
            vertx.fileSystem().readFile("banklink_private_key.der", result ->{
                try{
                    RSAPrivateKey key = getDerPrivateKey(result.result().getBytes(), "RSA");
                    instance.initSign(key);
                    instance.update((toBeSigned).getBytes());
                    byte[] signature = instance.sign();
                    vk_mac = new String(Base64.getEncoder().encode(signature), StandardCharsets.UTF_8);
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
                .put("name", vk_name)
                .put("description", vk_msg)
                .put("account_owner", vk_name)
                .put("account_nr", vk_acc)
                .put("return url", vk_return);

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
