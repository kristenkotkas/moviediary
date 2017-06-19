package server.template.ui;

public interface IdCardLoginTemplate extends BaseTemplate {
  IdCardLoginTemplate setCallbackUrl(String callbackUrl);

  IdCardLoginTemplate setClientVerifiedHeader(String clientVerifiedHeader);

  IdCardLoginTemplate setClientCertificateHeader(String clientCertificateHeader);

  IdCardLoginTemplate setClientVerifiedState(String clientVerifiedState);

  IdCardLoginTemplate setClientCertificate(String clientCertificate);
}
