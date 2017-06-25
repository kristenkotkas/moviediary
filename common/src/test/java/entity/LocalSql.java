package entity;

import static java.lang.System.currentTimeMillis;

public enum LocalSql {
  CREATE_USERS_TABLE("CREATE TABLE Users (" +
      "    Id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,\n" +
      "    Firstname VARCHAR(100) NOT NULL,\n" +
      "    Lastname VARCHAR(100) NOT NULL,\n" +
      "    Username VARCHAR(100) NOT NULL,\n" +
      "    Password VARCHAR(64) DEFAULT 'NULL',\n" +
      "    Salt VARCHAR(16) DEFAULT 'default' NOT NULL)"),
  CREATE_SETTINGS_TABLE("CREATE TABLE Settings (\n" +
      "  Id          INT NOT NULL AUTO_INCREMENT PRIMARY KEY,\n" +
      "  RuntimeType VARCHAR(100) DEFAULT 'default' NOT NULL,\n" +
      "  Username    VARCHAR(100)                   NOT NULL,\n" +
      "  Verified    VARCHAR(64) DEFAULT '0'        NOT NULL);"),
  CREATE_MOVIES_TABLE("CREATE TABLE Movies (\n" +
      "  Id    INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,\n" +
      "  Title VARCHAR(100) NOT NULL,\n" +
      "  Year  SMALLINT     NOT NULL,\n" +
      "  Image VARCHAR(64)  NOT NULL\n" +
      ");"),
  CREATE_SERIES_TABLE("CREATE TABLE Series (\n" +
      "  Username  VARCHAR(100) NOT NULL,\n" +
      "  SeriesId  INT          NOT NULL,\n" +
      "  EpisodeId INT          NOT NULL,\n" +
      "  SeasonId  VARCHAR(100) NOT NULL,\n" +
      "  Time      BIGINT       NOT NULL,\n" +
      "  PRIMARY KEY (Username, EpisodeId)\n" +
      ");"),
  CREATE_SERIES_INFO_TABLE("CREATE TABLE SeriesInfo (\n" +
      "  Id    INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,\n" +
      "  Title VARCHAR(100) NOT NULL,\n" +
      "  Image VARCHAR(100) NOT NULL\n" +
      ");"),
  CREATE_VIEWS_TABLE("CREATE TABLE Views (\n" +
      "  Id        INT NOT NULL AUTO_INCREMENT PRIMARY KEY,\n" +
      "  MovieId   INT                     NOT NULL,\n" +
      "  Start     DATETIME                NULL,\n" +
      "  End       DATETIME                NULL,\n" +
      "  WasFirst  TINYINT                 NOT NULL,\n" +
      "  WasCinema TINYINT                 NOT NULL,\n" +
      "  Username  VARCHAR(100)            NOT NULL,\n" +
      "  Comment   VARCHAR(500) DEFAULT '' NOT NULL\n);"),
  CREATE_WISHLIST_TABLE("CREATE TABLE Wishlist (" +
      "  MovieId  INT                     NOT NULL,\n" +
      "  Username VARCHAR(100)            NOT NULL,\n" +
      "  Time     BIGINT NOT NULL,\n" +
      "  PRIMARY KEY (Username, MovieId));"),
  INSERT_FORM_USER("INSERT INTO Users " +
      "(Username, Firstname, Lastname, Password, Salt) " +
      "VALUES ('unittest@kyngas.eu', 'Form', 'Tester', " +
      "'967a097e667b8ebcbab27a5327c504dbfefc3fac3ca9eb696e00de16b4005e60', '1ffa4de675252a4d')"),
  INSERT_FORM_SETTING("INSERT INTO Settings (Username, RuntimeType, Verified) " +
      "VALUES ('unittest@kyngas.eu', 'default', '1')"),
  INSERT_FB_USER("INSERT INTO Users " +
      "(Username, Firstname, Lastname, Password, Salt) " +
      "VALUES ('facebook_ekubamn_tester@tfbnw.net', 'Facebook', 'Tester', " +
      "'1f27217a682d9631b4a839c72ef9bdc4dd5aed7319cc5b0df016ea8ddb81aa1f', '67d2ba0a146054b3')"),
  INSERT_FB_SETTING("INSERT INTO Settings (Username, RuntimeType, Verified) " +
      "VALUES ('facebook_ekubamn_tester@tfbnw.net', 'default', '0')"),
  INSERT_MOVIES_HOBBIT("INSERT INTO Movies (Id, Title, Year, Image) " +
      "VALUES ('49051', 'The Hobbit: An Unexpected Journey', '2012', '/w29Guo6FX6fxzH86f8iAbEhQEFC.jpg')"),
  INSERT_MOVIES_GHOST("INSERT INTO Movies (Id, Title, Year, Image) " +
      "VALUES ('315837', 'Ghost in the Shell', '2017', '/si1ZyELNHdPUZw4pXR5KjMIIsBF.jpg')"),
  INSERT_SERIES_EPISODE("INSERT INTO Series " +
      "(Username, SeriesId, EpisodeId, SeasonId, Time) VALUES " +
      "('unittest@kyngas.eu', '42009', '1188308', '571bb2e29251414e97005342', '" + currentTimeMillis() + "')"),
  INSERT_SERIES_INFO("INSERT INTO SeriesInfo " +
      "(Id, Title, Image) VALUES " +
      "('42009', 'Black Mirror', '/djUxgzSIdfS5vNP2EHIBDIz9I8A.jpg')"),
  INSERT_WISHLIST_HOBBIT("INSERT INTO Wishlist (Username, MovieId, Time) " +
      "VALUES ('unittest@kyngas.eu', '49051', '" + currentTimeMillis() + "')"),
  INSERT_VIEW_HOBBIT("INSERT INTO Views " +
      "(Username, MovieId, Start, End, WasFirst, WasCinema, Comment) " +
      "VALUES ('unittest@kyngas.eu', '49051', '2017-04-23 17:58:00', '2017-04-23 19:44:00', '1', '0', 'random')"),
  INSERT_VIEW_GHOST("INSERT INTO Views " +
      "(Username, MovieId, Start, End, WasFirst, WasCinema, Comment) " +
      "VALUES ('unittest@kyngas.eu', '315837', '2017-03-17 17:58:00', '2017-03-17 19:44:00', '0', '1', 'lamp')");

  private final String command;

  LocalSql(String command) {
    this.command = command;
  }

  public String get() {
    return command;
  }
}
