package common.util;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
public class NetworkUtils {

  public static int getRandomUnboundPort() {
    try (final ServerSocket serverSocket = new ServerSocket(0)) {
      return serverSocket.getLocalPort();
    } catch (IOException ignored) {
    }
    return -1;
  }
}
