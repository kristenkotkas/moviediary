package server.service;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpClientResponse;
import server.entity.CacheItem;

import static io.vertx.rxjava.core.Future.future;
import static server.entity.Status.OK;
import static server.service.BankLinkServiceImpl.Cache.PAYMENT;
import static server.util.CommonUtils.check;
import static server.util.CommonUtils.ifTrue;

public class BankLinkServiceImpl extends CachingService<JsonObject> implements BankLinkService {
    private static final Logger LOG = LoggerFactory.getLogger(BankLinkServiceImpl.class);
    private static final int HTTP = 8083;
    private static final String ENDPOINT = "localhost";
    private static final String ENABLED = "use_pangalink";

    private static final String SOLUTIONS = "/api/project/";
    private static final String PAYMENTS = "/banklink/ipizza";

    private final JsonObject config;
    private final HttpClient client;

    protected BankLinkServiceImpl(Vertx vertx, JsonObject config) {
      super(CachingService.DEFAULT_MAX_CACHE_SIZE);
        this.config = config;
        this.client = vertx.createHttpClient();
    }

    @Override
    public Future<JsonObject> getPaymentSolutions() {
        return get(SOLUTIONS, getCached(PAYMENT.prefix));
    }

    @Override // TODO 23.02 save the details received (cert, private key, UID)
    public Future<JsonObject> createPaymentSolution(JsonObject solutionDetails) {
        return post(SOLUTIONS, solutionDetails, getCached(PAYMENT.prefix));
    }

    @Override
    public Future<JsonObject> getPaymentSolutionById(String id) {
        return get(SOLUTIONS + id, getCached(PAYMENT.get(id)));
    }

    private Future<JsonObject> get(String uri, CacheItem<JsonObject> cache) {
        //kas peaks olema cached?, kui jah siis kui kauaks
        return future(fut -> ifTrue(isEnabled(fut) && !tryCachedResult(false, cache, fut),
                () -> client.getNow(HTTP, ENDPOINT, uri, response -> handleResponse(response, cache, fut))));
    }

    private Future<JsonObject> post(String uri, JsonObject body, CacheItem<JsonObject> cache) {
        return future(fut -> ifTrue(isEnabled(fut), () -> client.post(HTTP, ENDPOINT, uri, response ->
                handleResponse(response, cache, fut))
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(body))));
    }

    private void handleResponse(HttpClientResponse response, CacheItem<JsonObject> cache, Future<JsonObject> future) {
        check(response.statusCode() == OK,
                () -> response.bodyHandler(body -> future.complete(cache.set(body.toJsonObject()))),
                () -> future.fail("API returned code: " + response.statusCode() +
                        "; message: " + response.statusMessage()));
    }

    private boolean isEnabled(Future future) {
        if (!config.getBoolean(ENABLED, false)) {
            future.fail("Pangalink usage is disabled!");
            return false;
        }
        return true;
    }

    public enum Cache {
        PAYMENT("payment_");

        private final String prefix;

        Cache(String prefix) {
            this.prefix = prefix;
        }

        public String get(String id) {
            return prefix + id;
        }
    }
}