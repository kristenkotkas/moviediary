package server.security;

import com.github.scribejava.apis.GoogleApi20;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.logout.GoogleLogoutActionBuilder;
import org.pac4j.oauth.client.OAuth20Client;
import org.pac4j.oauth.exception.OAuthCredentialsException;
import org.pac4j.oauth.profile.google2.Google2Profile;
import org.pac4j.oauth.profile.google2.Google2ProfileDefinition;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
public class Google2Client extends OAuth20Client<Google2Profile> {
  private static final String FULL_SCOPE = "https://www.googleapis.com/auth/plus.login email";

  public Google2Client(final String key, final String secret) {
    setKey(key);
    setSecret(secret);
  }

  @Override
  protected void clientInit(WebContext context) {
    configuration.setApi(GoogleApi20.instance());
    configuration.setProfileDefinition(new Google2ProfileDefinition());
    configuration.setScope(FULL_SCOPE);
    configuration.setWithState(true);
    configuration.setHasBeenCancelledFactory(ctx -> "access_denied"
        .equals(ctx.getRequestParameter(OAuthCredentialsException.ERROR)));
    setConfiguration(configuration);
    defaultLogoutActionBuilder(new GoogleLogoutActionBuilder<>());
    super.clientInit(context);
  }
}
