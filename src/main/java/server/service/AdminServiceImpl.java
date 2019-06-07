package server.service;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;

import static io.vertx.rxjava.core.Future.future;

public class AdminServiceImpl implements AdminService {

    public AdminServiceImpl(Vertx vertx) {
    }

    @Override
    public Future<JsonObject> updateMovie(String movieId) {
        return future(fut -> new JsonObject().put("movieId", movieId));
    }
}
