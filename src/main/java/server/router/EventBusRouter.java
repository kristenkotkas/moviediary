package server.router;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.vertx.auth.Pac4jUser;
import server.entity.Language;
import server.service.DatabaseService;
import server.service.TmdbService;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static io.vertx.ext.web.handler.sockjs.BridgeEventType.RECEIVE;
import static server.util.StringUtils.*;

/**
 * Contains addresses that eventbus listens and gateways on.
 */
public class EventBusRouter extends Routable {
    private static final Logger LOG = LoggerFactory.getLogger(EventBusRouter.class);
    public static final String EVENTBUS_ALL = "/eventbus/*";

    //Addresses that are used in eventbus
    public static final String DATABASE_USERS = "database_users";
    public static final String DATABASE_USERS_SIZE = "database_users_size";
    public static final String DATABASE_GET_HISTORY = "database_get_history";
    public static final String API_GET_SEARCH = "api_get_search";
    public static final String API_GET_MOVIE = "api_get_movie";
    public static final String DATABASE_GET_MOVIE_HISTORY = "database_get_movie_history";
    public static final String TRANSLATIONS = "translations";
    public static final String MESSENGER = "messenger";

    private final Map<String, MessageConsumer> consumers = new HashMap<>();
    private final Map<String, MessageConsumer> gateways = new HashMap<>();

    /**
     * Sets up addresses and their responses.
     */
    public EventBusRouter(Vertx vertx, DatabaseService database, TmdbService tmdb) {
        super(vertx);
        listen(DATABASE_USERS, reply(param -> database.getAllUsers()));
        listen(DATABASE_USERS_SIZE, reply((user, param) -> database.getAllUsers(), (user, json) -> json.size()));
        listen(DATABASE_GET_HISTORY, reply((username, param) -> database.getViews(username, param,
                new JsonObject(param).getInteger("page")), getDatabaseHistory()));
        listen(API_GET_SEARCH, reply(tmdb::getMovieByName));
        listen(API_GET_MOVIE, reply(tmdb::getMovieById));
        listen(DATABASE_GET_MOVIE_HISTORY, reply(database::getMovieViews, getDatabaseMovieHistory()));
        listen(TRANSLATIONS, reply(Language::getJsonTranslations));
        gateway(MESSENGER, log());
    }

    /**
     * Based on username and JsonObject parameter -> returns database history results.
     */
    private BiFunction<String, JsonObject, Object> getDatabaseHistory() {
        return (user, json) -> {
            json.remove("results");
            JsonArray array = json.getJsonArray("rows");
            for (int i = 0; i < array.size(); i++) {
                JsonObject jsonObject = array.getJsonObject(i);
                jsonObject.put("WasFirst", getFirstSeen(jsonObject.getBoolean("WasFirst")));
                jsonObject.put("WasCinema", getCinema(jsonObject.getBoolean("WasCinema")));
                jsonObject.put("DayOfWeek", getWeekdayFromDB(jsonObject.getString("Start")));
                jsonObject.put("Time", toNormalTime(jsonObject.getString("Start")));
                jsonObject.put("Start", getNormalDTFromDB(jsonObject.getString("Start"), LONG_DATE));
            }
            return json;
        };
    }

    /**
     * Based on username and JsonObject parameter -> returns database movie history results.
     */
    private BiFunction<String, JsonObject, Object> getDatabaseMovieHistory() {
        return (user, json) -> {
            json.remove("results");
            JsonArray array = json.getJsonArray("rows");
            for (int i = 0; i < array.size(); i++) {
                JsonObject jsonObject = array.getJsonObject(i);
                jsonObject.put("WasCinema", getCinema(jsonObject.getBoolean("WasCinema")));
                jsonObject.put("Start", getNormalDTFromDB(jsonObject.getString("Start"), LONG_DATE));
            }
            return json;
        };
    }

    /**
     * Request-response type of messaging.
     * Listens on address and replies.
     * Only inbound messages are allowed (and replies).
     *
     * @param address      to listen on
     * @param replyHandler to call for reply handling
     */
    private <T> void listen(String address, Handler<Message<T>> replyHandler) {
        consumers.put(address, vertx.eventBus().consumer(address, replyHandler));
    }

    /**
     * Request-response type of messaging.
     * Listens on address and replies.
     * Both inbound and outbound messages are allowed.
     *
     * @param address      to listen on
     * @param replyHandler to call for reply handling
     */
    private <T> void gateway(String address, Handler<Message<T>> replyHandler) {
        gateways.put(address, vertx.eventBus().consumer(address, replyHandler));
    }

    /**
     * Reply to message.
     *
     * @param processor to get some data from some service
     * @param compiler  to transform service data into usable form
     * @return replyHandler
     */
    private <T> Handler<Message<T>> reply(BiFunction<String, String, Future<T>> processor,
                                          BiFunction<String, T, Object> compiler) {
        return msg -> processor.apply(msg.headers().get("user"), String.valueOf(msg.body())).setHandler(ar -> {
            if (ar.succeeded()) {
                msg.reply(compiler.apply(msg.headers().get("user"), ar.result()));
            } else {
                msg.reply("Failure: " + ar.cause().getMessage());
            }
        });
    }

    /**
     * Reply to message.
     *
     * @param processor to get some data from some service
     * @return replyHandler
     */
    private <T> Handler<Message<T>> reply(Function<String, Future<T>> processor) {
        return msg -> processor.apply(String.valueOf(msg.body())).setHandler(ar -> {
            if (ar.succeeded()) {
                msg.reply(ar.result());
            } else {
                msg.reply("Failure: " + ar.cause().getMessage());
            }
        });
    }

    private <T> Handler<Message<T>> log() {
        return msg -> LOG.info("Gateway: " + messageToString(msg));
    }

    private <T> String messageToString(Message<T> msg) {
        return "Msg{address='" + msg.address() + '\'' +
                ", replyAddress='" + msg.replyAddress() + '\'' +
                ", body=" + msg.body() + '}';
    }

    /**
     * Permits messages to move on eventbus that have been specified in listeners and gateways.
     * Enables eventbus.
     * Adds current users username and firstname to message headers.
     */
    @Override
    public void route(Router router) {
        BridgeOptions options = new BridgeOptions();
        consumers.keySet().stream()
                .map(key -> new PermittedOptions().setAddress(key))
                .forEach(options::addInboundPermitted);
        gateways.keySet().stream()
                .map(key -> new PermittedOptions().setAddress(key))
                .forEach(permitted -> options.addInboundPermitted(permitted).addOutboundPermitted(permitted));
        router.route(EVENTBUS_ALL).handler(SockJSHandler.create(vertx).bridge(options, event -> {
            if (event.getRawMessage() != null && event.type() != RECEIVE) {
                CommonProfile profile = ((Pac4jUser) event.socket().webUser())
                        .pac4jUserProfiles().values().stream()
                        .findAny()
                        .orElse(null);
                event.setRawMessage(event.getRawMessage()
                        .put("headers", event.getRawMessage().getJsonObject("headers", new JsonObject())
                                .put("user", profile.getEmail())
                                .put("name", profile.getFirstName())));
            }
            event.complete(true);
        }));
    }

    /**
     * Unregisters all listener and gateway addresses.
     */
    @Override
    public void close() throws Exception {
        Stream.of(consumers, gateways)
                .flatMap(map -> map.values().stream())
                .filter(Objects::nonNull)
                .forEach(MessageConsumer::unregister);
    }
}
