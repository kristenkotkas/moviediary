package database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import util.rx.DbRxWrapper;
import static io.vertx.rx.java.RxHelper.toSubscriber;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
public class DatabaseServiceImpl extends DbRxWrapper implements DatabaseService {

  public DatabaseServiceImpl(Vertx vertx, JsonObject config) {
    super(vertx, config);
  }

  @Override
  public DatabaseService getAllUsers(Handler<AsyncResult<JsonArray>> handler) {
    query("SELECT * FROM USERS").subscribe(toSubscriber(handler));
    return this;
  }
}
