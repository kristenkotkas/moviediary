package server.security;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.sstore.SessionStore;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.pac4j.vertx.VertxWebContext;
import org.pac4j.vertx.context.session.VertxSessionStore;
import server.service.DatabaseService;

@Slf4j
public class VertxSessionStoreImpl extends VertxSessionStore {
    private static final Logger LOG = LoggerFactory.getLogger(VertxSessionStoreImpl.class);
    private DatabaseService databaseService;

    public VertxSessionStoreImpl(SessionStore sessionStore, DatabaseService databaseService) {
        super(sessionStore);
        this.databaseService = databaseService;
    }

    @Override
    public String getOrCreateSessionId(VertxWebContext context) {
        LOG.info("=== STARTED SESSION ===");
        LOG.info(ToStringBuilder.reflectionToString(context.getVertxUser()));
        try {
            context.getVertxUser().pac4jUserProfiles().values().forEach(
                    profile -> databaseService.insertLoginEvent(
                            profile.getEmail(), profile.getClientName(), context.getServerName())
            );
        } catch (Exception e) {
            LOG.error(e.getStackTrace());
        }
        return super.getOrCreateSessionId(context);
    }
}
