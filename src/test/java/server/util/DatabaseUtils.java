package server.util;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;

/**
 * @author <a href="https://bitbucket.org/kristjanhk/">Kristjan Hendrik Küngas</a>
 */
public class DatabaseUtils {
    private final JDBCClient client;

    public DatabaseUtils(Vertx vertx, JsonObject config) {
        config.put("url", "jdbc:hsqldb:mem:test?shutdown=true")
                .put("driver_class", "org.hsqldb.jdbcDriver")
                .put("max_pool_size", 30);
        // TODO: 20.04.2017 make sure login works
        // TODO: 20.04.2017 swap out mysql to hsqldb stuff in config
        this.client = JDBCClient.createShared(vertx, config);
        // TODO: 20.04.2017 create tables
        // TODO: 20.04.2017 add test data
        // TODO: 20.04.2017 methods for resetting data etc.
    }
}
