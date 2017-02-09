package sys.util;

import io.vertx.core.json.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.stream.Collectors;

public class FileUtils {

    /**
     * Loads config from classpath.
     *
     * @return local.json config
     * @throws IOException when not found
     */
    public static JsonObject getConfig() throws IOException {
        return new JsonObject(readToString("/local.json"));
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
     *
     * @param args to convert
     * @return jsonObject
     */
    public static JsonObject parseArguments(String[] args) {
        return new JsonObject(Arrays.stream(args)
                .filter(s -> s.startsWith("-"))
                .map(s -> s.replaceFirst("-", "").split("="))
                .collect(Collectors.toMap(s -> s[0], s -> s[1])));
    }
}
