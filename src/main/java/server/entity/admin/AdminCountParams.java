package server.entity.admin;

import io.vertx.rxjava.core.MultiMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@ToString
@Getter
@Setter(AccessLevel.PRIVATE)
public class AdminCountParams {

    private final MultiMap params;

    private final String REGEX_YEAR = "20\\d{2}";
    private final String REGEX_MONTH = "(0[1-9]|1[012])";
    private final String REGEX_DAY = "([012][1-9]|[123]0|31)";
    private final String REGEX_DATE = REGEX_YEAR + "-" + REGEX_MONTH + "-" + REGEX_DAY;
    private final String REGEX_BOOLEAN = "true|false";
    private final String REGEX_AGR = Aggregation.getValidationRegex();

    private Long year;
    private Long month;
    private Long day;
    private String startDate;
    private String endDate;
    private boolean sum;
    private Aggregation aggregation;

    private List<String> invalidParameters = new ArrayList<>();

    public AdminCountParams(MultiMap params) {
        this.params = params;
        setValue(this::setYear, Long::parseLong, "year", REGEX_YEAR);
        setValue(this::setMonth, Long::parseLong, "month", REGEX_MONTH);
        setValue(this::setDay, Long::parseLong, "day", REGEX_DAY);
        setValue(this::setStartDate, Function.identity(), "start_date", REGEX_DATE);
        setValue(this::setEndDate, Function.identity(), "end_date", REGEX_DATE);
        setValue(this::setSum, Boolean::valueOf, "sum", REGEX_BOOLEAN);
        setValue(this::setAggregation, value -> Aggregation.valueOf(value.toUpperCase()), "agr", REGEX_AGR);
    }

    public void checkParameters() throws BadRequestException {
        if (!invalidParameters.isEmpty()) {
            throw new BadRequestException("Invalid value for parameter: " + invalidParameters.toString());
        }

        if (isParamsNotPresent()) {
            throw new BadRequestException("At least 1 query parameter must exist.");
        }
    }

    private boolean isParamsNotPresent() {
        return getYear() == null
                && getMonth() == null
                && getDay() == null
                && getStartDate() == null
                && getEndDate() == null;
    }

    private <T> void setValue(Consumer<T> setter, Function<String, T> valueGetter, String param, String regex) {
        if (isValid(param, "^" + regex + "$")) {
            setter.accept(valueGetter.apply(getParamValue(param)));
        }
    }

    private String getParamValue(String param) {
        return params.get(param);
    }

    private boolean isValid(String param, String regex) {
        String value = getParamValue(param);
        if (value == null) {
            return false;
        }
        if (!value.matches(regex)) {
            invalidParameters.add(param + "=" + value);
            return false;
        }
        return true;
    }

}
