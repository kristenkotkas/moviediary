package server.template.engine;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.ValueResolver;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.github.jknack.handlebars.io.TemplateSource;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.Utils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * Handlebars template engine implementation.
 * Allows use of default methods within template interfaces.
 * Allows value resolving of primitive types, strings, maps, javaBean objects, jsonObjects and jsonArrays.
 * <p>
 * Original source: https://github.com/vert-x3/vertx-web/blob/master/vertx-template-engines/vertx-web-templ-handlebars/
 *
 * @author <a href="https://bitbucket.org/kristjanhk/">Kristjan Hendrik Küngas</a>
 */
public class HandlebarsTemplateEngineImpl extends CachingTemplateEngine<Template> implements HandlebarsTemplateEngine {
    private final Handlebars handlebars;
    private final Loader loader = new Loader();
    private final Constructor<MethodHandles.Lookup> methodHandlesConstructor = MethodHandles.Lookup.class
            .getDeclaredConstructor(Class.class, int.class);
    private final ValueResolver[] resolvers = ArrayUtils.addAll(ValueResolver.VALUE_RESOLVERS,
            JsonArrayValueResolver.INSTANCE, JsonObjectValueResolver.INSTANCE);

    protected HandlebarsTemplateEngineImpl() throws NoSuchMethodException {
        super(HandlebarsTemplateEngine.DEFAULT_TEMPLATE_EXTENSION, HandlebarsTemplateEngine.DEFAULT_MAX_CACHE_SIZE);
        methodHandlesConstructor.setAccessible(true);
        handlebars = new Handlebars(loader).with((context, var) -> "null");
    }

    /**
     * Loads template with given filename from classpath, caches it and returns it as given type.
     * Sets response content type as text/html.
     *
     * @param ctx      to get vertx and response from
     * @param fileName to use
     * @param type     to convert loaded template to
     * @return type safe template
     */
    @Override
    public <S extends TypeSafeTemplate> S getSafeTemplate(RoutingContext ctx, String fileName, Class<S> type) {
        try {
            Template template = cache.get(fileName);
            if (template == null) {
                synchronized (this) {
                    loader.setVertx(ctx.vertx());
                    template = handlebars.compile(fileName);
                    cache.put(fileName, template);
                }
            }
            ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaders.TEXT_HTML);
            return getTypeSafeTemplate(template, type, ctx);
        } catch (IOException e) {
            ctx.fail(new FileNotFoundException("Template file: " + fileName + " was not found."));
        }
        return null;
    }

    @Override
    public void render(TypeSafeTemplate template, Handler<AsyncResult<Buffer>> handler) {
        try {
            handler.handle(Future.succeededFuture(Buffer.buffer(template.render())));
        } catch (Exception e) {
            handler.handle(Future.failedFuture(e));
        }
    }

    @Override
    public Handlebars getHandlebars() {
        return handlebars;
    }

    /**
     * Converts template to type safe template.
     * Template and type must not be null and type must be an interface.
     *
     * @param template to convert
     * @param type     to convert to
     * @param ctx      to use
     * @return type safe template
     */
    @SuppressWarnings("unchecked")
    private <S extends TypeSafeTemplate> S getTypeSafeTemplate(Template template, Class<S> type, RoutingContext ctx) {
        if (template != null && type != null && type.isInterface()) {
            return (S) createTypeSafeTemplate(template, type, ctx);
        }
        return null;
    }

    /**
     * Proxies template to create type safety.
     * Variables set using set methods will be saved.
     * Default methods will be called as usual.
     * Render method will render the template.
     * All other methods will be ignored and will return null.
     *
     * @param template to use
     * @param type     to use
     * @param ctx      to use
     * @return proxied template
     */
    private Object createTypeSafeTemplate(Template template, Class<?> type, RoutingContext ctx) {
        Map<String, Object> attributes = new HashMap<>();
        return Proxy.newProxyInstance(template.getClass().getClassLoader(), new Class[]{type},
                (proxy, method, args) -> {
                    if (method.isDefault()) {
                        Class<?> declaringClass = method.getDeclaringClass();
                        return methodHandlesConstructor.newInstance(declaringClass, MethodHandles.Lookup.PRIVATE)
                                .unreflectSpecial(method, declaringClass)
                                .bindTo(proxy)
                                .invokeWithArguments(args);
                    }
                    String methodName = method.getName();
                    if ("render".equals(methodName)) {
                        Context context = Context.newBuilder(ctx.data())
                                .combine(attributes)
                                .resolver(resolvers)
                                .build();
                        attributes.clear();
                        return template.apply(context).trim();
                    }
                    if (Modifier.isPublic(method.getModifiers()) && methodName.startsWith("set")) {
                        String attrName = StringUtils.uncapitalize(methodName.substring("set".length()));
                        if (args != null && args.length == 1 && attrName.length() > 0) {
                            attributes.put(attrName, args[0]);
                            if (TypeSafeTemplate.class.isAssignableFrom(method.getReturnType())) {
                                return proxy;
                            }
                        }
                    }
                    return null;
                });
    }

    /**
     * Template loader from classpath.
     */
    private class Loader implements TemplateLoader {
        private Vertx vertx;

        private void setVertx(Vertx vertx) {
            this.vertx = vertx;
        }

        @Override
        public TemplateSource sourceAt(String location) throws FileNotFoundException {
            try {
                String adjustedLocation = adjustLocation(location);
                String template = Utils.readFileToString(vertx, adjustedLocation);
                if (template == null) {
                    throw new IllegalArgumentException("Cannot find resource " + adjustedLocation);
                }
                long lastMod = System.currentTimeMillis();
                return new TemplateSource() {
                    @Override
                    public String content() throws IOException {
                        return template;
                    }

                    @Override
                    public String filename() {
                        return adjustedLocation;
                    }

                    @Override
                    public long lastModified() {
                        return lastMod;
                    }
                };
                // TODO: 26/01/2017 should catch only a sub exception?
            } catch (VertxException e) {
                throw new FileNotFoundException(e.getMessage());
            }
        }

        @Override
        public String resolve(String location) {
            return location;
        }

        @Override
        public String getPrefix() {
            return null;
        }

        @Override
        public String getSuffix() {
            return extension;
        }
    }
}