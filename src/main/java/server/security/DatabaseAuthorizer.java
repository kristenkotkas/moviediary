package server.security;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.pac4j.core.authorization.authorizer.ProfileAuthorizer;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.oauth.profile.facebook.FacebookProfile;
import org.pac4j.oauth.profile.google2.Google2Profile;
import server.entity.JsonObj;
import server.entity.SyncResult;
import server.entity.TriFunction;
import server.service.DatabaseService;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.pac4j.core.exception.HttpAction.redirect;
import static org.pac4j.core.util.CommonHelper.addParameter;
import static server.router.AuthRouter.AUTH_LOGOUT;
import static server.router.DatabaseRouter.DISPLAY_MESSAGE;
import static server.router.UiRouter.UI_LOGIN;
import static server.service.DatabaseService.*;
import static server.util.StringUtils.genString;
import static server.util.StringUtils.hash;

/**
 * Authorizer that checks against database whether authenticated user is allowed to access resources.
 */
public class DatabaseAuthorizer extends ProfileAuthorizer<CommonProfile> {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseAuthorizer.class);
    private static final String UNAUTHORIZED = "AUTHORIZER_UNAUTHORIZED";
    public static final String URL = "url";

    private final DatabaseService database;

    public DatabaseAuthorizer(DatabaseService database) {
        this.database = database;
    }

    /**
     * Checks whether Pac4j user profile is authorized.
     */
    @Override
    protected boolean isProfileAuthorized(WebContext context, CommonProfile profile) throws HttpAction {
        if (profile == null) {
            return false;
        }
        SyncResult<JsonObject> result = new SyncResult<>();
        result.executeAsync(() -> database.getAllUsers().setHandler(ar -> result.setReady(ar.result())));
        // TODO: 12/03/2017 retryable
        return ProfileAuthorizer.isAuthorized(database, profile,
                getRows(result.await(5, TimeUnit.SECONDS).get(new JsonObj())));
    }

    @Override
    public boolean isAuthorized(WebContext context, List<CommonProfile> profiles) throws HttpAction {
        return isAnyAuthorized(context, profiles);
    }

    /**
     * On error user is deauthenticated and redirected to login screen with appropriate message.
     */
    @Override
    protected boolean handleError(WebContext context) throws HttpAction {
        throw redirect(UNAUTHORIZED, context,
                addParameter(AUTH_LOGOUT, URL, addParameter(UI_LOGIN, DISPLAY_MESSAGE, UNAUTHORIZED)));
    }

    /**
     * Defines authorization methods for different Pac4j profiles.
     */
    public enum ProfileAuthorizer {
        FACEBOOK(FacebookProfile.class, oAuth2Authorization()),
        GOOGLE(Google2Profile.class, oAuth2Authorization()),
        IDCARD(IdCardProfile.class, (IdCardProfile profile, Stream<JsonObject> stream, DatabaseService database) -> {
            boolean isAuthorized = stream.anyMatch(json -> profile.getSerial().equals(json.getString(DB_USERNAME)));
            if (isAuthorized) {
                return true;
            }
            SyncResult<Boolean> result = new SyncResult<>();
            result.executeAsync(() -> database.insertUser(profile.getSerial(),
                    genString(),
                    profile.getFirstName(),
                    profile.getFamilyName()).setHandler(ar -> result.setReady(ar.succeeded())));
            return result.await().get();
            // TODO: 11/03/2017 timeout + retryable
        }),

        FORM(FormProfile.class, (FormProfile profile, Stream<JsonObject> stream, DatabaseService database) -> stream
                .filter(json -> json.getString(DB_USERNAME).equals(profile.getEmail()))
                .filter(json -> json.getString(DB_VERIFIED).equals("1"))
                .anyMatch(json -> hash(profile.getPassword(), profile.getSalt()).equals(json.getString(DB_PASSWORD))));

        private final Class type;
        private final TriFunction<CommonProfile, Stream<JsonObject>, DatabaseService, Boolean> checker;

        <S extends CommonProfile> ProfileAuthorizer(Class type, TriFunction<S, Stream<JsonObject>, DatabaseService,
                Boolean> checker) {
            this.type = type;
            this.checker = uncheckedCast(checker);
        }

        /**
         * Checks profile against defined authorizers.
         */
        public static boolean isAuthorized(DatabaseService database, CommonProfile profile, JsonArray users) {
            for (ProfileAuthorizer authorizer : values()) {
                if (authorizer.type.isInstance(profile)) {
                    return authorizer.checker.apply(profile, users.stream().map(obj -> (JsonObject) obj), database);
                }
            }
            return false;
        }

        // TODO: 12/03/2017 ilusamalt
        @SuppressWarnings("unchecked")
        private static <S extends CommonProfile> TriFunction<CommonProfile, Stream<JsonObject>, DatabaseService,
                Boolean> uncheckedCast(TriFunction<S, Stream<JsonObject>, DatabaseService, Boolean> authChecker) {
            return (TriFunction<CommonProfile, Stream<JsonObject>, DatabaseService, Boolean>) authChecker;
        }

        /**
         * Authorization for Facebook and Google profiles.
         */
        private static TriFunction<CommonProfile, Stream<JsonObject>, DatabaseService, Boolean> oAuth2Authorization() {
            return (profile, stream, database) -> {
                boolean isAuthorized = stream.anyMatch(json -> json.getString(DB_USERNAME).equals(profile.getEmail()));
                if (isAuthorized) {
                    return true;
                }
                if (profile.getEmail() == null) {
                    // TODO: 2.03.2017 redirect user to login with message: "you need to allow access to email"
                    throw new TechnicalException("User email not found.");
                }
                SyncResult<Boolean> result = new SyncResult<>();
                result.executeAsync(() -> database.insertUser(
                        profile.getEmail(),
                        genString(),
                        profile.getFirstName(),
                        profile.getFamilyName()).setHandler(ar -> result.setReady(ar.succeeded())));
                return result.await().get();
                // TODO: 11/03/2017 timeout + retryable
            };
        }
    }
}