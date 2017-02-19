package server.security;

import org.pac4j.core.profile.CommonProfile;

public class IdCardProfile extends CommonProfile {
    public static final String SERIAL = "Serialnumber";
    public static final String FIRSTNAME = "first_name";
    public static final String LASTNAME = "family_name";
    public static final String ISSUER = "Issuer";
    public static final String COUNTRY = "location";

    public IdCardProfile(IdCardCredentials credentials) {
        setId(credentials.getSerial());
        addAttribute(SERIAL, credentials.getSerial());
        addAttribute(FIRSTNAME, credentials.getFirstName());
        addAttribute(LASTNAME, credentials.getLastName());
        addAttribute(ISSUER, credentials.getIssuer());
        addAttribute(COUNTRY, credentials.getCountry());
    }

    public String getSerial() {
        return (String) getAttribute(SERIAL);
    }

    public String getIssuer() {
        return (String) getAttribute(ISSUER);
    }
}
