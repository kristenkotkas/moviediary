package database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import rx.Single;
import static io.vertx.rx.java.RxHelper.toSubscriber;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
public class DatabaseServiceImpl implements DatabaseService {
  private final JDBCClient jdbcClient;

  public DatabaseServiceImpl(io.vertx.ext.jdbc.JDBCClient jdbcClient) {
    this.jdbcClient = new JDBCClient(jdbcClient);
  }

  @Override
  public DatabaseService getAllUsers(Handler<AsyncResult<JsonObject>> handler) {
    jdbcClient.rxGetConnection()
              .flatMap(conn -> Single.just(conn).doOnUnsubscribe(conn::close))
              .flatMap(conn -> conn.rxQuery("SELECT * FROM Users"))
              .map(ResultSet::toJson)
              .subscribe(toSubscriber(handler));
    return this;
  }
}
