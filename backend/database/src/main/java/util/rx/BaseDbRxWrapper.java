package util.rx;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import io.vertx.rxjava.ext.sql.SQLConnection;
import rx.Single;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
public class BaseDbRxWrapper {
  protected final JDBCClient client;

  // TODO: 23.08.2017 try https://github.com/jklingsporn/vertx-jooq-async

  public BaseDbRxWrapper(Vertx vertx, JsonObject config) {
    this.client = JDBCClient
        .createNonShared(new io.vertx.rxjava.core.Vertx(vertx), config.getJsonObject("mysql", config));
    // TODO: 23.08.2017 get keys from (global?) .properties
  }

  protected JsonArray getRows(JsonObject json) {
    return json.getJsonArray("rows", new JsonArray());
  }

  protected Single<SQLConnection> getConnection() {
    return client.rxGetConnection()
                 .flatMap(conn -> Single.just(conn).doOnUnsubscribe(conn::close));
  }

  protected Single<JsonObject> execute(String sql, JsonArray params) {
    return getConnection()
        .flatMap(conn -> conn.rxUpdateWithParams(sql, params))
        .map(UpdateResult::toJson);
  }

  protected Single<JsonObject> execute(String sql) {
    return getConnection()
        .flatMap(conn -> conn.rxUpdate(sql))
        .map(UpdateResult::toJson);
  }

  protected Single<UpdateResult> executeResult(String sql, JsonArray params) {
    return getConnection()
        .flatMap(conn -> conn.rxUpdateWithParams(sql, params));
  }

  protected Single<Void> executeNoResult(String sql, JsonArray params) {
    return getConnection()
        .flatMap(conn -> conn.rxUpdateWithParams(sql, params))
        .map(result -> (Void) null);
  }

  protected Single<JsonArray> query(String sql) {
    return getConnection()
        .flatMap(conn -> conn.rxQuery(sql))
        .map(ResultSet::toJson)
        .map(this::getRows);
  }

  protected Single<JsonArray> query(String sql, JsonArray params) {
    return getConnection()
        .flatMap(conn -> conn.rxQueryWithParams(sql, params))
        .map(ResultSet::toJson)
        .map(this::getRows);
  }

  protected Single<ResultSet> queryResult(String sql, JsonArray params) {
    return getConnection()
        .flatMap(conn -> conn.rxQueryWithParams(sql, params));
  }
}
