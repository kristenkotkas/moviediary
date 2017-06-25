package util;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static javax.xml.bind.DatatypeConverter.printHexBinary;

public class StringUtils {
  private static final Logger LOG = LoggerFactory.getLogger(StringUtils.class);

  public static String hash(String input, String salt) {
    try {
      return bytesToString(MessageDigest.getInstance("SHA-256").digest(input.concat(salt).getBytes(UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      LOG.error("Hashing uses an invalid algorithm.", e);
    }
    return "null";
  }

  private static String bytesToString(byte[] array) {
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
      return bytesToString(bytes);
    } catch (NoSuchAlgorithmException e) {
      LOG.error("String generating uses an invalid algorithm.", e);
    }
    return "default";
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

  public static String parseParam(String requestUri) {
    return requestUri.split(":")[1];
  }
}
