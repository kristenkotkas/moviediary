package server.template.ui;

import eu.kyngas.template.engine.TypeSafeTemplate;

public interface BaseTemplate extends TypeSafeTemplate {
    void setLogoutLink(String logoutLink);
}