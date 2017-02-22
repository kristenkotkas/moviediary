package server.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class StringUtils {
    public static final int SHORT_DATE = 1;
    public static final int LONG_DATE = 2;
    private static Map<String, String> months = new HashMap<String, String>() {{
        put("January,","01");
        put("February,","02");
        put("March,","03");
        put("April,","04");
        put("May,","05");
        put("June,","06");
        put("July,","07");
        put("August,","08");
        put("September,","09");
        put("October,","10");
        put("November,","11");
        put("December,","12");
    }};

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

    public static String toNormalTime(String dateTime) {
        //15:23 <- 2017-02-21T15:46:11Z
        LocalDateTime time = LocalDateTime.parse(dateTime.substring(0, dateTime.length() - 1));
        String hour = Integer.toString(time.getHour());
        String min = Integer.toString(time.getMinute());

        return (hour.length() == 1 ? "0" + hour : hour) + ":" +
                (min.length() == 1 ? "0" + min : min);
    }

    public static String getNormalDTFromDB(String date, int type) {
        return getNormalDate(LocalDateTime.parse(date.substring(0, date.length()-1)), type);
    }

    public static String toCapital(String string) {
        String result = "";

        result += string.substring(0, 1).toUpperCase();
        result += string.substring(1, string.length()).toLowerCase();

        return result;
    }

    public static String getWeekdayFromDB(String date) {
        return LocalDateTime.parse(date.substring(0, date.length()-1)).getDayOfWeek().toString();
    }

    public static String getFirstSeen(boolean string) {
        return string ? "\"fa fa-eye text-green\"" : ""; // FIXME: 21. veebr. 2017 PARANDADA
    }

    public static String getCinema(boolean string) {
        return string ? "\"fa fa-ticket text-green\"" : ""; // FIXME: 21. veebr. 2017 PARANDADA
    }

    public static String formToDBDate(String date, boolean isEnd) {
        //"22 February, 2017" -> 2017-02-22T
        if (!date.equals("")) {
            String[] parts = date.split(" ");
            if (isEnd) return LocalDate.parse((parts[2] + "-" + months.get(parts[1]) + "-") + parts[0]).plusDays(1).toString() + "T";
            else return (parts[2] + "-" + months.get(parts[1]) + "-") + parts[0] + "T";
        } return "";
    }


}
