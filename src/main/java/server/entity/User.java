package server.entity;

import io.vertx.core.json.JsonObject;

/**
 * @author <a href="https://bitbucket.org/kristjanhk/">Kristjan Hendrik Küngas</a>
 */
public class User {
    private int id;
    private String firstname;
    private String lastname;
    private String username;
    private String runtimeType;
    private boolean verified;

    public User(JsonObject user) {
        this.id = user.getInteger("Id");
        this.firstname = user.getString("Firstname");
        this.lastname = user.getString("Lastname");
        this.username = user.getString("Username");
        this.runtimeType = user.getString("RuntimeType");
        this.verified = user.getString("Verified").equals("1");
    }

    public User(int id, String firstname, String lastname, String username, String runtimeType, boolean verified) {
        this.id = id;
        this.firstname = firstname;
        this.lastname = lastname;
        this.username = username;
        this.runtimeType = runtimeType;
        this.verified = verified;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRuntimeType() {
        return runtimeType;
    }

    public void setRuntimeType(String runtimeType) {
        this.runtimeType = runtimeType;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }
}
