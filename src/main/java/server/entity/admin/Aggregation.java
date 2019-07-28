package server.entity.admin;

import lombok.Getter;
import server.util.StringUtils;

@Getter
public enum Aggregation {

    HOUR("Hour"), DAY("DayOfMonth"), WEEK("WeekDay"), MONTH("Month");

    private String value;

    Aggregation(String value) {
        this.value = value;
    }

    public static String getValidationRegex() {
        return StringUtils.getRegexForEnum(Aggregation.class);

    }
}
