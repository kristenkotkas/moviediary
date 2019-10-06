package server.entity.admin;

import io.vertx.rxjava.core.MultiMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter(AccessLevel.PRIVATE)
public class AdminCountParams extends TimelineParams {

    private final String REGEX_AGR = Aggregation.getValidationRegex();

    private boolean sum;
    private Aggregation aggregation;

    public AdminCountParams(MultiMap params) {
        super(params);
        setValue(this::setSum, Boolean::valueOf, "sum", REGEX_BOOLEAN);
        setValue(this::setAggregation, value -> Aggregation.valueOf(value.toUpperCase()), "agr", REGEX_AGR);
    }

}
