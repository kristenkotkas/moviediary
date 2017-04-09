package server.entity;

/**
 * @author <a href="https://bitbucket.org/kristjanhk/">Kristjan Hendrik Küngas</a>
 */
public class User {
    public int id;
    public String firstname;
    public String lastname;
    public String username;
    public String runtimeType;
    public boolean verified;

    public User(int id, String firstname, String lastname, String username, String runtimeType, boolean verified) {
        this.id = id;
        this.firstname = firstname;
        this.lastname = lastname;
        this.username = username;
        this.runtimeType = runtimeType;
        this.verified = verified;
    }
}
