package server.security;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.pac4j.core.client.IndirectClientV2;
import org.pac4j.core.client.RedirectAction;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.util.CommonHelper;
import server.router.UiRouter;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import static java.nio.charset.StandardCharsets.UTF_8;
import static server.router.UiRouter.UI_IDCARDLOGIN;
import static server.security.SecurityConfig.CLIENT_CERTIFICATE;
import static server.security.SecurityConfig.CLIENT_VERIFIED_STATE;

/**
 * Indirect client for ID Card login.
 */
public class IdCardClient extends IndirectClientV2<IdCardCredentials, IdCardProfile> {
    private static final Logger log = LoggerFactory.getLogger(UiRouter.class);
    private static final String URL = "https://id.movies.kyngas.eu" + UI_IDCARDLOGIN;
    private static final String CERT_TYPE = "X.509";
    private static final String VERIFIED = "SUCCESS";

    private String loginUrl = null;

    public IdCardClient() {
        setAuthenticator((credentials, context) -> credentials.setUserProfile(new IdCardProfile(credentials)));
    }

    @Override
    protected void internalInit(WebContext context) {
        super.internalInit(context);
        loginUrl = callbackUrlResolver.compute(URL, context);
        setRedirectActionBuilder(webContext -> RedirectAction.redirect(loginUrl));
        setCredentialsExtractor(webContext -> {
            String verify = webContext.getRequestParameter(CLIENT_VERIFIED_STATE);
            String cert = webContext.getRequestParameter(CLIENT_CERTIFICATE);
            if (verify == null || !VERIFIED.equals(verify)) {
                log.error("Client certificate is invalid: " + verify + "; " + cert);
                return null;
            }
            if (cert == null) {
                log.error("Client certificate is missing, verification state: " + verify);
                return null;
            }
            try {
                return new IdCardCredentials(((X509Certificate) CertificateFactory.getInstance(CERT_TYPE)
                        .generateCertificate(new ByteArrayInputStream(fixFormat(cert).getBytes(UTF_8))))
                        .getSubjectDN()
                        .getName());
            } catch (CertificateException e) {
                log.error("Client certificate is invalid: " + cert, e);
            }
            return null;
        });
    }

    private String fixFormat(String certificate) {
        return certificate.replace("-----BEGIN CERTIFICATE----- ", "-----BEGIN CERTIFICATE-----\r\n");
    }

    @Override
    public String toString() {
        return CommonHelper.toString(getClass(),
                "callbackUrl", callbackUrl,
                "name", getName(),
                "loginUrl", loginUrl,
                "redirectActionBuilder", getRedirectActionBuilder(),
                "extractor", getCredentialsExtractor(),
                "authenticator", getAuthenticator(),
                "profileCreator", getProfileCreator());
    }
}
