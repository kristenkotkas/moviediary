package server.security;

import io.vertx.core.json.JsonObject;
import org.pac4j.core.profile.CommonProfile;
import server.entity.SyncResult;
import server.service.DatabaseService;

import static server.security.SecurityConfig.*;
import static server.service.DatabaseService.*;

public class FormProfile extends CommonProfile {
    // TODO: 19.02.2017 define attributes

    public FormProfile(String email, String password, DatabaseService database) {
        setId(email);
        addAttribute(PAC4J_EMAIL, email);
        addAttribute(PAC4J_PASSWORD, password);
        SyncResult<JsonObject> result = new SyncResult<>();
        result.executeAsync(() -> database.getUser(email).setHandler(ar -> result.setReady(ar.result())));
        getRows(result.await().get()).stream()
                .map(obj -> (JsonObject) obj)
                .filter(json -> email.equals(json.getString(DB_USERNAME)))
                .findAny()
                .ifPresent(json -> {
                    addAttribute(PAC4J_FIRSTNAME, json.getString(DB_FIRSTNAME));
                    addAttribute(PAC4J_LASTNAME, json.getString(DB_LASTNAME));
                });
    }

    // TODO: 17.02.2017 return hash instead
    public String getPassword() {
        return (String) getAttribute(PAC4J_PASSWORD);
    }
}
