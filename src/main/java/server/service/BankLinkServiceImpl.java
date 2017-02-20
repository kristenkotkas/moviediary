package server.service;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import static server.entity.Status.OK;
import static server.service.BankLinkServiceImpl.Cache.PAYMENT;

public class BankLinkServiceImpl extends CachingServiceImpl<JsonObject> implements BankLinkService {
    private static final Logger log = LoggerFactory.getLogger(BankLinkServiceImpl.class);
    private static final int HTTP = 8083;
    private static final String ENDPOINT = "localhost" ;
    private static final String ENABLED = "use_pangalink";

    private static final String PAYMENTS = "/api/project/";

    private final Vertx vertx;
    private final JsonObject config;
    private final HttpClient client;

    protected BankLinkServiceImpl(Vertx vertx, JsonObject config) {
        super(CachingServiceImpl.DEFAULT_MAX_CACHE_SIZE);
        this.vertx = vertx;
        this.config = config;
        this.client = vertx.createHttpClient();
    }

    public Future<JsonObject> getPayments(){
        return get(PAYMENTS, getCached(PAYMENT.prefix));
    }

    @Override
    public Future<JsonObject> getPaymentById(String id) {
        return get(PAYMENTS + id, getCached(PAYMENT.get(id)));
    }

    private Future<JsonObject> get(String uri, CacheItem<JsonObject> cache) {
        Future<JsonObject> future = Future.future();
        if (isEnabled(future) && !tryCachedResult(false, cache, future)) { //kas peaks olema cached?, kui jah siis kui kauaks
            client.getNow(HTTP, ENDPOINT, uri, response -> handleResponse(response, cache, future));
        }
        return future;
    }

    private void handleResponse(HttpClientResponse response, CacheItem<JsonObject> cache, Future<JsonObject> future) {
        if (response.statusCode() == OK) {
            response.bodyHandler(body -> future.complete(cache.set(body.toJsonObject())));
        } else {
            future.fail("API returned code: " + response.statusCode() +
                    "; message: " + response.statusMessage());
        }
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