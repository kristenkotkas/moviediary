package server.security;

import com.github.scribejava.core.model.OAuth2AccessToken;
import io.vertx.core.json.JsonObject;
import org.pac4j.oauth.config.OAuth20Configuration;
import org.pac4j.oauth.profile.definition.OAuth20ProfileDefinition;

import java.util.stream.Stream;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
public class GoogleProfileDefinition extends OAuth20ProfileDefinition<GoogleProfile> {
  public static final String ID = "sub";
  public static final String EMAIL = "email";
  public static final String GIVEN_NAME = "given_name";
  public static final String FAMILY_NAME = "family_name";

  public GoogleProfileDefinition() {
    super(x -> new GoogleProfile());
  }

  @Override
  public String getProfileUrl(OAuth2AccessToken accessToken, OAuth20Configuration configuration) {
    return "https://openidconnect.googleapis.com/v1/userinfo";
  }

  @Override
  public GoogleProfile extractUserProfile(String body) {
    GoogleProfile profile = newProfile();
    JsonObject json = new JsonObject(body);
    profile.setId(json.getString(ID));
    Stream.of(EMAIL, GIVEN_NAME, FAMILY_NAME).forEach(key -> convertAndAdd(profile, key, json.getString(key)));
    return profile;
  }
}
