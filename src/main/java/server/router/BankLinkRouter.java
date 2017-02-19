package server.router;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import server.service.BankLinkService;

import static server.entity.Status.badRequest;
import static server.util.HandlerUtils.jsonResponse;
import static server.util.HandlerUtils.parseParam;
import static server.util.HandlerUtils.resultHandler;

/**
 * Created by Alar on 19/02/2017.
 */
public class BankLinkRouter extends Routable {

    private static final Logger log = LoggerFactory.getLogger(BankLinkRouter.class);
    private static final String API_GET_PAYMENTS = "/:payments";
    private static final String API_GET_PAYMENT = "/payments/:paymentId";

    private final BankLinkService bls;

    public BankLinkRouter(Vertx vertx, BankLinkService bls) {
        super(vertx);
        this.bls = bls;
    }

    @Override
    public void route(Router router) {
        router.get(API_GET_PAYMENTS).handler(this::handleApiGetPayments);
        router.get(API_GET_PAYMENT).handler(this::handleApiGetPayment);
    }

    private void handleApiGetPayments(RoutingContext ctx) { // handleb kõikide maksete json päringut /payments/: -> localhost:8082/api/project
        String name = ctx.request().getParam(parseParam(API_GET_PAYMENTS));
        if (name == null) {
            badRequest(ctx);
            return;
        }
        bls.getPayments().setHandler(resultHandler(ctx, jsonResponse(ctx)));
    }

    private void handleApiGetPayment(RoutingContext ctx) { // handleb vastava makse json päringut /payments/:tehingu id -> localhost:8082/api/project/1234
        String id = ctx.request().getParam(parseParam(API_GET_PAYMENT));
        if (id == null) {
            badRequest(ctx);
            return;
        }
        bls.getPaymentById(id).setHandler(resultHandler(ctx, jsonResponse(ctx)));
    }
}
