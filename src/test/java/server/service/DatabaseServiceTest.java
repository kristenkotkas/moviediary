package server.service;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import rx.Observable;
import server.util.CommonUtils;
import server.util.LocalDatabase;
import server.verticle.ServerVerticle;

import java.util.Map;

import static io.vertx.rxjava.core.RxHelper.deployVerticle;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static server.service.DatabaseService.Column.*;
import static server.util.FileUtils.getConfig;
import static server.util.LocalDatabase.*;
import static server.util.NetworkUtils.HTTP_PORT;
import static server.util.StringUtils.hash;

@RunWith(VertxUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DatabaseServiceTest {
    private static final int PORT = 8082;

    private static Vertx vertx;
    private static JsonObject config;
    private static DatabaseService database;
    private static LocalDatabase localDatabase;

    @BeforeClass
    public static void setUp(TestContext ctx) throws Exception {
        vertx = Vertx.vertx();
        config = getConfig().put(HTTP_PORT, PORT);
        initializeDatabase(vertx, config.getJsonObject("mysql")).rxSetHandler()
                .doOnSuccess(db -> localDatabase = db)
                .doOnError(ctx::fail)
                .flatMap(db -> Observable.just(DatabaseService.create(vertx, config)).toSingle())
                .doOnSuccess(db -> database = db)
                .doOnError(ctx::fail)
                .toCompletable()
                .andThen(deployVerticle(vertx, new ServerVerticle()
                        .setDatabase(database), new DeploymentOptions().setConfig(config)))
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
        isCorrectUser(database.getUser("unittest@kyngas.eu").rxSetHandler()
                .doOnError(ctx::fail)
                .map(DatabaseService::getRows)
                .map(array -> array.getJsonObject(0))
                .toBlocking()
                .value());
    }

    @Test
    public void testGetAllUsers(TestContext ctx) throws Exception {
        JsonArray users = database.getAllUsers().rxSetHandler()
                .doOnError(ctx::fail)
                .map(DatabaseService::getRows)
                .toBlocking()
                .value();
        System.out.println(users.encodePrettily());
        assertThat(users.size(), is(2));
        isCorrectUser(users.getJsonObject(0));
    }

    @Test
    public void testGetUsersCount(TestContext ctx) throws Exception {
        String count = database.getUsersCount().rxSetHandler().doOnError(ctx::fail).toBlocking().value();
        assertThat(count, is("2"));
    }

    private void isCorrectUser(JsonObject user) {
        assertThat(user.getString(FIRSTNAME.getName()), is("Form"));
        assertThat(user.getString(LASTNAME.getName()), is("Tester"));
        assertThat(user.getString(USERNAME.getName()), is("unittest@kyngas.eu"));
        assertThat(user.getString(PASSWORD.getName()),
                is("967a097e667b8ebcbab27a5327c504dbfefc3fac3ca9eb696e00de16b4005e60"));
        assertThat(user.getString(SALT.getName()), is("1ffa4de675252a4d"));
        assertThat(user.getString(RUNTIMETYPE.getName()), is("default"));
        assertThat(user.getString(VERIFIED.getName()), is("1"));
    }

    @Test
    public void testGetUserSettings(TestContext ctx) throws Exception {
        JsonObject settings = database.getSettings("unittest@kyngas.eu").rxSetHandler()
                .doOnError(ctx::fail)
                .map(DatabaseService::getRows)
                .map(array -> array.getJsonObject(0))
                .toBlocking()
                .value();
        assertThat(settings.getString(USERNAME.getName()), is("unittest@kyngas.eu"));
        assertThat(settings.getString(RUNTIMETYPE.getName()), is("default"));
        assertThat(settings.getString(VERIFIED.getName()), is("1"));
    }

    @Test
    public void testGetWishlist(TestContext ctx) throws Exception {
        localDatabase.updateOrInsertBlocking(SQL_INSERT_MOVIES_HOBBIT, null);
        localDatabase.updateOrInsertBlocking(SQL_INSERT_WISHLIST, null);
        JsonArray wishlist = database.getWishlist("unittest@kyngas.eu").rxSetHandler()
                .doOnError(ctx::fail)
                .map(DatabaseService::getRows)
                .toBlocking()
                .value();
        assertThat(wishlist.size(), is(1));
        JsonObject wish = wishlist.getJsonObject(0);
        assertThat(wish.getString(TITLE.getName()), is("The Hobbit: An Unexpected Journey"));
        assertThat(wish.getInteger(YEAR.getName()), is(2012));
        assertThat(wish.getString(IMAGE.getName()), is("/w29Guo6FX6fxzH86f8iAbEhQEFC.jpg"));
        assertThat(wish.getInteger(MOVIEID.getName()), is(49051));
    }

    @Test
    public void testIsInWishlist(TestContext ctx) throws Exception {
        localDatabase.updateOrInsertBlocking(SQL_INSERT_WISHLIST, null);
        JsonArray wishlist = database.isInWishlist("unittest@kyngas.eu", 49051).rxSetHandler()
                .doOnError(ctx::fail)
                .map(DatabaseService::getRows)
                .toBlocking()
                .value();
        assertThat(wishlist.size(), is(1));
        JsonObject wish = wishlist.getJsonObject(0);
        assertThat(wish.getInteger(MOVIEID.getName()), is(49051));
    }

    @Test
    public void testGetSeenEpisodes(TestContext ctx) throws Exception {
        localDatabase.updateOrInsertBlocking(SQL_INSERT_SERIES_INFO, null);
        localDatabase.updateOrInsertBlocking(SQL_INSERT_SERIES_EPISODE, null);
        JsonArray episodes = database.getSeenEpisodes("unittest@kyngas.eu", 42009).rxSetHandler()
                .doOnError(ctx::fail)
                .map(DatabaseService::getRows)
                .toBlocking()
                .value();
        assertThat(episodes.size(), is(1));
        JsonObject episode = episodes.getJsonObject(0);
        assertThat(episode.getInteger(EPISODEID.getName()), is(1188308));
    }

    @Test
    public void testInsertEpisodeView(TestContext ctx) throws Exception {
        JsonObject data = new JsonObject()
                .put("seriesId", 42009)
                .put("episodeId", 1188308)
                .put("seasonId", "52595fb3760ee346619586ed");
        database.insertEpisodeView("unittest@kyngas.eu", data.encode()).rxSetHandler()
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
        database.insertWishlist("unittest@kyngas.eu", 49501).rxSetHandler()
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
        database.insertSeries(data.getInteger("Id"), data.getString("Title"), data.getString("Image"))
                .rxSetHandler().doOnError(ctx::fail).toBlocking().value();
        JsonArray series = localDatabase.queryBlocking("SELECT * FROM SeriesInfo", null);
        assertThat(series.size(), is(1));
        JsonObject show = series.getJsonObject(0);
        assertThat(data, equalTo(show));
    }

    @Test
    public void testInsertMovie(TestContext ctx) throws Exception {
        JsonObject data = new JsonObject()
                .put("Id", 49051)
                .put("Title", "The Hobbit: An Unexpected Journey")
                .put("Year", 2012)
                .put("Image", "/w29Guo6FX6fxzH86f8iAbEhQEFC.jpg");
        database.insertMovie(data.getInteger("Id"), data.getString("Title"), data.getInteger("Year"),
                data.getString("Image")).rxSetHandler().doOnError(ctx::fail).toBlocking().value();
        JsonArray movies = localDatabase.queryBlocking("SELECT * FROM Movies", null);
        assertThat(movies.size(), is(1));
        JsonObject movie = movies.getJsonObject(0);
        assertThat(data, equalTo(movie));
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
        database.insertView("unittest@kyngas.eu", data.encode()).rxSetHandler()
                .doOnError(ctx::fail).toBlocking().value();
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
        database.insertUser("test@test.ee", "ultrateam3000", "ultra", "tester")
                .rxSetHandler().toBlocking().value();
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
        assertThrewException(database.insertUser(null, "b", "c", "d"));
        assertThrewException(database.insertUser("a", null, "c", "d"));
        assertThrewException(database.insertUser("a", "b", null, "d"));
        assertThrewException(database.insertUser("a", "b", "c", null));
    }

    private void assertThrewException(Future<JsonObject> future) {
        assertThat(future.rxSetHandler()
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
                .put("is-cinema", false);
        localDatabase.updateOrInsertBlocking(SQL_INSERT_MOVIES_HOBBIT, null);
        localDatabase.updateOrInsertBlocking(SQL_INSERT_VIEW_HOBBIT, null);
        JsonObject view = getSingleItem(ctx, database.getViews("unittest@kyngas.eu", data.encode(), 0));
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
        localDatabase.updateOrInsertBlocking(SQL_INSERT_MOVIES_HOBBIT, null);
        localDatabase.updateOrInsertBlocking(SQL_INSERT_VIEW_HOBBIT, null);
        JsonObject year = getSingleItem(ctx,
                database.getYearsDistribution("unittest@kyngas.eu", data.encode()));
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
        localDatabase.updateOrInsertBlocking(SQL_INSERT_MOVIES_HOBBIT, null);
        localDatabase.updateOrInsertBlocking(SQL_INSERT_VIEW_HOBBIT, null);
        JsonObject day = getSingleItem(ctx,
                database.getWeekdaysDistribution("unittest@kyngas.eu", data.encode()));
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
        localDatabase.updateOrInsertBlocking(SQL_INSERT_MOVIES_HOBBIT, null);
        localDatabase.updateOrInsertBlocking(SQL_INSERT_VIEW_HOBBIT, null);
        JsonObject hour = getSingleItem(ctx,
                database.getTimeDistribution("unittest@kyngas.eu", data.encode()));
        assertThat(hour.getInteger("Hour"), is(17));
        assertThat(hour.getInteger("Count"), is(1));
    }

    @Test
    public void testGetMovieViews(TestContext ctx) throws Exception {
        localDatabase.updateOrInsertBlocking(SQL_INSERT_VIEW_HOBBIT, null);
        JsonObject view = getSingleItem(ctx, database.getMovieViews("unittest@kyngas.eu", "49051"));
        assertThat(view.getString("Start"), is("2017-04-23T14:58:00Z"));
        assertThat(view.getInteger("WasCinema"), is(0));
    }

    @Test
    public void testGetTotalDistinctMoviesCount(TestContext ctx) throws Exception {
        localDatabase.updateOrInsertBlocking(SQL_INSERT_VIEW_HOBBIT, null);
        localDatabase.updateOrInsertBlocking(SQL_INSERT_VIEW_GHOST, null);
        JsonObject count = getSingleItem(ctx, database.getTotalDistinctMoviesCount("unittest@kyngas.eu"));
        assertThat(count.getInteger("unique_movies"), is(2));
    }

    @Test
    public void testGetTotalRuntime(TestContext ctx) throws Exception {
        localDatabase.updateOrInsertBlocking(SQL_INSERT_VIEW_HOBBIT, null);
        localDatabase.updateOrInsertBlocking(SQL_INSERT_VIEW_GHOST, null);
        JsonObject runtime = getSingleItem(ctx, database.getTotalRuntime("unittest@kyngas.eu"));
        assertThat(runtime.getInteger("Runtime"), is(212));
    }

    @Test
    public void testGetNewMovieCount(TestContext ctx) throws Exception {
        localDatabase.updateOrInsertBlocking(SQL_INSERT_VIEW_HOBBIT, null);
        localDatabase.updateOrInsertBlocking(SQL_INSERT_VIEW_GHOST, null);
        JsonObject newMovieCount = getSingleItem(ctx, database.getNewMovieCount("unittest@kyngas.eu"));
        assertThat(newMovieCount.getInteger("new_movies"), is(1));
    }

    @Test
    public void testGetTotalMovieCount(TestContext ctx) throws Exception {
        localDatabase.updateOrInsertBlocking(SQL_INSERT_VIEW_HOBBIT, null);
        localDatabase.updateOrInsertBlocking(SQL_INSERT_VIEW_GHOST, null);
        JsonObject totalMovieCount = getSingleItem(ctx, database.getTotalMovieCount("unittest@kyngas.eu"));
        assertThat(totalMovieCount.getInteger("total_movies"), is(2));
        assertThat(totalMovieCount.getInteger("Runtime"), is(212));
    }

    @Test
    public void testGetTopMoviesHome(TestContext ctx) throws Exception {
        Map<Integer, JsonObject> data = CommonUtils.<Integer, JsonObject>mapBuilder()
                .put(49051, new JsonObject().put("Title", "The Hobbit: An Unexpected Journey").put("Count", 1))
                .put(315837, new JsonObject().put("Title", "Ghost in the Shell").put("Count", 1))
                .build();
        localDatabase.updateOrInsertBlocking(SQL_INSERT_MOVIES_HOBBIT, null);
        localDatabase.updateOrInsertBlocking(SQL_INSERT_MOVIES_GHOST, null);
        localDatabase.updateOrInsertBlocking(SQL_INSERT_VIEW_HOBBIT, null);
        localDatabase.updateOrInsertBlocking(SQL_INSERT_VIEW_GHOST, null);
        JsonArray array = database.getTopMoviesHome("unittest@kyngas.eu").rxSetHandler()
                .doOnError(ctx::fail)
                .map(DatabaseService::getRows)
                .toBlocking().value();
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

    private JsonObject getSingleItem(TestContext ctx, Future<JsonObject> future) {
        JsonArray array = future.rxSetHandler()
                .doOnError(ctx::fail)
                .map(DatabaseService::getRows)
                .toBlocking().value();
        assertThat(array.size(), is(1));
        return array.getJsonObject(0);
    }
}
