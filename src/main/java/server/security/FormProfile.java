package server.security;

import org.pac4j.core.profile.CommonProfile;

import static org.pac4j.core.context.Pac4jConstants.PASSWORD;

public class FormProfile extends CommonProfile {
    private static final String EMAIL = "email";

    public FormProfile(String email, String password) {
        setId(email);
        addAttribute(EMAIL, email);
        addAttribute(PASSWORD, password);
    }

    @Override
    public String getEmail() {
        return super.getEmail();
    }

    // TODO: 17.02.2017 return hash instead
    public String getPassword() {
        return (String) getAttribute(PASSWORD);
    }
}
