package launcher;

import common.util.CommonUtils;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
public class Launcher {

  public static void main(String[] args) {
    CommonUtils.setLoggingToSLF4J();
    Deployer.deployVerticles(args);
  }
}
