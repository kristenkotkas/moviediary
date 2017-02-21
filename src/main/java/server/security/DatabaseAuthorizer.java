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
import server.entity.TriFunction;
import server.service.DatabaseService;

import java.util.List;
import java.util.stream.Stream;

import static server.service.DatabaseService.*;

public class DatabaseAuthorizer extends ProfileAuthorizer<CommonProfile> {
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
        return ProfileAuthorizer.isAuthorized(database, profile, getRows(result.await().get(new JsonObj())));
    }

    @Override
    public boolean isAuthorized(WebContext context, List<CommonProfile> profiles) throws HttpAction {
        return isAnyAuthorized(context, profiles);
    }

    public enum ProfileAuthorizer {
        FACEBOOK(FacebookProfile.class, oAuth2Authorization()),
        GOOGLE(Google2Profile.class, oAuth2Authorization()),
        IDCARD(IdCardProfile.class, (IdCardProfile profile, Stream<JsonObject> stream, DatabaseService database) -> {
            System.out.println("-------------Authorizing ID Card user------------------");
            boolean isAuthorized = stream.anyMatch(json -> profile.getSerial().equals(json.getString(DB_USERNAME)));
            System.out.println("Is authorized: " + isAuthorized);
            if (isAuthorized) {
                return true;
            }
            System.out.println("-------------Registering ID Card user------------------");
            SyncResult<Boolean> result = new SyncResult<>();
            result.executeAsync(() -> database.insertUser(profile.getSerial(),
                    "",
                    profile.getFirstName(),
                    profile.getFamilyName()).setHandler(ar -> result.setReady(ar.succeeded())));
            return result.await().get();
        }),

        //todo password hashing checking
        FORM(FormProfile.class, (FormProfile profile, Stream<JsonObject> stream, DatabaseService database) -> stream
                .filter(json -> json.getString(DB_USERNAME).equals(profile.getEmail()))
                .anyMatch(json -> profile.getPassword().equals(json.getString(DB_PASSWORD))));

        private final Class type;
        private final TriFunction<CommonProfile, Stream<JsonObject>, DatabaseService, Boolean> checker;

        <S extends CommonProfile> ProfileAuthorizer(Class type, TriFunction<S, Stream<JsonObject>, DatabaseService,
                Boolean> checker) {
            this.type = type;
            this.checker = uncheckedCast(checker);
        }

        public static boolean isAuthorized(DatabaseService database, CommonProfile profile, JsonArray users) {
            // TODO: 19.02.2017 profiles as enummap or smth -> get enum based on clientname from profile
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

        private static TriFunction<CommonProfile, Stream<JsonObject>, DatabaseService, Boolean> oAuth2Authorization() {
            return (profile, stream, database) -> {
                System.out.println("------------Authorizing Facebook/Google user--------------");
                System.out.println(profile);
                boolean isAuthorized = stream.anyMatch(json -> profile.getEmail().equals(json.getString(DB_USERNAME)));
                if (isAuthorized) {
                    return true;
                }
                System.out.println("------------Registering Facebook/Google user-------------");
                SyncResult<Boolean> result = new SyncResult<>();
                result.executeAsync(() -> database.insertUser(
                        profile.getEmail(),
                        "",
                        profile.getFirstName(),
                        profile.getFamilyName()).setHandler(ar -> result.setReady(ar.succeeded())));
                return result.await().get();
            };
        }
    }
}