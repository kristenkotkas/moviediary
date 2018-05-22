package server.database;

import io.vertx.core.Vertx;
import java.sql.Timestamp;
import org.jooq.Configuration;
import org.jooq.DatePart;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
public abstract class CommonDao extends Dao {

  public CommonDao(Vertx vertx, Configuration jooq) {
    super(vertx, jooq);
  }

  protected Field<Integer> dayOfWeek(Field<Timestamp> field) {
    return DSL.field("DAY_OF_WEEK({0})", SQLDataType.INTEGER, field);
  }

  protected Field<Integer> dayOfMonth(Field<Timestamp> field) {
    return DSL.field("DAY_OF_MONTH({0})", SQLDataType.INTEGER, field);
  }

  protected Field<Integer> timestampDiff(DatePart part, Field<Timestamp> t1, Field<Timestamp> t2) {
    return DSL.field("TIMESTAMPDIFF({0}, {1}, {2})", Integer.class, DSL.keyword(part.toSQL()), t1, t2);
  }
}
