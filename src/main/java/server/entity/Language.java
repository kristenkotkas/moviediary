package server.entity;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.ext.web.Cookie;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import static io.vertx.rxjava.core.Future.succeededFuture;

/**
 * Handles getting strings from ResourceBundles.
 */
public enum Language {
    ENGLISH(Locale.UK),
    ESTONIAN(new Locale.Builder().setLanguage("et").setScript("Latn").setRegion("EE").build()),
    GERMAN(new Locale.Builder().setLanguage("de").setScript("Latn").setRegion("GR").build());

    public static final String LANGUAGE = "lang";
    private final Locale locale;
    private final ResourceBundle bundle;

    Language(Locale locale) {
        this.locale = locale;
        this.bundle = ResourceBundle.getBundle("messages", locale, new UTF8Control());
    }

    public static String getString(String key, String lang) {
        return key == null ? null : getLanguageOrDefault(lang).getString(key);
    }

    public static String getString(String key, RoutingContext ctx) {
        Cookie lang = ctx.getCookie(LANGUAGE);
        return getString(key, lang != null ? lang.getValue() : null);
    }

    public static Future<JsonObject> getJsonTranslations(String lang) {
        ResourceBundle bundle = getLanguageOrDefault(lang).getBundle();
        return succeededFuture(bundle.keySet().stream()
                .collect(JsonObject::new, (json, key) -> json.put(key, bundle.getString(key)), JsonObject::mergeIn));
    }

    public static String validate(String lang) {
        return getLanguageOrDefault(lang).toString();
    }

    private static Language getLanguageOrDefault(String lang) {
        return Stream.of(values())
                     .filter(language -> language.getLocale().getLanguage().equals(lang))
                     .findAny()
                     .orElse(ENGLISH);
    }

    public Locale getLocale() {
        return locale;
    }

    public ResourceBundle getBundle() {
        return bundle;
    }

    public String getString(String key) {
        return bundle.getString(key);
    }

    /**
     * Implementation of reading a ResourceBundle in UTF-8 format.
     */
    private static class UTF8Control extends ResourceBundle.Control {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format,
                                        ClassLoader loader, boolean reload) throws IOException {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            ResourceBundle bundle = null;
            InputStream stream = null;
            if (reload) {
                URL url = loader.getResource(resourceName);
                if (url != null) {
                    URLConnection connection = url.openConnection();
                    if (connection != null) {
                        connection.setUseCaches(false);
                        stream = connection.getInputStream();
                    }
                }
            } else {
                stream = loader.getResourceAsStream(resourceName);
            }
            if (stream != null) {
                try {
                    bundle = new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
                } finally {
                    stream.close();
                }
            }
            return bundle;
        }
    }
}
