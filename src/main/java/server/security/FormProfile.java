package server.security;

import io.vertx.core.json.JsonObject;
import org.pac4j.core.profile.CommonProfile;
import server.service.DatabaseService;

import static server.security.SecurityConfig.*;
import static server.service.DatabaseService.Column;
import static server.service.DatabaseService.getRows;

/**
 * Profile for form client.
 */
public class FormProfile extends CommonProfile {
    // TODO: 19.02.2017 define attributes

    public FormProfile(String email, String password, DatabaseService database) {
        setId(email);
        addAttribute(PAC4J_EMAIL, email);
        addAttribute(PAC4J_PASSWORD, password);
        getRows(database.getUser(email).rxSetHandler().toBlocking().value()).stream()
                .map(obj -> (JsonObject) obj)
                .filter(json -> email.equals(json.getString(Column.USERNAME.getName())))
                .findAny()
                .ifPresent(json -> {
                    addAttribute(PAC4J_FIRSTNAME, json.getString(Column.FIRSTNAME.getName()));
                    addAttribute(PAC4J_LASTNAME, json.getString(Column.LASTNAME.getName()));
                    addAttribute(PAC4J_SALT, json.getString(Column.SALT.getName()));
                });
    }

    public String getPassword() {
        return (String) getAttribute(PAC4J_PASSWORD);
    }

    public String getSalt() {
        return (String) getAttribute(PAC4J_SALT);
    }
}