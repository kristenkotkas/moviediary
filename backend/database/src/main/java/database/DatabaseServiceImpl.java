package database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import rx.Single;
import util.MappingUtils;
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
  public DatabaseService getAllUsers(Handler<AsyncResult<JsonObject>> handler) {
    query("SELECT * FROM Users").map(MappingUtils::toRowsJson).subscribe(toSubscriber(handler));
    return this;
  }

  @Override
  public DatabaseService getTestUsers(String input, int test, JsonObject in2, JsonArray in3, Handler<AsyncResult<JsonObject>> handler) {
    JsonObject json = new JsonObject();
    json.put("returnInput", input);
    json.put("returnTest", test);
    json.put("returnIn2", in2);
    json.put("returnIn3", in3);
    Single.just(json).subscribe(toSubscriber(handler));
    return this;
  }
}
