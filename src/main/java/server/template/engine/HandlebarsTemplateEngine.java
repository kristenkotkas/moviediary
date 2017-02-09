package server.template.engine;

import com.github.jknack.handlebars.Handlebars;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;

/**
 * Handlebars template engine.
 * Allows use of type safe templates.
 *
 * @author <a href="https://bitbucket.org/kristjanhk/">Kristjan Hendrik Küngas</a>
 */
public interface HandlebarsTemplateEngine {
    int DEFAULT_MAX_CACHE_SIZE = 10000;
    String DEFAULT_TEMPLATE_EXTENSION = "hbs";

    static HandlebarsTemplateEngine create() throws Exception {
        return new HandlebarsTemplateEngineImpl();
    }

    /**
     * Loads template with given filename from classpath and returns it as given type.
     *
     * @param ctx      to get vertx and response from
     * @param fileName to use
     * @param type     to convert loaded template to
     * @return type safe template
     */
    <S extends TypeSafeTemplate> S getSafeTemplate(RoutingContext ctx, String fileName, Class<S> type);

    /**
     * Renders given template and passes completed future to handler.
     *
     * @param template to render
     * @param handler  to call asynchronously
     */
    void render(TypeSafeTemplate template, Handler<AsyncResult<Buffer>> handler);

    Handlebars getHandlebars();
}