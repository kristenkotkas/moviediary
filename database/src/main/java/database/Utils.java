package database;

import entity.MapBuilder;

import java.time.LocalDate;
import java.util.Map;

class Utils {
  private static Map<String, String> MONTHS = new MapBuilder<String, String>()
      .put("January,", "01")
      .put("February,", "02")
      .put("March,", "03")
      .put("April,", "04")
      .put("May,", "05")
      .put("June,", "06")
      .put("July,", "07")
      .put("August,", "08")
      .put("September,", "09")
      .put("October,", "10")
      .put("November,", "11")
      .put("December,", "12")
      .build();

  public static String formToDBDate(String date, boolean isEnd) {
    //"22 February, 2017" -> 2017-02-22T
    if (!date.equals("")) {
      String[] parts = date.split(" ");
      String day = parts[0].length() == 1 ? "0" + parts[0] : parts[0];
      if (isEnd) {
        return LocalDate.parse(parts[2] + "-" + MONTHS.get(parts[1]) + "-" + day).plusDays(1).toString() + "T";
      } else {
        return parts[2] + "-" + MONTHS.get(parts[1]) + "-" + parts[0] + "T";
      }
    }
    return "";
  }

  public static String movieDateToDBDate(String date) {
    //24 March, 2017 22:01 -> 2017-03-24 22:01:00
    String[] parts = date.split(" ");
    System.out.println("DATE....");
    System.out.println(date);
    return parts[2] + "-" + MONTHS.get(parts[1]) + "-" + parts[0] + " " +
        parts[3].split(":")[0] + ":" + parts[3].split(":")[1] + ":00";
  }
}
