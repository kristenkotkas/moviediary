package server.template.ui;

public interface NotFoundTemplate extends BaseTemplate {
    NotFoundTemplate setNotFoundName(String fileName);

    NotFoundTemplate setErrorMessage(String message);

    NotFoundTemplate setErrorCode(String errorCode);
}
