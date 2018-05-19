/*
 * Copyright (C) 2018 Kristjan Hendrik Küngas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package server.database;

import io.vertx.core.json.JsonObject;
import java.sql.SQLException;
import lombok.Getter;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.Server;
import org.jooq.Configuration;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
public class DatabaseManager {
  private final JsonObject config;
  private final JdbcDataSource dataSource;
  private final Server server;

  @Getter
  private final Configuration jooqConfiguration;

  public DatabaseManager(JsonObject config) throws SQLException {
    this.config = config.getJsonObject("database");
    this.dataSource = createDataSource();
    this.server = Server.createTcpServer();
    this.jooqConfiguration = createJooqConfiguration();
  }

  public DatabaseManager start() throws SQLException {
     server.start();
     return this;
  }

  public void stop() {
    server.shutdown();
  }

  private JdbcDataSource createDataSource() {
    JdbcDataSource jdbcDataSource = new JdbcDataSource();
    jdbcDataSource.setURL(config.getString("url"));
    jdbcDataSource.setUser(config.getString("user"));
    jdbcDataSource.setPassword(config.getString("password"));
    return jdbcDataSource;
  }

  private Configuration createJooqConfiguration() {
    Configuration configuration = new DefaultConfiguration();
    configuration.set(SQLDialect.H2);
    configuration.set(dataSource);
    return configuration;
  }
}
