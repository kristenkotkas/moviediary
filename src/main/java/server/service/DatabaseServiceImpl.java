package server.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;

//näited -> https://github.com/vert-x3/vertx-examples/tree/master/jdbc-examples

public class DatabaseServiceImpl extends CachingServiceImpl<JsonObject> implements DatabaseService {
    private static final Logger log = LoggerFactory.getLogger(DatabaseServiceImpl.class);
    private static final String MYSQL = "mysql";
    private static final String RESULTS = "results";

    private final Vertx vertx;
    private final JsonObject config;
    private final JDBCClient client;

    private static final String SQL_QUERY_USERS = "SELECT * FROM Users";

    protected DatabaseServiceImpl(Vertx vertx, JsonObject config) {
        super(CachingServiceImpl.DEFAULT_MAX_CACHE_SIZE);
        this.vertx = vertx;
        this.config = config;
        this.client = JDBCClient.createShared(vertx, config.getJsonObject(MYSQL));
    }

    @Override
    public Future<JsonObject> getAllUsers() {
        Future<JsonObject> future = Future.future();
        CacheItem<JsonObject> cache = getCached(CACHE_ALL);
        if (!tryCachedResult(false, cache, future)) { //cache timeout peaks väikseks panema või mitte kasutama
            client.getConnection(connHandler(future,
                    conn -> conn.query(SQL_QUERY_USERS, resultHandler(conn, CACHE_ALL, future))));
        }
        return future;
    }

    private Handler<AsyncResult<SQLConnection>> connHandler(Future future, Handler<SQLConnection> handler) {
        return conn -> {
            if (conn.succeeded()) {
                handler.handle(conn.result());
            } else {
                future.fail(conn.cause());
            }
        };
    }

    private Handler<AsyncResult<ResultSet>> resultHandler(SQLConnection conn, String key, Future<JsonObject> future) {
        return ar -> {
            if (ar.succeeded()) {
                JsonObject json = ar.result().toJson();
                json.remove(RESULTS); //andmebaas tagastab liialt palju infot, jätame osa ära (rows ja results on sama info topelt)
                future.complete(getCached(key).set(json));
            } else {
                future.fail(ar.cause());
            }
            conn.close();
        };
    }
}
