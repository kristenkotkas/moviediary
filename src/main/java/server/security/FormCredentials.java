package server.security;

import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.util.CommonHelper;

public class FormCredentials extends Credentials {
    private final String email;
    private final String password;

    public FormCredentials(String email, String password) {
        this.email = email;
        this.password = password;
        setClientName(FormClient.class.getSimpleName());
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final FormCredentials that = (FormCredentials) o;
        if (email != null ? !email.equals(that.email) : that.email != null) {
            return false;
        } else if (password != null ? !password.equals(that.password) : that.password != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = email != null ? email.hashCode() : 0;
        result = 31 * result + (password != null ? password.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return CommonHelper.toString(this.getClass(),
                "Email", email,
                "Password", "[hidden]",
                "clientName", getClientName());
    }
}