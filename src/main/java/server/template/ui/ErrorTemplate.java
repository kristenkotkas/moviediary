package server.template.ui;

public interface ErrorTemplate extends BaseTemplate {
    ErrorTemplate setPosterFileName(String fileName);

    ErrorTemplate setErrorMessage(String message);

    ErrorTemplate setErrorCode(int statusCode);
}
