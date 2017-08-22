package common.util;

import common.entity.JsonObj;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import java.io.*;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@Slf4j
public class FileUtils {
  private static final String COMMON = "/server.json";
  private static final boolean IS_RUNNING_FROM_JAR = getJarName().contains(".jar");

  /**
   * Loads config from classpath.
   *
   * @return server.json config
   */
  public static JsonObject getConfig() {
    return getConfig(null);
  }

  /**
   * Loads config from classpath.
   *
   * @return server.json config
   */
  public static JsonObject getConfig(String[] args) {
    JsonObject config = new JsonObj();
    try {
      config.mergeIn(new JsonObj(readToString(COMMON)));
    } catch (IOException e) {
      log.error(COMMON + " not found.");
    }
    return config.mergeIn(parseArguments(args));
  }

  /**
   * Reads file on given location on classpath into string.
   *
   * @param location to read from
   * @return file at that location as string
   * @throws IOException when not found
   */
  public static String readToString(String location) throws IOException {
    return readToString(FileUtils.class.getResourceAsStream(location));
  }

  /**
   * Reads inputStream into string.
   *
   * @param inputStream to read
   * @return stream as string
   * @throws IOException when not found
   */
  public static String readToString(InputStream inputStream) throws IOException {
    if (inputStream == null) {
      throw new FileNotFoundException();
    }
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int length;
    while ((length = inputStream.read(buffer)) != -1) {
      result.write(buffer, 0, length);
    }
    inputStream.close();
    return result.toString("UTF-8");
  }

  /**
   * Converts given args to JsonObject.
   * Example input: "-key1=value1", "-key2=value2", ...
   *
   * @param args to convert
   * @return jsonObj
   */
  private static JsonObj parseArguments(String... args) {
    return new JsonObj(stream(args)
        .filter(s -> s.startsWith("-"))
        .map(s -> s.replaceFirst("-", "").split("="))
        .collect(toMap(s -> s[0], s -> s[1])));
  }

  private static String getJarName() {
    return new File(FileUtils.class.getProtectionDomain()
                                   .getCodeSource()
                                   .getLocation()
                                   .getPath())
        .getName();
  }

  public static boolean isRunningFromJar() {
    return IS_RUNNING_FROM_JAR;
  }
}
