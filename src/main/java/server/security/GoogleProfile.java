package server.security;

import org.pac4j.oauth.profile.OAuth20Profile;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
public class GoogleProfile extends OAuth20Profile {

  @Override
  public String getEmail() {
    return getAttribute(GoogleProfileDefinition.EMAIL, String.class);
  }

  @Override
  public String getFirstName() {
    return getAttribute(GoogleProfileDefinition.GIVEN_NAME, String.class);
  }

  @Override
  public String getFamilyName() {
    return getAttribute(GoogleProfileDefinition.FAMILY_NAME, String.class);
  }
}
