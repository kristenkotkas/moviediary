package server.template.ui;

public interface LoginTemplate extends BaseTemplate {
    LoginTemplate setFormUrl(String formUrl);

    LoginTemplate setFacebook(String facebook);

    LoginTemplate setGoogle(String google);

    LoginTemplate setIdCard(String idCard);
}
