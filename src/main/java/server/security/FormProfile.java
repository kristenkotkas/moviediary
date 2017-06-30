package server.security;

import database.rxjava.DatabaseService;
import entity.JsonObj;
import org.pac4j.core.profile.CommonProfile;

import static server.security.SecurityConfig.*;
import static util.JsonUtils.getRows;

/**
 * Profile for form client.
 */
public class FormProfile extends CommonProfile {
  // TODO: 19.02.2017 define attributes

  public FormProfile(String email, String password, DatabaseService database) {
    setId(email);
    addAttribute(PAC4J_EMAIL, email);
    addAttribute(PAC4J_PASSWORD, password);
    getRows(database.rxGetUser(email).toBlocking().value()).stream()
        .map(JsonObj::fromParent)
        .filter(json -> email.equals(json.getString("Username")))
        .findAny()
        .ifPresent(json -> {
          addAttribute(PAC4J_FIRSTNAME, json.getString("Firstname"));
          addAttribute(PAC4J_LASTNAME, json.getString("Lastname"));
          addAttribute(PAC4J_SALT, json.getString("Salt"));
        });
  }

  public String getPassword() {
    return (String) getAttribute(PAC4J_PASSWORD);
  }

  public String getSalt() {
    return (String) getAttribute(PAC4J_SALT);
  }
}