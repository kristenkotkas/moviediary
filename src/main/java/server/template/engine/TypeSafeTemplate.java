package server.template.engine;

import java.io.IOException;

/**
 * Type safe template.
 * See Handlebars engine implementation for details.
 *
 * @author <a href="https://bitbucket.org/kristjanhk/">Kristjan Hendrik Küngas</a>
 */
public interface TypeSafeTemplate {
    String render() throws IOException;
}
