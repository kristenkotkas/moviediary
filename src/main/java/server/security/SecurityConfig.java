package server.security;

import io.vertx.core.json.JsonObject;
import org.pac4j.core.client.Client;
import org.pac4j.core.config.Config;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.oauth.client.Google2Client;
import org.pac4j.vertx.auth.Pac4jAuthProvider;
import server.service.DatabaseService;

import java.util.Arrays;
import java.util.function.BiFunction;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static server.security.SecurityConfig.AuthClient.getCallback;
import static server.security.SecurityConfig.AuthClient.values;
import static server.util.NetworkUtils.isServer;

public class SecurityConfig {
    public static final String CLIENT_VERIFIED_STATE = "SSL_CLIENT_VERIFY";
    public static final String CLIENT_CERTIFICATE = "SSL_CLIENT_CERT";
    public static final String AUTHORIZER = "CommonAuthorizer";

    public static final String PAC4J_EMAIL = "email";
    public static final String PAC4J_PASSWORD = "password";
    public static final String PAC4J_SERIAL = "serialnumber";
    public static final String PAC4J_FIRSTNAME = "first_name";
    public static final String PAC4J_LASTNAME = "family_name";
    public static final String PAC4J_ISSUER = "issuer";
    public static final String PAC4J_COUNTRY = "location";

    public static final String DB_EMAIL = "Email";
    public static final String DB_PASSWORD = "Password";
    public static final String DB_SERIAL = "Serialnumber";
    public static final String DB_FIRSTNAME = "Firstname";
    public static final String DB_LASTNAME = "Lastname";



    private final Pac4jAuthProvider authProvider = new Pac4jAuthProvider();
    private final Config pac4jConfig;

    public SecurityConfig(JsonObject config, DatabaseService database) {
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

    public enum AuthClient {
        FORM("form", FormClient.class, (key, secret) -> new FormClient()),
        FACEBOOK("facebook", FacebookClient.class, FacebookClient::new),
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
            return config.getJsonObject(OAUTH).getString(isServer(config) ? CALLBACK : LOCAL_CALLBACK);
        }

        public static String getClientNames() {
            return Arrays.stream(values())
                    .map(AuthClient::getClientName)
                    .collect(joining(","));
        }

        public Client create(JsonObject config) {
            JsonObject clientConfig = config.getJsonObject(OAUTH).getJsonObject(configKey, new JsonObject());
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
