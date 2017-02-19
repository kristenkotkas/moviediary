package server.template.ui;

public interface NavigationTemplate extends BaseTemplate {
    NavigationTemplate setUser(String user);
    NavigationTemplate setHome(String home);
    NavigationTemplate setMovies(String movies);
    NavigationTemplate setHistory(String history);
    NavigationTemplate setStatistics(String statistics);
    NavigationTemplate setWishlist(String wishlist);
    NavigationTemplate setUserName(String name);
}
