package server.entity.admin;

import io.vertx.rxjava.core.MultiMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter(AccessLevel.PRIVATE)
public class AdminSessionsParams extends TimelineParams {

    private boolean sessions;
    private Integer minCount;
    private Integer minMaxDiff;

    public AdminSessionsParams(MultiMap params) {
        super(params);
        setValue(this::setSessions, Boolean::valueOf, "sessions", REGEX_BOOLEAN);
        setValue(this::setMinCount, Integer::valueOf, "min_count", REGEX_NUMBER);
        setValue(this::setMinMaxDiff, Integer::valueOf, "min_diff", REGEX_NUMBER);
    }

}
