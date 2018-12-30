package server.security;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.pac4j.core.authorization.authorizer.ProfileAuthorizer;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.oauth.profile.facebook.FacebookProfile;
import server.entity.TriFunction;
import server.service.DatabaseService;

import java.util.List;
import java.util.stream.Stream;

import static org.pac4j.core.exception.HttpAction.redirect;
import static org.pac4j.core.util.CommonHelper.addParameter;
import static server.router.AuthRouter.AUTH_LOGOUT;
import static server.router.DatabaseRouter.DISPLAY_MESSAGE;
import static server.router.UiRouter.UI_LOGIN;
import static server.service.DatabaseService.Column.*;
import static server.service.DatabaseService.getRows;
import static server.util.StringUtils.genString;
import static server.util.StringUtils.hash;

/**
 * Authorizer that checks against database whether authenticated user is allowed to access resources.
 */
public class DatabaseAuthorizer extends ProfileAuthorizer<CommonProfile> {
    public static final String UNAUTHORIZED = "AUTHORIZER_UNAUTHORIZED";
    public static final String URL = "url";

    private final DatabaseService database;

    public DatabaseAuthorizer(DatabaseService database) {
        this.database = database;
    }

    // TODO: 19.05.2017 enum is short but ugly -> rewrite
    // TODO: 19.05.2017 database stuff is sync -> use worker or async pac4j

    /**
     * Checks whether Pac4j user profile is authorized.
     */
    @Override
    protected boolean isProfileAuthorized(WebContext context, CommonProfile profile) {
        return profile != null && ProfileAuthorizer.isAuthorized(database, profile,
                getRows(database.getAllUsers().rxSetHandler().toBlocking().value()));
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
        GOOGLE(GoogleProfile.class, oAuth2Authorization()),
        IDCARD(IdCardProfile.class, (IdCardProfile p, Stream<JsonObject> stream, DatabaseService database) -> stream
                .anyMatch(json -> p.getSerial().equals(json.getString(USERNAME.getName()))) ||
                database.insertUser(p.getSerial(), genString(), p.getFirstName(), p.getFamilyName())
                        .rxSetHandler().toBlocking().value() != null),
        FORM(FormProfile.class, (FormProfile p, Stream<JsonObject> stream, DatabaseService database) -> stream
                .filter(json -> json.getString(USERNAME.getName()).equals(p.getEmail()))
                .filter(json -> json.getString(VERIFIED.getName()).equals("1"))
                .anyMatch(json -> hash(p.getPassword(), p.getSalt()).equals(json.getString(PASSWORD.getName()))));

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

        @SuppressWarnings("unchecked")
        private static <S extends CommonProfile> TriFunction<CommonProfile, Stream<JsonObject>, DatabaseService,
                Boolean> uncheckedCast(TriFunction<S, Stream<JsonObject>, DatabaseService, Boolean> authChecker) {
            return (TriFunction<CommonProfile, Stream<JsonObject>, DatabaseService, Boolean>) authChecker;
        }

        /**
         * Authorization for Facebook and Google profiles.
         */
        private static TriFunction<CommonProfile, Stream<JsonObject>, DatabaseService, Boolean> oAuth2Authorization() {
            return (p, stream, database) -> {
                boolean isAuthorized = stream.anyMatch(json -> json.getString(USERNAME.getName()).equals(p.getEmail()));
                if (isAuthorized) {
                    return true;
                }
                if (p.getEmail() == null) {
                    // TODO: 2.03.2017 redirect user to login with message: "you need to allow access to email"
                    throw new TechnicalException("User email not found.");
                }
                return database.insertUser(p.getEmail(), genString(), p.getFirstName(), p.getFamilyName())
                        .rxSetHandler().toBlocking().value() != null;
            };
        }
    }
}