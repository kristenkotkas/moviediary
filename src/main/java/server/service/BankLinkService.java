package server.service;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;

public interface BankLinkService {

    static BankLinkService create(Vertx vertx, JsonObject config) {
        return new BankLinkServiceImpl(vertx, config);
    }

    Future<JsonObject> getPaymentSolutionById(String id);

    Future<JsonObject> getPaymentSolutions();

    Future<JsonObject> createPaymentSolution(JsonObject solutionDetails);
}
