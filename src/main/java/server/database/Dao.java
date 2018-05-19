package server.database;

import javax.security.auth.login.Configuration;
import lombok.RequiredArgsConstructor;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@RequiredArgsConstructor
public abstract class Dao {
  protected final Configuration jooq;

  public void close() {

  }
}
