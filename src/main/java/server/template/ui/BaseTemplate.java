package server.template.ui;

import eu.kyngas.template.engine.TypeSafeTemplate;

public interface BaseTemplate extends TypeSafeTemplate {
    void setLogoutLink(String logoutLink);

    void setUser(String user);

    void setHome(String home);

    void setMovies(String movies);

    void setHistory(String history);

    void setStatistics(String statistics);

    void setWishlist(String wishlist);

    void setUserName(String name);
}