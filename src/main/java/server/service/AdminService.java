package server.service;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;

public interface AdminService {

    static AdminService create(Vertx vertx) {
        return new AdminServiceImpl(vertx);
    }

    Future<JsonObject> updateMovie(String movieId);

}
