package database;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@VertxGen
@ProxyGen
public interface DatabaseService {
  String ADDRESS = "backend.database";

  static DatabaseService create(JDBCClient jdbcClient) {
    return new DatabaseServiceImpl(jdbcClient);
  }

  static DatabaseService createProxy(Vertx vertx) {
    return new DatabaseServiceVertxEBProxy(vertx, ADDRESS);
  }

  @Fluent
  DatabaseService getAllUsers(Handler<AsyncResult<JsonObject>> handler);
}
