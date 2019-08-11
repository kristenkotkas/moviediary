package server.entity.admin;

import io.vertx.rxjava.core.MultiMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.function.Function;

@Getter
@Setter(AccessLevel.PRIVATE)
public class TimelineParams extends SearchParams {

    protected final String REGEX_YEAR = "20\\d{2}";
    protected final String REGEX_MONTH = "(0[1-9]|1[012])";
    protected final String REGEX_DAY = "([012][1-9]|[123]0|31)";
    protected final String REGEX_DATE = REGEX_YEAR + "-" + REGEX_MONTH + "-" + REGEX_DAY;

    protected Long year;
    protected Long month;
    protected Long day;
    protected String startDate;
    protected String endDate;

    public TimelineParams(MultiMap params) {
        super(params);
        setValue(this::setYear, Long::parseLong, "year", REGEX_YEAR);
        setValue(this::setMonth, Long::parseLong, "month", REGEX_MONTH);
        setValue(this::setDay, Long::parseLong, "day", REGEX_DAY);
        setValue(this::setStartDate, Function.identity(), "start_date", REGEX_DATE);
        setValue(this::setEndDate, Function.identity(), "end_date", REGEX_DATE);
    }

    protected boolean isParamsNotPresent() {
        return getYear() == null
                && getMonth() == null
                && getDay() == null
                && getStartDate() == null
                && getEndDate() == null;
    }
}
