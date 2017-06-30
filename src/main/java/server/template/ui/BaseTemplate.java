package server.template.ui;

import eu.kyngas.template.engine.TypeSafeTemplate;

public interface BaseTemplate extends TypeSafeTemplate {
  void setLang(String lang);

  void setLogoutUrl(String logoutUrl);

  void setLoginPage(String loginPage);

  void setHomePage(String homePage);

  void setMoviesPage(String moviesPage);

  void setSeriesPage(String moviesPage);

  void setHistoryPage(String historyPage);

  void setStatisticsPage(String statisticsPage);

  void setDiscoverPage(String wishlistPage);

  void setUserName(String name);

  void setUserFirstName(String name);
}