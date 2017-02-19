package server.security;

import io.vertx.core.json.JsonObject;
import org.pac4j.core.client.Client;
import org.pac4j.core.config.Config;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.oauth.client.Google2Client;
import org.pac4j.vertx.auth.Pac4jAuthProvider;
import server.service.DatabaseService;

import java.util.Arrays;
import java.util.function.BiFunction;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static server.router.UiRouter.UI_FORM_LOGIN;
import static server.security.SecurityConfig.AuthClient.getCallback;
import static server.security.SecurityConfig.AuthClient.values;
import static server.util.NetworkUtils.isServer;

public class SecurityConfig {
    public static final String CLIENT_VERIFIED_STATE = "SSL_CLIENT_VERIFY";
    public static final String CLIENT_CERTIFICATE = "SSL_CLIENT_CERT";
    public static final String AUTHORIZER = "CommonAuthorizer";

    private final Pac4jAuthProvider authProvider = new Pac4jAuthProvider();
    private final Config pac4jConfig;

    public SecurityConfig(JsonObject config, DatabaseService database) {
        this.pac4jConfig = new Config(getCallback(config), Arrays.stream(values())
                .map(client -> client.create(config))
                .collect(toList()));
        this.pac4jConfig.addAuthorizer(AUTHORIZER, new DatabaseAuthorizer(database));
        // TODO: 17.02.2017 store in config or smth
        this.pac4jConfig.getClients().findClient(FormClient.class).setLoginUrl((isServer(config) ?
                "https://movies.kyngas.eu" : "http://localhost:8081") + UI_FORM_LOGIN);
    }

    public Config getPac4jConfig() {
        return pac4jConfig;
    }

    public Pac4jAuthProvider getAuthProvider() {
        return authProvider;
    }

    public enum AuthClient {
        FORM("form", FormClient.class, (key, secret) -> new FormClient("", new FormAuthenticator())),
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
