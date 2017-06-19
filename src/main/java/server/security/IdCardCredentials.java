package server.security;

import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.util.CommonHelper;

import static server.util.StringUtils.capitalizeName;

/**
 * Credentials for ID Card client.
 */
public class IdCardCredentials extends Credentials {
  private final String serial;
  private final String firstName;
  private final String lastName;
  private final String issuer;
  private final String country;

  // SERIALNUMBER=\d{11},
  // GIVENNAME=(firstName),
  // SURNAME=(lastName),
  // CN="(lastName),(firstName),\d{11}",
  // OU=authentication,
  // O=(issuer),
  // C=(country)

  public IdCardCredentials(String subjectDN) {
    String[] data = subjectDN.split(", ");
    serial = getItem(data, 0);
    firstName = capitalizeName(getItem(data, 1));
    lastName = capitalizeName(getItem(data, 2));
    issuer = getItem(data, 5);
    country = getItem(data, 6);
    setClientName(IdCardClient.class.getSimpleName());
  }

  private String getItem(String[] data, int location) {
    return data[location].split("=")[1];
  }

  public String getSerial() {
    return serial;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getIssuer() {
    return issuer;
  }

  public String getCountry() {
    return country;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final IdCardCredentials that = (IdCardCredentials) o;
    if (serial != null ? !serial.equals(that.serial) : that.serial != null) {
      return false;
    } else if (firstName != null ? !firstName.equals(that.firstName) : that.firstName != null) {
      return false;
    } else if (lastName != null ? !lastName.equals(that.lastName) : that.lastName != null) {
      return false;
    } else if (issuer != null ? !issuer.equals(that.serial) : that.issuer != null) {
      return false;
    } else if (country != null ? !country.equals(that.country) : that.country != null) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = serial != null ? serial.hashCode() : 0;
    result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
    result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
    result = 31 * result + (issuer != null ? issuer.hashCode() : 0);
    result = 31 * result + (country != null ? country.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return CommonHelper.toString(this.getClass(),
        "SerialNumber", serial,
        "Firstname", firstName,
        "Lastname", lastName,
        "Issuer", issuer,
        "Country", country,
        "clientName", getClientName());
  }
}