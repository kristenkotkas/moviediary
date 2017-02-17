package server.security;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.pac4j.core.authorization.authorizer.ProfileAuthorizer;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.oauth.profile.facebook.FacebookProfile;
import org.pac4j.oauth.profile.google2.Google2Profile;
import server.entity.JsonObj;
import server.entity.SyncResult;
import server.service.DatabaseService;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static server.service.DatabaseService.getRows;

public class DatabaseAuthorizer extends ProfileAuthorizer<CommonProfile> {
    private static final String EMAIL = "Email";
    private static final String PASSWORD = "Password";
    private static final String SERIAL = "Serialnumber";

    private static final String FIRSTNAME = "Firstname";

    private final DatabaseService database;

    public DatabaseAuthorizer(DatabaseService database) {
        this.database = database;
    }

    @Override
    protected boolean isProfileAuthorized(WebContext context, CommonProfile profile) throws HttpAction {
        if (profile == null) {
            return false;
        }
        SyncResult<JsonObject> result = new SyncResult<>();
        result.executeAsync(() -> database.getAllUsers().setHandler(ar -> result.setReady(ar.result())));
        return ProfileAuthorizer.isAuthorized(profile, getRows(result.await().get(new JsonObj())));
    }

    @Override
    public boolean isAuthorized(WebContext context, List<CommonProfile> profiles) throws HttpAction {
        return isAnyAuthorized(context, profiles);
    }

    public enum ProfileAuthorizer {
        FACEBOOK(FacebookProfile.class, (profile, stream) -> stream
                .anyMatch(json -> {
                    System.out.println("TEST---------------------------------");
                    System.out.println(profile);
                    return profile.getEmail().equals(json.getString(EMAIL));
                })),
        GOOGLE(Google2Profile.class, (profile, stream) -> stream
                .anyMatch(json -> profile.getEmail().equals(json.getString(EMAIL)))),

        //todo change to serial number checking (hash)
        IDCARD(IdCardProfile.class, (IdCardProfile profile, Stream<JsonObject> stream) -> stream
                .anyMatch(json ->
                        profile.getFirstName().toLowerCase().equals(json.getString(FIRSTNAME).toLowerCase()))),

        FORM(FormProfile.class, (FormProfile profile, Stream<JsonObject> stream) -> stream
                .filter(json -> json.getString(EMAIL).equals(profile.getEmail()))
                .anyMatch(json -> profile.getPassword().equals(json.getString(PASSWORD))));

        private final Class type;
        private final BiFunction<CommonProfile, Stream<JsonObject>, Boolean> checker;

        <S extends CommonProfile> ProfileAuthorizer(Class type, BiFunction<S, Stream<JsonObject>, Boolean> checker) {
            this.type = type;
            this.checker = uncheckedCast(checker);
        }

        public static boolean isAuthorized(CommonProfile profile, JsonArray users) {
            for (ProfileAuthorizer authorizer : values()) {
                if (authorizer.type.isInstance(profile)) {
                    return authorizer.checker.apply(profile, users.stream().map(obj -> (JsonObject) obj));
                }
            }
            return false;
        }

        @SuppressWarnings("unchecked")
        private static <S extends CommonProfile> BiFunction<CommonProfile, Stream<JsonObject>, Boolean> uncheckedCast(
                BiFunction<S, Stream<JsonObject>, Boolean> authChecker) {
            return (BiFunction<CommonProfile, Stream<JsonObject>, Boolean>) authChecker;
        }
    }
}