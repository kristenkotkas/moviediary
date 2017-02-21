package server.util;

import java.time.LocalDateTime;

public class StringUtils {
    public static final int SHORT_DATE = 1;
    public static final int LONG_DATE = 2;

    /**
     * Returns String representing given LocalDateTime.
     *
     * @param date to format
     * @return date as string
     */
    public static String getNormalDate(LocalDateTime date, int type) {
        String returnString = Integer.toString(date.getDayOfMonth()) + " " +
                date.getMonth().toString().substring(0, 1).toUpperCase();
        if (type == SHORT_DATE) {
            returnString += date.getMonth().toString().substring(1, 3).toLowerCase();
        } else {
            returnString += date.getMonth().toString().substring(1).toLowerCase();
        }
        returnString += " " + date.getYear();
        return returnString;
    }

    public static String getNormalDTFromDB(String date, int type) {
        return getNormalDate(LocalDateTime.parse(date.substring(0, date.length()-1)), type);
    }

    public static String getNormalBoolean(boolean string) {
        return string ? "\"fa fa-check text-green\"" : ""; // FIXME: 21. veebr. 2017 PARANDADA
    }
}
