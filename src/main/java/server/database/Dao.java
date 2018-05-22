package server.database;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.conf.RenderNameStyle;
import org.jooq.impl.DSL;
import server.entity.FunctionThrowingEx;
import server.util.FutureUtils;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@RequiredArgsConstructor
public abstract class Dao {
  protected final Vertx vertx;
  protected final Configuration jooq;

  public DSLContext dsl() {
    DSLContext dsl = DSL.using(jooq);
    dsl.settings().setRenderNameStyle(RenderNameStyle.QUOTED);
    return dsl;
  }

  protected <T> Future<T> async(FunctionThrowingEx<DSLContext, T> func) {
    Objects.requireNonNull(func);
    return Future.future(fut -> vertx.executeBlocking(h -> {
      try {
        h.complete(func.apply(dsl()));
      } catch (Exception e) {
        h.fail(e.getCause());
      }
    }, FutureUtils.fromRx(fut)));
  }

  protected Future<JsonObject> asyncJson(FunctionThrowingEx<DSLContext, Map<String, Object>> func) {
    return async(dsl -> new JsonObject(func.apply(dsl)));
  }

  protected Future<JsonArray> asyncArray(FunctionThrowingEx<DSLContext, List<Map<String, Object>>> func) {
    return async(dsl -> toArray(func.apply(dsl)));
  }

  private JsonArray toArray(List<Map<String, Object>> list) {
    return list.stream()
        .map(JsonObject::new)
        .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
  }

  protected <T extends Record> Future<Boolean> asyncInsertIgnore(Table<T> table, Object pojo) {
    return async(dsl -> dsl
            .insertInto(table)
            .set(dsl.newRecord(table, pojo))
            .onDuplicateKeyIgnore()
            .execute() > 0);
  }
}
