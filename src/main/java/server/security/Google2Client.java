package server.security;

import com.github.scribejava.apis.GoogleApi20;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.logout.GoogleLogoutActionBuilder;
import org.pac4j.oauth.client.OAuth20Client;
import org.pac4j.oauth.exception.OAuthCredentialsException;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
public class Google2Client extends OAuth20Client<GoogleProfile> {
  private static final String FULL_SCOPE = "profile email";

  public Google2Client(final String key, final String secret) {
    setKey(key);
    setSecret(secret);
  }

  @Override
  protected void clientInit(WebContext context) {
    configuration.setApi(GoogleApi20.instance());
    configuration.setProfileDefinition(new GoogleProfileDefinition());
    configuration.setScope(FULL_SCOPE);
    configuration.setWithState(true);
    configuration.setHasBeenCancelledFactory(ctx -> "access_denied"
        .equals(ctx.getRequestParameter(OAuthCredentialsException.ERROR)));
    setConfiguration(configuration);
    defaultLogoutActionBuilder(new GoogleLogoutActionBuilder<>());
    super.clientInit(context);
  }
}
