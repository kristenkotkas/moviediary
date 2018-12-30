package server.security;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.sstore.LocalSessionStore;
import io.vertx.rxjava.ext.web.sstore.SessionStore;
import org.pac4j.core.client.Client;
import org.pac4j.core.config.Config;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.vertx.auth.Pac4jAuthProvider;
import org.pac4j.vertx.context.session.VertxSessionStore;
import server.service.DatabaseService;

import java.util.Arrays;
import java.util.function.BiFunction;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static server.security.SecurityConfig.AuthClient.getCallback;
import static server.security.SecurityConfig.AuthClient.values;
import static server.util.NetworkUtils.isServer;

/**
 * Contains login clients used for user authentication.
 */
public class SecurityConfig {
    public static final String CLIENT_VERIFIED_STATE = "SSL_CLIENT_VERIFY";
    public static final String CLIENT_CERTIFICATE = "SSL_CLIENT_CERT";
    public static final String AUTHORIZER = "DatabaseAuthorizer";
    public static final String CSRF_TOKEN = "csrfToken";

    public static final String PAC4J_EMAIL = "email";
    public static final String PAC4J_PASSWORD = "password";
    public static final String PAC4J_SERIAL = "serialnumber";
    public static final String PAC4J_FIRSTNAME = "first_name";
    public static final String PAC4J_LASTNAME = "family_name";
    public static final String PAC4J_ISSUER = "issuer";
    public static final String PAC4J_COUNTRY = "location";
    public static final String PAC4J_SALT = "salt";

    private final Pac4jAuthProvider authProvider = new Pac4jAuthProvider();
    private final Config pac4jConfig;
    private final SessionStore sessionStore;
    private final VertxSessionStore vertxSessionStore;

    /**
     * Initializes Pac4j security engine with authentication clients and database authorizer.
     */
    public SecurityConfig(Vertx vertx, JsonObject config, DatabaseService database) {
        this.sessionStore = LocalSessionStore.create(vertx);
        this.vertxSessionStore = new VertxSessionStore(sessionStore.getDelegate());
        this.pac4jConfig = new Config(getCallback(config), Arrays.stream(values())
                .map(client -> client.create(config))
                .collect(toList()));
        this.pac4jConfig.getClients().findClient(FormClient.class).enable(database, config);
        this.pac4jConfig.addAuthorizer(AUTHORIZER, new DatabaseAuthorizer(database));
    }


    public Config getPac4jConfig() {
        return pac4jConfig;
    }

    public Pac4jAuthProvider getAuthProvider() {
        return authProvider;
    }

    public SessionStore getSessionStore() {
        return sessionStore;
    }

    public VertxSessionStore getVertxSessionStore() {
        return vertxSessionStore;
    }

    /**
     * Defined user authenticators.
     */
    public enum AuthClient {
        REDIRECT("redirect", RedirectClient.class, (key, secret) -> new RedirectClient()),
        FORM("form", FormClient.class, (key, secret) -> new FormClient()),
        FACEBOOK("facebook", FacebookClient.class, (key, secret) -> {
            FacebookClient fb = new FacebookClient(key, secret);
            fb.setScope("email");
            return fb;
        }),
        GOOGLE("google", Google2Client.class, Google2Client::new),
        IDCARD("idcard", IdCardClient.class, (key, secret) -> new IdCardClient());

        private static final String OAUTH = "oauth";
        private static final String LOCAL_CALLBACK = "localCallback";
        private static final String CALLBACK = "callback";
        private static final String KEY = "key";
        private static final String SECRET = "secret";
        private static final String PREFIX = "?client_name=";

        private final String configKey;
        private final Class clientClass;
        private final BiFunction<String, String, Client> client;

        AuthClient(String configKey, Class clientClass, BiFunction<String, String, Client> client) {
            this.configKey = configKey;
            this.clientClass = clientClass;
            this.client = client;
        }

        public static String getCallback(JsonObject config) {
            if (!config.containsKey(OAUTH)) {
                return null;
            }
            return config.getJsonObject(OAUTH).getString(isServer(config) ? CALLBACK : LOCAL_CALLBACK);
        }

        public static String getClientNames() {
            return Arrays.stream(values())
                    .map(AuthClient::getClientName)
                    .collect(joining(","));
        }

        public Client create(JsonObject config) {
            JsonObject clientConfig = config
                .getJsonObject(OAUTH, new JsonObject())
                .getJsonObject(configKey, new JsonObject());
            return client.apply(clientConfig.getString(KEY), clientConfig.getString(SECRET));
        }

        public String getClientName() {
            return clientClass.getSimpleName();
        }

        public String getClientNamePrefixed() {
            return PREFIX + getClientName();
        }
    }
}