package server.security;

import database.rxjava.DatabaseService;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.pac4j.core.authorization.authorizer.ProfileAuthorizer;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.oauth.profile.facebook.FacebookProfile;
import org.pac4j.oauth.profile.google2.Google2Profile;
import server.entity.TriFunction;

import java.util.List;
import java.util.stream.Stream;

import static org.pac4j.core.exception.HttpAction.redirect;
import static org.pac4j.core.util.CommonHelper.addParameter;
import static server.router.AuthRouter.AUTH_LOGOUT;
import static server.router.DatabaseRouter.DISPLAY_MESSAGE;
import static server.router.UiRouter.UI_LOGIN;
import static util.JsonUtils.getRows;
import static util.StringUtils.genString;
import static util.StringUtils.hash;

/**
 * Authorizer that checks against database whether authenticated user is allowed to access resources.
 */
public class DatabaseAuthorizer extends ProfileAuthorizer<CommonProfile> {
  public static final String UNAUTHORIZED = "AUTHORIZER_UNAUTHORIZED";
  public static final String URL = "url";

  private final DatabaseService database;

  public DatabaseAuthorizer(database.DatabaseService database) {
    this.database = new DatabaseService(database);
  }

  // TODO: 19.05.2017 enum is short but ugly -> rewrite
  // TODO: 19.05.2017 database stuff is sync -> use worker or async pac4j

  /**
   * Checks whether Pac4j user profile is authorized.
   */
  @Override
  protected boolean isProfileAuthorized(WebContext context, CommonProfile profile) throws HttpAction {
    return profile != null &&
        ProfileAuthorizer.isAuthorized(database, profile, getRows(database.rxGetAllUsers().toBlocking().value()));
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
    IDCARD(IdCardProfile.class, (IdCardProfile p, Stream<JsonObject> stream, DatabaseService database) -> stream
        .anyMatch(json -> p.getSerial().equals(json.getString("Username"))) ||
        database.rxInsertOAuth2User(p.getSerial(), genString(), p.getFirstName(), p.getFamilyName())
            .toBlocking().value() != null),
    FORM(FormProfile.class, (FormProfile p, Stream<JsonObject> stream, DatabaseService database) -> stream
        .filter(json -> json.getString("Username").equals(p.getEmail()))
        .filter(json -> json.getString("Verified").equals("1"))
        .anyMatch(json -> hash(p.getPassword(), p.getSalt()).equals(json.getString("Password"))));

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
        boolean isAuthorized = stream.anyMatch(json -> json.getString("Username").equals(p.getEmail()));
        if (isAuthorized) {
          return true;
        }
        if (p.getEmail() == null) {
          // TODO: 2.03.2017 redirect user to login with message: "you need to allow access to email"
          throw new TechnicalException("User email not found.");
        }
        return database.rxInsertOAuth2User(p.getEmail(), genString(), p.getFirstName(), p.getFamilyName())
            .toBlocking().value() != null;
      };
    }
  }
}