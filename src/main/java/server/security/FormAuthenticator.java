package server.security;

import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.core.util.CommonHelper;

public class FormAuthenticator implements Authenticator<UsernamePasswordCredentials> {

    @Override
    public void validate(UsernamePasswordCredentials credentials, WebContext context) throws HttpAction {
        if (credentials == null) {
            throw new CredentialsException("No credentials provided.");
        }
        String email = credentials.getUsername();
        String password = credentials.getPassword();
        if (CommonHelper.isBlank(email)) {
            throw new CredentialsException("Username cannot be blank.");
        }
        if (CommonHelper.isBlank(password)) {
            throw new CredentialsException("Password cannot be blank.");
        }
        credentials.setUserProfile(new FormProfile(email, password));
    }
}
