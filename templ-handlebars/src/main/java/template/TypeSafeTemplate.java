package template;

import java.io.IOException;

/**
 * Type safe template.
 * See Handlebars engine implementation for details.
 */
public interface TypeSafeTemplate {
  String render() throws IOException;
}
