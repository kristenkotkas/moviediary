package server.util;

import java.time.LocalDateTime;
import java.util.SplittableRandom;

public class StringUtils {
  public static final SplittableRandom RANDOM = new SplittableRandom();
  public static final int SHORT_DATE = 1;
  public static final int LONG_DATE = 2;

  /**
   * Returns String representing given LocalDateTime.
   *
   * @param date to format
   * @return date as string
   */
  public static String getNormalDate(LocalDateTime date, int type) {
    String returnString = date.getDayOfMonth() + " " + date.getMonth().toString().substring(0, 1).toUpperCase();
    return returnString + (type == SHORT_DATE ?
        date.getMonth().toString().substring(1, 3).toLowerCase() :
        date.getMonth().toString().substring(1).toLowerCase()) +
        " " + date.getYear();
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
    return getNormalDate(LocalDateTime.parse(date.substring(0, date.length() - 1)), type);
  }

  public static String getWeekdayFromDB(String date) {
    return LocalDateTime.parse(date.substring(0, date.length() - 1)).getDayOfWeek().toString();
  }

  public static String getFirstSeen(boolean string) {
    return string ? "fa fa-eye" : ""; // FIXME: 21. veebr. 2017 PARANDADA
  }

  public static String getCinema(boolean string) {
    return string ? "fa fa-ticket new" : ""; // FIXME: 21. veebr. 2017 PARANDADA
  }
}
