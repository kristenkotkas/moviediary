package server.template.ui;

public interface LoginTemplate extends BaseTemplate {
  LoginTemplate setFormUrl(String formUrl);

  LoginTemplate setFacebookUrl(String facebookUrl);

  LoginTemplate setGoogleUrl(String googleUrl);

  LoginTemplate setIdCardUrl(String idCardUrl);

  LoginTemplate setRecommenderUrl(String recommenderUrl);

  LoginTemplate setDescToGenreUrl(String descToGenreUrl);

  LoginTemplate setDisplayMessage(String displayMessage);
}
