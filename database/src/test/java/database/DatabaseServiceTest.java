package database;

import entity.LocalDatabase;
import entity.MapBuilder;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.core.Vertx;
import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import rx.Single;
import util.JsonUtils;

import java.util.Map;

import static entity.LocalDatabase.initializeDatabase;
import static entity.LocalDatabase.toTestingConfig;
import static entity.LocalSql.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static util.FileUtils.getConfig;
import static util.StringUtils.hash;

@RunWith(VertxUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DatabaseServiceTest {
  private static Vertx vertx;
  private static database.rxjava.DatabaseService database;
  private static LocalDatabase localDatabase;

  @BeforeClass
  public static void setUp(TestContext ctx) throws Exception {
    vertx = Vertx.vertx();
    JsonObject config = toTestingConfig(getConfig());
    JDBCClient jdbcClient = JDBCClient.createNonShared(vertx.getDelegate(), config.getJsonObject("mysql"));
    initializeDatabase(vertx, config).rxSetHandler()
        .doOnSuccess(db -> localDatabase = db)
        .flatMap(db -> Single.just(DatabaseService.create(jdbcClient)))
        .doOnSuccess(db -> database = new database.rxjava.DatabaseService(db))
        .test()
        .awaitTerminalEvent(10, SECONDS)
        .assertCompleted();
  }

  @AfterClass
  public static void tearDown(TestContext ctx) throws Exception {
    Async async = ctx.async();
    localDatabase.close();
    vertx.rxClose().subscribe(v -> async.complete(), ctx::fail);
  }

  @Before
  public void setUp() throws Exception {
    localDatabase.resetCleanStateBlocking();
  }

  @Test
  public void testGetUser(TestContext ctx) throws Exception {
    isCorrectUser(database.rxGetUser("unittest@kyngas.eu")
        .doOnError(ctx::fail)
        .map(JsonUtils::getRows)
        .map(array -> array.getJsonObject(0))
        .toBlocking()
        .value());
  }

  @Test
  public void testGetAllUsers(TestContext ctx) throws Exception {
    JsonArray users = database.rxGetAllUsers()
        .doOnError(ctx::fail)
        .map(JsonUtils::getRows)
        .toBlocking()
        .value();
    assertThat(users.size(), is(2));
    isCorrectUser(users.getJsonObject(0));
  }

  @Test
  public void testGetUsersCount(TestContext ctx) throws Exception {
    Long count = database.rxGetUsersCount()
        .doOnError(ctx::fail)
        .toBlocking().value()
        .getLong("Count");
    assertThat(count, is(2L));
  }

  private void isCorrectUser(JsonObject user) {
    assertThat(user.getString("Firstname"), is("Form"));
    assertThat(user.getString("Lastname"), is("Tester"));
    assertThat(user.getString("Username"), is("unittest@kyngas.eu"));
    assertThat(user.getString("Password"),
        is("967a097e667b8ebcbab27a5327c504dbfefc3fac3ca9eb696e00de16b4005e60"));
    assertThat(user.getString("Salt"), is("1ffa4de675252a4d"));
    assertThat(user.getString("RuntimeType"), is("default"));
    assertThat(user.getString("Verified"), is("1"));
  }

  @Test
  public void testGetUserSettings(TestContext ctx) throws Exception {
    JsonObject settings = database.rxGetSettings("unittest@kyngas.eu")
        .doOnError(ctx::fail)
        .map(JsonUtils::getRows)
        .map(array -> array.getJsonObject(0))
        .toBlocking()
        .value();
    assertThat(settings.getString("Username"), is("unittest@kyngas.eu"));
    assertThat(settings.getString("RuntimeType"), is("default"));
    assertThat(settings.getString("Verified"), is("1"));
  }

  @Test
  public void testGetWishlist(TestContext ctx) throws Exception {
    localDatabase.updateOrInsertBlocking(INSERT_MOVIES_HOBBIT, null);
    localDatabase.updateOrInsertBlocking(INSERT_WISHLIST_HOBBIT, null);
    JsonArray wishlist = database.rxGetWishlist("unittest@kyngas.eu")
        .doOnError(ctx::fail)
        .map(JsonUtils::getRows)
        .toBlocking()
        .value();
    assertThat(wishlist.size(), is(1));
    JsonObject wish = wishlist.getJsonObject(0);
    assertThat(wish.getString("Title"), is("The Hobbit: An Unexpected Journey"));
    assertThat(wish.getInteger("Year"), is(2012));
    assertThat(wish.getString("Image"), is("/w29Guo6FX6fxzH86f8iAbEhQEFC.jpg"));
    assertThat(wish.getInteger("MovieId"), is(49051));
  }

  @Test
  public void testIsInWishlist(TestContext ctx) throws Exception {
    localDatabase.updateOrInsertBlocking(INSERT_WISHLIST_HOBBIT, null);
    JsonArray wishlist = database.rxIsInWishlist("unittest@kyngas.eu", 49051)
        .doOnError(ctx::fail)
        .map(JsonUtils::getRows)
        .toBlocking()
        .value();
    assertThat(wishlist.size(), is(1));
    JsonObject wish = wishlist.getJsonObject(0);
    assertThat(wish.getInteger("MovieId"), is(49051));
  }

  @Test
  public void testGetSeenEpisodes(TestContext ctx) throws Exception {
    localDatabase.updateOrInsertBlocking(INSERT_SERIES_INFO, null);
    localDatabase.updateOrInsertBlocking(INSERT_SERIES_EPISODE, null);
    JsonArray episodes = database.rxGetSeenEpisodes("unittest@kyngas.eu", 42009)
        .doOnError(ctx::fail)
        .map(JsonUtils::getRows)
        .toBlocking()
        .value();
    assertThat(episodes.size(), is(1));
    JsonObject episode = episodes.getJsonObject(0);
    assertThat(episode.getInteger("EpisodeId"), is(1188308));
  }

  @Test
  public void testInsertEpisodeView(TestContext ctx) throws Exception {
    JsonObject data = new JsonObject()
        .put("seriesId", 42009)
        .put("episodeId", 1188308)
        .put("seasonId", "52595fb3760ee346619586ed");
    database.rxInsertEpisodeView("unittest@kyngas.eu", data.encode())
        .doOnError(ctx::fail).toBlocking().value();
    JsonArray episodes = localDatabase.queryBlocking("SELECT * FROM Series WHERE Username = ?", new JsonArray()
        .add("unittest@kyngas.eu"));
    assertThat(episodes.size(), is(1));
    JsonObject episode = episodes.getJsonObject(0);
    assertThat(episode.getInteger("SeriesId"), is(data.getInteger("seriesId")));
    assertThat(episode.getInteger("EpisodeId"), is(data.getInteger("episodeId")));
    assertThat(episode.getString("SeasonId"), is(data.getString("seasonId")));
  }

  @Test
  public void testInsertWishlist(TestContext ctx) throws Exception {
    database.rxInsertWishlist("unittest@kyngas.eu", 49501)
        .doOnError(ctx::fail).toBlocking().value();
    JsonArray wishlist = localDatabase.queryBlocking("SELECT * FROM Wishlist WHERE Username = ?",
        new JsonArray().add("unittest@kyngas.eu"));
    assertThat(wishlist.size(), is(1));
    JsonObject wish = wishlist.getJsonObject(0);
    assertThat(wish.getInteger("MovieId"), is(49501));
  }

  @Test
  public void testInsertSeries(TestContext ctx) throws Exception {
    JsonObject data = new JsonObject()
        .put("Id", 42009)
        .put("Title", "Black Mirror")
        .put("Image", "/djUxgzSIdfS5vNP2EHIBDIz9I8A.jpg");
    database.rxInsertSeries(data.getInteger("Id"), data.getString("Title"), data.getString("Image"))
        .doOnError(ctx::fail)
        .toBlocking()
        .value();
    JsonArray series = localDatabase.queryBlocking("SELECT * FROM SeriesInfo", null);
    assertThat(series.size(), is(1));
    JsonObject show = series.getJsonObject(0);
    assertThat(data, Matchers.equalTo(show));
  }

  @Test
  public void testInsertMovie(TestContext ctx) throws Exception {
    JsonObject data = new JsonObject()
        .put("Id", 49051)
        .put("Title", "The Hobbit: An Unexpected Journey")
        .put("Year", 2012)
        .put("Image", "/w29Guo6FX6fxzH86f8iAbEhQEFC.jpg");
    database.rxInsertMovie(data.getInteger("Id"), data.getString("Title"), data.getInteger("Year"),
        data.getString("Image"))
        .doOnError(ctx::fail)
        .toBlocking()
        .value();
    JsonArray movies = localDatabase.queryBlocking("SELECT * FROM Movies", null);
    assertThat(movies.size(), is(1));
    JsonObject movie = movies.getJsonObject(0);
    assertThat(data, Matchers.equalTo(movie));
  }

  @Test
  public void testInsertMovieView(TestContext ctx) throws Exception {
    JsonObject data = new JsonObject()
        .put("movieId", "49051")
        .put("start", "24 March, 2017 22:01")
        .put("end", "24 March, 2017 23:31")
        .put("wasFirst", false)
        .put("wasCinema", true)
        .put("comment", "something random");
    database.rxInsertView("unittest@kyngas.eu", data.encode())
        .doOnError(ctx::fail)
        .toBlocking()
        .value();
    JsonArray views = localDatabase.queryBlocking("SELECT * FROM Views", null);
    assertThat(views.size(), is(1));
    JsonObject view = views.getJsonObject(0);
    assertThat(String.valueOf(view.getInteger("MovieId")), is(data.getString("movieId")));
    assertThat(view.getString("Start"), is("2017-03-24T20:01:00Z"));
    assertThat(view.getString("End"), is("2017-03-24T21:31:00Z"));
    assertThat(view.getInteger("WasFirst"), is(data.getBoolean("wasFirst") ? 1 : 0));
    assertThat(view.getInteger("WasCinema"), is(data.getBoolean("wasCinema") ? 1 : 0));
    assertThat(view.getString("Comment"), is(data.getString("comment")));
  }

  @Test
  public void testInsertFbGoogleIdUser() throws Exception {
    database.rxInsertOAuth2User("test@test.ee", "ultrateam3000", "ultra", "tester")
        .toBlocking()
        .value();
    JsonArray users = localDatabase.queryBlocking("SELECT * FROM Users", null);
    assertThat(users.size(), is(3));
    JsonObject user = users.getJsonObject(2);
    assertThat(user.getString("Firstname"), is("ultra"));
    assertThat(user.getString("Lastname"), is("tester"));
    assertThat(user.getString("Username"), is("test@test.ee"));
    assertThat(user.getString("Password"), is(hash("ultrateam3000", user.getString("Salt"))));
  }

  @Test
  public void testInsertUserAnyNullParamFails() throws Exception {
    assertThrewException(
        database.rxInsertFormUser(null, "b", "c", "d", null));
    assertThrewException(
        database.rxInsertFormUser("a", null, "c", "d", null));
    assertThrewException(
        database.rxInsertFormUser("a", "b", null, "d", null));
    assertThrewException(
        database.rxInsertFormUser("a", "b", "c", null, null));
  }

  private void assertThrewException(Single<JsonObject> single) {
    assertThat(single
        .map(json -> false)
        .onErrorReturn(err -> true)
        .toBlocking().value(), is(true));
  }

  @Test
  public void testGetViews(TestContext ctx) throws Exception {
    JsonObject data = new JsonObject()
        .put("start", "20 April, 2017")
        .put("end", "26 April, 2017")
        .put("is-first", false)
        .put("is-cinema", false)
        .put("page", 0);
    localDatabase.updateOrInsertBlocking(INSERT_MOVIES_HOBBIT, null);
    localDatabase.updateOrInsertBlocking(INSERT_VIEW_HOBBIT, null);
    JsonObject view = getSingleItem(ctx, database.rxGetViews("unittest@kyngas.eu", data.encode()));
    assertThat(view.getInteger("MovieId"), is(49051));
    assertThat(view.getString("Title"), is("The Hobbit: An Unexpected Journey"));
    assertThat(view.getString("Start"), is("2017-04-23T14:58:00Z"));
    assertThat(view.getInteger("WasFirst"), is(1));
    assertThat(view.getInteger("WasCinema"), is(0));
    assertThat(view.getString("Image"), is("/w29Guo6FX6fxzH86f8iAbEhQEFC.jpg"));
    assertThat(view.getString("Comment"), is("random"));
    assertThat(view.getInteger("Runtime"), is(106));
  }

  @Test
  public void testGetYearsDistribution(TestContext ctx) throws Exception {
    JsonObject data = new JsonObject()
        .put("start", "20 April, 2017")
        .put("end", "26 April, 2017")
        .put("is-first", false)
        .put("is-cinema", false);
    localDatabase.updateOrInsertBlocking(INSERT_MOVIES_HOBBIT, null);
    localDatabase.updateOrInsertBlocking(INSERT_VIEW_HOBBIT, null);
    JsonObject year = getSingleItem(ctx,
        database.rxGetYearsDistribution("unittest@kyngas.eu", data.encode()));
    assertThat(year.getInteger("Year"), is(2012));
    assertThat(year.getInteger("Count"), is(1));
  }

  @Test
  public void testGetWeekdaysDistribution(TestContext ctx) throws Exception {
    JsonObject data = new JsonObject()
        .put("start", "20 April, 2017")
        .put("end", "26 April, 2017")
        .put("is-first", false)
        .put("is-cinema", false);
    localDatabase.updateOrInsertBlocking(INSERT_MOVIES_HOBBIT, null);
    localDatabase.updateOrInsertBlocking(INSERT_VIEW_HOBBIT, null);
    JsonObject day = getSingleItem(ctx,
        database.rxGetWeekdaysDistribution("unittest@kyngas.eu", data.encode()));
    assertThat(day.getInteger("Day"), is(6));
    assertThat(day.getInteger("Count"), is(1));
  }

  @Test
  public void testGetTimeDistribution(TestContext ctx) throws Exception {
    JsonObject data = new JsonObject()
        .put("start", "20 April, 2017")
        .put("end", "26 April, 2017")
        .put("is-first", false)
        .put("is-cinema", false);
    localDatabase.updateOrInsertBlocking(INSERT_MOVIES_HOBBIT, null);
    localDatabase.updateOrInsertBlocking(INSERT_VIEW_HOBBIT, null);
    JsonObject hour = getSingleItem(ctx,
        database.rxGetTimeDistribution("unittest@kyngas.eu", data.encode()));
    assertThat(hour.getInteger("Hour"), is(17));
    assertThat(hour.getInteger("Count"), is(1));
  }

  @Test
  public void testGetMovieViews(TestContext ctx) throws Exception {
    localDatabase.updateOrInsertBlocking(INSERT_VIEW_HOBBIT, null);
    JsonObject view = getSingleItem(ctx, database.rxGetMovieViews("unittest@kyngas.eu", "49051"));
    assertThat(view.getString("Start"), is("2017-04-23T14:58:00Z"));
    assertThat(view.getInteger("WasCinema"), is(0));
  }

  @Test
  public void testGetTotalDistinctMoviesCount(TestContext ctx) throws Exception {
    localDatabase.updateOrInsertBlocking(INSERT_VIEW_HOBBIT, null);
    localDatabase.updateOrInsertBlocking(INSERT_VIEW_GHOST, null);
    JsonObject count = getSingleItem(ctx, database.rxGetTotalDistinctMoviesCount("unittest@kyngas.eu"));
    assertThat(count.getInteger("unique_movies"), is(2));
  }

  @Test
  public void testGetTotalRuntime(TestContext ctx) throws Exception {
    localDatabase.updateOrInsertBlocking(INSERT_VIEW_HOBBIT, null);
    localDatabase.updateOrInsertBlocking(INSERT_VIEW_GHOST, null);
    JsonObject runtime = getSingleItem(ctx, database.rxGetTotalRuntime("unittest@kyngas.eu"));
    assertThat(runtime.getInteger("Runtime"), is(212));
  }

  @Test
  public void testGetNewMovieCount(TestContext ctx) throws Exception {
    localDatabase.updateOrInsertBlocking(INSERT_VIEW_HOBBIT, null);
    localDatabase.updateOrInsertBlocking(INSERT_VIEW_GHOST, null);
    JsonObject newMovieCount = getSingleItem(ctx, database.rxGetNewMovieCount("unittest@kyngas.eu"));
    assertThat(newMovieCount.getInteger("new_movies"), is(1));
  }

  @Test
  public void testGetTotalMovieCount(TestContext ctx) throws Exception {
    localDatabase.updateOrInsertBlocking(INSERT_VIEW_HOBBIT, null);
    localDatabase.updateOrInsertBlocking(INSERT_VIEW_GHOST, null);
    JsonObject totalMovieCount = getSingleItem(ctx, database.rxGetTotalMovieCount("unittest@kyngas.eu"));
    assertThat(totalMovieCount.getInteger("total_movies"), is(2));
    assertThat(totalMovieCount.getInteger("Runtime"), is(212));
  }

  @Test
  public void testGetTopMoviesHome(TestContext ctx) throws Exception {
    Map<Integer, JsonObject> data = new MapBuilder<Integer, JsonObject>()
        .put(49051, new JsonObject().put("Title", "The Hobbit: An Unexpected Journey").put("Count", 1))
        .put(315837, new JsonObject().put("Title", "Ghost in the Shell").put("Count", 1))
        .build();
    localDatabase.updateOrInsertBlocking(INSERT_MOVIES_HOBBIT, null);
    localDatabase.updateOrInsertBlocking(INSERT_MOVIES_GHOST, null);
    localDatabase.updateOrInsertBlocking(INSERT_VIEW_HOBBIT, null);
    localDatabase.updateOrInsertBlocking(INSERT_VIEW_GHOST, null);
    JsonArray array = database.rxGetTopMoviesHome("unittest@kyngas.eu")
        .doOnError(ctx::fail)
        .map(JsonUtils::getRows)
        .toBlocking()
        .value();
    assertThat(array.size(), is(2));
    JsonObject movie1 = array.getJsonObject(0);
    JsonObject movie1data = data.get(movie1.getInteger("MovieId"));
    assertThat(movie1.getString("Title"), is(movie1data.getString("Title")));
    assertThat(movie1.getInteger("Count"), is(movie1data.getInteger("Count")));
    JsonObject movie2 = array.getJsonObject(1);
    JsonObject movie2data = data.get(movie2.getInteger("MovieId"));
    assertThat(movie2.getString("Title"), is(movie2data.getString("Title")));
    assertThat(movie2.getInteger("Count"), is(movie2data.getInteger("Count")));
  }

  private JsonObject getSingleItem(TestContext ctx, Single<JsonObject> single) {
    JsonArray array = single
        .doOnError(ctx::fail)
        .map(JsonUtils::getRows)
        .toBlocking().value();
    assertThat(array.size(), is(1));
    return array.getJsonObject(0);
  }

  @Test
  public void testGetLastWishlistHome(TestContext ctx) throws Exception {
    localDatabase.updateOrInsertBlocking(INSERT_MOVIES_HOBBIT, null);
    localDatabase.updateOrInsertBlocking(INSERT_WISHLIST_HOBBIT, null);
    JsonObject lastWish = getSingleItem(ctx, database.rxGetLastWishlistHome("unittest@kyngas.eu"));
    assertThat(lastWish.getString("Title"), is("The Hobbit: An Unexpected Journey"));
    assertThat(lastWish.getInteger("Year"), is(2012));
    assertThat(lastWish.getInteger("MovieId"), is(49051));
  }

  @Test
  public void testGetLastMoviesHome(TestContext ctx) throws Exception {
    Map<Integer, JsonObject> data = new MapBuilder<Integer, JsonObject>()
        .put(49051, new JsonObject()
            .put("Title", "The Hobbit: An Unexpected Journey")
            .put("week_day", 6)
            .put("Start", "2017-04-23T14:58:00Z"))
        .put(315837, new JsonObject()
            .put("Title", "Ghost in the Shell")
            .put("week_day", 4)
            .put("Start", "2017-03-17T15:58:00Z"))
        .build();
    localDatabase.updateOrInsertBlocking(INSERT_MOVIES_HOBBIT, null);
    localDatabase.updateOrInsertBlocking(INSERT_MOVIES_GHOST, null);
    localDatabase.updateOrInsertBlocking(INSERT_VIEW_HOBBIT, null);
    localDatabase.updateOrInsertBlocking(INSERT_VIEW_GHOST, null);
    JsonArray array = database.rxGetLastMoviesHome("unittest@kyngas.eu")
        .doOnError(ctx::fail)
        .map(JsonUtils::getRows)
        .toBlocking()
        .value();
    assertThat(array.size(), is(2));
    JsonObject movie1 = array.getJsonObject(0);
    JsonObject movie1data = data.get(movie1.getInteger("MovieId"));
    assertThat(movie1.getString("Title"), is(movie1data.getString("Title")));
    assertThat(movie1.getInteger("week_day"), is(movie1data.getInteger("week_day")));
    assertThat(movie1.getString("Start"), is(movie1data.getString("Start")));
    JsonObject movie2 = array.getJsonObject(1);
    JsonObject movie2data = data.get(movie2.getInteger("MovieId"));
    assertThat(movie2.getString("Title"), is(movie2data.getString("Title")));
    assertThat(movie2.getInteger("week_day"), is(movie2data.getInteger("week_day")));
    assertThat(movie2.getString("Start"), is(movie2data.getString("Start")));
  }

  @Test
  public void testGetWatchingSeries(TestContext ctx) throws Exception {
    localDatabase.updateOrInsertBlocking(INSERT_SERIES_INFO, null);
    localDatabase.updateOrInsertBlocking(INSERT_SERIES_EPISODE, null);
    JsonArray series = database.rxGetWatchingSeries("unittest@kyngas.eu")
        .doOnError(ctx::fail)
        .map(JsonUtils::getRows)
        .toBlocking()
        .value();
    assertThat(series.size(), is(1));
    JsonObject show = series.getJsonObject(0);
    assertThat(show.getString("Title"), is("Black Mirror"));
    assertThat(show.getString("Image"), is("/djUxgzSIdfS5vNP2EHIBDIz9I8A.jpg"));
    assertThat(show.getInteger("SeriesId"), is(42009));
    assertThat(show.getInteger("Count"), is(1));
  }

  @Test
  public void testRemoveFromWishlist(TestContext ctx) throws Exception {
    localDatabase.updateOrInsertBlocking(INSERT_WISHLIST_HOBBIT, null);
    JsonObject result = database.rxRemoveFromWishlist("unittest@kyngas.eu", "49051")
        .doOnError(ctx::fail)
        .toBlocking()
        .value();
    assertThat(result.getInteger("updated"), is(1));
    JsonArray wishlist = localDatabase.queryBlocking("SELECT * FROM Wishlist", null);
    assertThat(wishlist.size(), is(0));
  }

  @Test
  public void testRemoveView(TestContext ctx) throws Exception {
    int key = localDatabase.updateOrInsertBlocking(INSERT_VIEW_HOBBIT, null)
        .getJsonArray("keys")
        .getInteger(0);
    localDatabase.updateOrInsertBlocking(INSERT_VIEW_GHOST, null);
    JsonObject result = database.rxRemoveView("unittest@kyngas.eu", String.valueOf(key))
        .doOnError(ctx::fail)
        .toBlocking()
        .value();
    assertThat(result.getInteger("updated"), is(1));
    JsonArray views = localDatabase.queryBlocking("SELECT * FROM Views", null);
    assertThat(views.size(), is(1));
    assertThat(views.getJsonObject(0).getInteger("MovieId"), is(315837));
  }

  @Test
  public void testRemoveEpisode(TestContext ctx) throws Exception {
    localDatabase.updateOrInsertBlocking(INSERT_SERIES_EPISODE, null);
    JsonObject result = database.rxRemoveEpisode("unittest@kyngas.eu", "1188308")
        .doOnError(ctx::fail)
        .toBlocking()
        .value();
    assertThat(result.getInteger("updated"), is(1));
    JsonArray views = localDatabase.queryBlocking("SELECT * FROM Series", null);
    assertThat(views.size(), is(0));
  }

  @Test
  public void testGetAllTimeMeta(TestContext ctx) throws Exception {
    JsonObject data = new JsonObject().put("is-first", false).put("is-cinema", false);
    localDatabase.updateOrInsertBlocking(INSERT_VIEW_HOBBIT, null);
    localDatabase.updateOrInsertBlocking(INSERT_VIEW_GHOST, null);
    JsonObject meta = getSingleItem(ctx, database.rxGetAllTimeMeta("unittest@kyngas.eu", data.encode()));
    assertThat(meta.getInteger("Count"), is(2));
    assertThat(meta.getString("Start"), is("2017-03-17"));
  }

  @Test
  public void testGetViewsMeta(TestContext ctx) throws Exception {
    localDatabase.updateOrInsertBlocking(INSERT_MOVIES_HOBBIT, null);
    localDatabase.updateOrInsertBlocking(INSERT_MOVIES_GHOST, null);
    localDatabase.updateOrInsertBlocking(INSERT_VIEW_HOBBIT, null);
    localDatabase.updateOrInsertBlocking(INSERT_VIEW_GHOST, null);
    JsonObject data = new JsonObject()
        .put("start", "20 April, 2017")
        .put("end", "26 April, 2017")
        .put("is-first", false)
        .put("is-cinema", false);
    JsonObject meta = getSingleItem(ctx, database.rxGetViewsMeta("unittest@kyngas.eu", data.encode()));
    assertThat(meta.getInteger("Count"), is(1));
  }
}
