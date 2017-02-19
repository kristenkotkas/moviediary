package server.service;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * Created by Alar on 19/02/2017.
 */
public interface BankLinkService {

    static BankLinkService create(Vertx vertx, JsonObject config) {
        return new BankLinkServiceImpl(vertx, config);
    }

    public Future<JsonObject> getPaymentById(String id);

    public Future<JsonObject> getPayments();
}
