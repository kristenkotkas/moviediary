package server.service;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public interface BankLinkService {

    static BankLinkService create(Vertx vertx, JsonObject config) {
        return new BankLinkServiceImpl(vertx, config);
    }

    Future<String> createPayment(String paymentDetails);

    Future<JsonObject> getPaymentSolutionById(String id);

    Future<JsonObject> getPaymentSolutions();

    Future<JsonObject> createPaymentSolution(JsonObject solutionDetails);
}
