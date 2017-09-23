package util.rx;

import common.util.ClassUtils;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import rx.Single;
import util.MappingUtils;
import java.util.Optional;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
public class DbRxWrapper extends BaseDbRxWrapper {

  public DbRxWrapper(Vertx vertx, JsonObject config) {
    super(vertx, config);
  }

  protected <T> Single<Optional<T>> queryFirstChecked(String sql, JsonArray params, Class<T> clazz) {
    return query(sql, params)
        .map(MappingUtils::getRows)
        .map(array -> array.size() > 0 ? array.getValue(0) : null)
        .map(obj -> Optional.ofNullable(ClassUtils.checkedCast(obj, clazz)));
  }
}
