package server.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.inject.internal.ImmutableMap;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static javax.xml.bind.DatatypeConverter.printHexBinary;

public class StringUtils {
    private static final Logger LOG = LoggerFactory.getLogger(StringUtils.class);
    public static final SplittableRandom RANDOM = new SplittableRandom();
    public static final int SHORT_DATE = 1;
    public static final int LONG_DATE = 2;
    private static ImmutableMap<String, String> MONTHS = ImmutableMap.<String, String>builder()
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
            .put("December,", "12").build();

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

    /**
     * Lowercases and capitalizes given name.
     *
     * @param name to use
     * @return capitalized text string
     */
    public static String capitalizeName(String name) {
        return Arrays.stream(name.split(" "))
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase())
                .collect(joining(" "));
    }


    public static String hash(String input, String salt) {
        try {
            return toString(MessageDigest.getInstance("SHA-256").digest(input.concat(salt).getBytes(UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Hashing uses an invalid algorithm.", e);
        }
        return "null";
    }

    private static String toString(byte[] array) {
        return printHexBinary(array).toLowerCase();
    }

    /**
     * Generates 16 length string consisting of lowercase alphanumeric characters.
     *
     * @return generated string
     */
    public static String genString() {
        try {
            byte[] bytes = new byte[8];
            SecureRandom.getInstance("SHA1PRNG").nextBytes(bytes);
            return toString(bytes);
        } catch (NoSuchAlgorithmException e) {
            LOG.error("String generating uses an invalid algorithm.", e);
        }
        return "default";
    }

    public static String toXml(Object obj) {
        ObjectMapper mapper = new XmlMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <E extends Enum<E>> String getRegexForEnum(Class<E> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(value -> value.toString().toLowerCase())
                .collect(Collectors.joining("|"));
    }


}
