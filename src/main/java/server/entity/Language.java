package server.entity;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import static server.router.AuthRouter.LANGUAGE;

/**
 * Handles getting strings from ResourceBundles.
 */
public enum Language {
    ENGLISH(Locale.UK),
    ESTONIAN(new Locale.Builder().setLanguage("et").setScript("Latn").setRegion("EE").build()),
    GERMAN(new Locale.Builder().setLanguage("de").setScript("Latn").setRegion("GR").build());

    private static final Logger LOG = LoggerFactory.getLogger(Language.class);
    private final Locale locale;
    private final ResourceBundle bundle;

    Language(Locale locale) {
        this.locale = locale;
        this.bundle = ResourceBundle.getBundle("messages", locale, new UTF8Control());
    }

    public static String getString(String key, String lang) {
        return Stream.of(values())
                .filter(language -> language.getLocale().getLanguage().equals(lang))
                .findAny()
                .orElse(ENGLISH)
                .getString(key);
    }

    public static String getString(String key, RoutingContext ctx) {
        return getString(key, (String) ctx.session().data().get(LANGUAGE));
    }

    public Locale getLocale() {
        return locale;
    }

    public String getString(String key) {
        LOG.info("i18n: lang:" + locale.getLanguage() + ", key: " + key + ", val: " + bundle.getString(key));
        return bundle.getString(key);
    }

    /**
     * Implementation of reading a ResourceBundle in UTF-8 format.
     */
    private static class UTF8Control extends ResourceBundle.Control {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format,
                                        ClassLoader loader, boolean reload)
                throws IllegalAccessException, InstantiationException, IOException {
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
                    bundle = new PropertyResourceBundle(new InputStreamReader(stream, "UTF-8"));
                } finally {
                    stream.close();
                }
            }
            return bundle;
        }
    }
}
