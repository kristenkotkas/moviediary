package server.entity.admin;

import io.vertx.rxjava.core.MultiMap;
import lombok.ToString;

@ToString
public class AdminSessionsParams extends TimelineParams {

    public AdminSessionsParams(MultiMap params) {
        super(params);
    }

}
