package server.security;

import org.pac4j.core.profile.CommonProfile;

public class IdCardProfile extends CommonProfile {
    private final IdCardCredentials credentials;

    public IdCardProfile(IdCardCredentials credentials) {
        this.credentials = credentials;
    }

    public String getSerial() {
        return credentials.getSerial();
    }

    public String getFirstName() {
        return credentials.getFirstName();
    }

    public String getLastName() {
        return credentials.getLastName();
    }

    public String getIssuer() {
        return credentials.getIssuer();
    }

    public String getCountry() {
        return credentials.getCountry();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final IdCardProfile that = (IdCardProfile) obj;
        return credentials.equals(that.credentials);
    }

    @Override
    public int hashCode() {
        return credentials != null ? credentials.hashCode() : 0;
    }
}
