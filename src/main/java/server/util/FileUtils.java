package server.util;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import server.entity.JsonObj;

import java.io.*;
import java.util.Arrays;
import java.util.stream.Collectors;

public class FileUtils {
    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);
    private static final String CONFIG = "/server.json";

    public static final String UI_CACHE = "uiCaching";

    /**
     * Loads config from classpath.
     *
     * @return server.json config
     */
    public static JsonObject getConfig(String[] args) {
        try {
            JsonObject json = new JsonObj(readToString(CONFIG)).mergeIn(parseArguments(args));
            if (isRunningFromJar()) {
                json.remove(UI_CACHE);
            }
            return json;
        } catch (IOException e) {
            log.error(CONFIG + " not found.");
        }
        return new JsonObj();
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
     * Example: -key1=value1 -key2=value2 ...
     *
     * @param args to convert
     * @return jsonObject
     */
    private static JsonObj parseArguments(String[] args) {
        return args == null ? new JsonObj() : new JsonObj(Arrays.stream(args)
                .filter(s -> s.startsWith("-"))
                .map(s -> s.replaceFirst("-", "").split("="))
                .collect(Collectors.toMap(s -> s[0], s -> s[1])));
    }

    private static String getJarName() {
        return new File(FileUtils.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath())
                .getName();
    }

    public static boolean isRunningFromJar() {
        return getJarName().contains(".jar");
    }
}