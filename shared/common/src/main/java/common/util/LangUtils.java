package common.util;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
public class LangUtils {

  /**
   * Capitalizes given text.
   *
   * @param text to use
   * @return capitalized text string
   */
  public static String capitalize(String text) {
    return text.substring(0, 1).toUpperCase() + text.substring(1);
  }
}
