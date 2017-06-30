package server.security;

import org.pac4j.core.profile.CommonProfile;

import static server.security.SecurityConfig.*;

/**
 * Profile for ID Card client.
 */
public class IdCardProfile extends CommonProfile {
  // TODO: 19.02.2017 define attributes

  public IdCardProfile(IdCardCredentials credentials) {
    setId(credentials.getSerial());
    addAttribute(PAC4J_EMAIL, credentials.getSerial());
    addAttribute(PAC4J_SERIAL, credentials.getSerial());
    addAttribute(PAC4J_FIRSTNAME, credentials.getFirstName());
    addAttribute(PAC4J_LASTNAME, credentials.getLastName());
    addAttribute(PAC4J_ISSUER, credentials.getIssuer());
    addAttribute(PAC4J_COUNTRY, credentials.getCountry());
  }

  public String getSerial() {
    return (String) getAttribute(PAC4J_SERIAL);
  }

  public String getIssuer() {
    return (String) getAttribute(PAC4J_ISSUER);
  }
}
