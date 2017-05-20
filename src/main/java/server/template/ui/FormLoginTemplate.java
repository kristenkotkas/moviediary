package server.template.ui;

public interface FormLoginTemplate extends BaseTemplate {
    FormLoginTemplate setCallbackUrl(String callbackUrl);

    FormLoginTemplate setRegisterUrl(String registerUrl);

    FormLoginTemplate setCsrfToken(String token);
}
