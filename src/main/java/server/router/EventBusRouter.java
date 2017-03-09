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

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
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
    public static final String EVENTBUS = "/eventbus";

    public static final String DATABASE_USERS = "database_users";
    public static final String DATABASE_USERS_SIZE = "database_users_size";
    public static final String DATABASE_GET_HISTORY = "database_get_history";
    public static final String API_GET_SEARCH = "api_get_search";
    public static final String API_GET_MOVIE = "api_get_movie";
    public static final String DATABASE_GET_MOVIE_HISTORY = "database_get_movie_history";
    public static final String TRANSLATIONS = "translations";

    public static final String MESSENGER = "messenger";

    private final Map<String, MessageConsumer> consumers = new ConcurrentHashMap<>();
    private final Map<String, MessageConsumer> gateways = new ConcurrentHashMap<>();

    public EventBusRouter(Vertx vertx, DatabaseService database, TmdbService tmdb) {
        super(vertx);
        listen(DATABASE_USERS, reply(param -> database.getAllUsers()));
        listen(DATABASE_USERS_SIZE, reply((user, param) -> database.getAllUsers(), (user, json) -> json.size()));
        listen(DATABASE_GET_HISTORY, reply(database::getViews, (user, json) -> {
            json.remove("results");
            JsonArray array = json.getJsonArray("rows");
            for (int i = 0; i < array.size(); i++) {
                array.getJsonObject(i).put("WasFirst", getFirstSeen(
                        array.getJsonObject(i).getBoolean("WasFirst")));
                array.getJsonObject(i).put("WasCinema", getCinema(
                        array.getJsonObject(i).getBoolean("WasCinema")));
                array.getJsonObject(i).put("DayOfWeek", toCapital(getWeekdayFromDB(array.getJsonObject(i)
                        .getString("Start")).substring(0, 3)));
                array.getJsonObject(i).put("Time", toNormalTime(array.getJsonObject(i).getString("Start")));
                array.getJsonObject(i).put("Start", getNormalDTFromDB(
                        array.getJsonObject(i).getString("Start"), LONG_DATE));
            }
            return json;
        }));
        listen(API_GET_SEARCH, reply(tmdb::getMovieByName));
        listen(API_GET_MOVIE, reply(tmdb::getMovieById));
        listen(DATABASE_GET_MOVIE_HISTORY, reply(database::getMovieViews, (user, json) -> {
            json.remove("results");
            JsonArray array = json.getJsonArray("rows");
            for (int i = 0; i < array.size(); i++) {
                array.getJsonObject(i).put("WasCinema", getCinema(
                        array.getJsonObject(i).getBoolean("WasCinema")));
                array.getJsonObject(i).put("Start", getNormalDTFromDB(
                        array.getJsonObject(i).getString("Start"), LONG_DATE));
            }
            return json;
        }));
        listen(TRANSLATIONS, reply(Language::getJsonTranslations));
        gateway(MESSENGER, log());
    }

    private <T> void listen(String address, Handler<Message<T>> replyHandler) {
        consumers.put(address, vertx.eventBus().consumer(address, replyHandler));
    }

    private <T> void gateway(String address, Handler<Message<T>> replyHandler) {
        gateways.put(address, vertx.eventBus().consumer(address, replyHandler));
    }

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

    private <T> Handler<Message<T>> reply(Function<String, Future<T>> processor) {
        return msg -> processor.apply(String.valueOf(msg.body())).setHandler(ar -> {
            if (ar.succeeded()) {
                msg.reply(ar.result());
            } else {
                msg.reply("Failure: " + ar.cause().getMessage());
            }
        });
    }

    // publish received reply to everyone registered (listening) on the address
    private <T> Handler<Message<T>> publish(String address, Function<String, Future<T>> processor,
                                            Function<T, Object> compiler) {
        return msg -> processor.apply(String.valueOf(msg.body())).setHandler(ar -> {
            if (ar.succeeded()) {
                vertx.eventBus().publish(address, compiler.apply(ar.result()));
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
                        .put("headers", new JsonObject()
                                .put("user", profile.getEmail())
                                .put("name", profile.getFirstName())));
            }
            event.complete(true);
        }));
    }

    @Override
    public void close() throws Exception {
        Stream.of(consumers, gateways)
                .flatMap(map -> map.values().stream())
                .filter(Objects::nonNull)
                .forEach(MessageConsumer::unregister);
    }
}
