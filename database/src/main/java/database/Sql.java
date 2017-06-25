package database;

enum Sql {
  ADD_WISHLIST("INSERT IGNORE INTO Wishlist (Username, MovieId, Time) VALUES (?, ?, ?)"),
  ADD_EPISODE("INSERT IGNORE INTO Series (Username, SeriesId, EpisodeId, SeasonId, Time) VALUES (?, ?, ?, ?, ?)"),
  WISHLIST_CONTAINS("SELECT MovieId FROM Wishlist WHERE Username = ? AND MovieId = ?"),
  GET_WISHLIST("SELECT Title, Time, Year, Image, MovieId FROM Wishlist " +
      "JOIN Movies ON Wishlist.MovieId = Movies.Id " +
      "WHERE Username =  ? ORDER BY Time DESC"),
  GET_YEARS_DISTRIBUTION("SELECT Year, COUNT(*) AS 'Count' FROM Views " +
      "JOIN Movies ON Movies.Id = Views.MovieId " +
      "WHERE Username = ? AND Start >= ? AND Start <= ?"),
  GET_WEEKDAYS_DISTRIBUTION("SELECT ((DAYOFWEEK(Start) + 5) % 7) AS Day, COUNT(*) AS 'Count' FROM Views " +
      "WHERE Username = ? AND Start >= ? AND Start <= ?"),
  GET_TIME_DISTRIBUTION("SELECT HOUR(Start) AS Hour, COUNT(*) AS Count FROM Views " +
      "WHERE Username = ? AND Start >= ? AND Start <= ? "),
  GET_MONTH_YEAR_DISTRIBUTION("SELECT MONTH(Start) AS Month, YEAR(Start) AS Year, COUNT(MONTHNAME(Start)) AS Count " +
      "FROM Views " +
      "WHERE Username = ? AND Start >= ? AND Start <= ? "),
  GET_ALL_TIME_META("SELECT DATE(Min(Start)) AS Start, COUNT(*) AS Count, " +
      "SUM(TIMESTAMPDIFF(MINUTE, Start, End)) AS Runtime " +
      "FROM Views " +
      "WHERE Username = ?"),
  ADD_OAUTH2_USER("INSERT INTO Users (Username, Firstname, Lastname, Password, Salt) VALUES (?, ?, ?, ?, ?)"),
  GET_USERS("SELECT * FROM Users JOIN Settings ON Users.Username = Settings.Username"),
  GET_USER("SELECT * FROM Users JOIN Settings ON Users.Username = Settings.Username WHERE Users.Username = ?"),
  ADD_VIEW("INSERT INTO Views (Username, MovieId, Start, End, WasFirst, WasCinema, Comment) " +
      "VALUES (?, ?, ?, ?, ?, ?, ?)"),
  GET_VIEWS("SELECT Views.Id, MovieId, Title, Start, WasFirst, WasCinema, Image, Comment, " +
      "TIMESTAMPDIFF(MINUTE, Start, End) AS Runtime FROM Views " +
      "JOIN Movies ON Views.MovieId = Movies.Id " +
      "WHERE Username = ? AND Start >= ? AND Start <= ?"),
  GET_TOP_MOVIES_STATISTICS("SELECT MovieId, Title, COUNT(*) AS Count, Image FROM Views " +
      "JOIN Movies ON Movies.Id = Views.MovieId " +
      "WHERE Username = ? AND Start >= ? AND Start <= ?"),
  GET_SETTINGS("SELECT * FROM Settings WHERE Username = ?"),
  ADD_MOVIE("INSERT IGNORE INTO Movies VALUES (?, ?, ?, ?)"),
  ADD_SERIES("INSERT IGNORE INTO SeriesInfo VALUES (?, ?, ?)"),
  GET_USER_COUNT("SELECT COUNT(*) AS Count FROM Users"),
  GET_MOVIE_VIEWS("SELECT Id, Start, WasCinema FROM Views WHERE Username = ? AND MovieId = ? ORDER BY Start DESC"),
  GET_VIEWS_META("SELECT Count(*) AS Count, SUM(TIMESTAMPDIFF(MINUTE, Start, End)) AS Runtime FROM Views " +
      "JOIN Movies ON Views.MovieId = Movies.Id " +
      "WHERE Username = ? AND Start >= ? AND Start <= ?"),
  DELETE_VIEW("DELETE FROM Views WHERE Username = ? AND Id = ?"),
  DELETE_EPISODE("DELETE FROM Series WHERE Username = ? AND EpisodeId = ?"),
  GET_SEEN_EPISODES("SELECT EpisodeId FROM Series WHERE Username = ? AND SeriesId = ?"),
  GET_WATCHING_SERIES("SELECT Title, Image, SeriesId, COUNT(SeriesId) AS Count FROM Series " +
      "JOIN SeriesInfo ON Series.SeriesId = SeriesInfo.Id " +
      "WHERE Username = ? GROUP BY Title, Image, SeriesId ORDER BY Title"),
  DELETE_WISHLIST("DELETE FROM Wishlist WHERE Username = ? AND MovieId = ?"),
  GET_LAST_VIEWS("SELECT Title, Start, MovieId, WEEKDAY(Start) AS 'week_day', WasCinema FROM Views " +
      "JOIN Movies ON Movies.Id = Views.MovieId " +
      "WHERE Username = ? ORDER BY Start DESC LIMIT 5"),
  GET_HOME_WISHLIST("SELECT Title, Time, Year, MovieId FROM Wishlist " +
      "JOIN Movies ON Movies.Id = Wishlist.MovieId " +
      "WHERE Username = ? ORDER BY Time DESC LIMIT 5"),
  GET_TOP_MOVIES("SELECT MovieId, Title, COUNT(*) AS Count, Image FROM Views " +
      "JOIN Movies ON Movies.Id = Views.MovieId " +
      "WHERE Username = ? GROUP BY MovieId ORDER BY Count DESC LIMIT 5"),
  GET_TOTAL_MOVIE_COUNT("SELECT COUNT(*) AS 'total_movies', SUM(TIMESTAMPDIFF(MINUTE, Start, End)) AS Runtime " +
      "FROM Views " +
      "WHERE Username = ?"),
  GET_TOTAL_CINEMA_COUNT("SELECT COUNT(*) AS 'total_cinema' FROM Views WHERE Username = ? AND WasCinema = 1"),
  GET_NEW_MOVIE_COUNT("SELECT COUNT(WasFirst) AS 'new_movies' FROM Views WHERE Username = ? AND WasFirst = 1"),
  GET_TOTAL_RUNTIME("SELECT SUM(TIMESTAMPDIFF(MINUTE, Start, End)) AS Runtime FROM Views WHERE Username = ?"),
  GET_TOTAL_DISTINCT_MOVIES("SELECT COUNT(DISTINCT MovieId) AS 'unique_movies' FROM Views WHERE Username = ?"),
  ADD_SEASON("INSERT IGNORE INTO Series (Username, SeriesId, EpisodeId, SeasonId, Time) VALUES"),
  DELETE_SEASON("DELETE FROM Series WHERE Username = ? AND SeasonId = ?"),
  UPDATE_USER_VERIFICATION_STATUS("UPDATE Settings SET Verified = ? WHERE Username = ?"),
  ADD_USER_SETTINGS("INSERT INTO Settings (Username, Verified) VALUES (?, ?)"),
  ADD_FORM_USER("INSERT INTO Users (Firstname, Lastname, Username, Password, Salt) VALUES (?, ?, ?, ?, ?)");

  private final String command;

  Sql(String command) {
    this.command = command;
  }

  public String get() {
    return command;
  }
}
