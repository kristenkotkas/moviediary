package launcher;

import lombok.extern.slf4j.Slf4j;
import java.io.IOException;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@Slf4j
public class Launcher {

  public static void main(String[] args) throws IOException {
    log.info("Starting...");
    Deployer.deployVerticles()
            .doOnError(err -> log.error("Verticle deployment failed.", err))
            .subscribe(s -> log.info("Verticle deployment succeeded."));
  }
}
