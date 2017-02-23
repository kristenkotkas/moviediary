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
import org.pac4j.vertx.auth.Pac4jUser;
import server.service.DatabaseService;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static io.vertx.ext.web.handler.sockjs.BridgeEventType.SEND;
import static server.util.StringUtils.*;

public class EventBusRouter extends Routable {
    private static final Logger log = LoggerFactory.getLogger(EventBusRouter.class);

    public static final String EVENTBUS_ALL = "/eventbus/*";
    public static final String EVENTBUS = "/eventbus";

    public static final String DATABASE_USERS = "database_users";
    public static final String DATABASE_USERS_SIZE = "database_users_size";
    public static final String TEST_GATEWAY = "go_right_through";
    public static final String DATABASE_GET_HISTORY = "database_get_history";

    private final ConcurrentHashMap<String, MessageConsumer> consumers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, MessageConsumer> gateways = new ConcurrentHashMap<>();

    public EventBusRouter(Vertx vertx, DatabaseService database) {
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
        gateway(TEST_GATEWAY, log());

        // TODO: 20.02.2017 remove
        //saadab stringi sellele aadressile iga 2 sekundi tagant
        //vertx.setPeriodic(2000L, timer -> vertx.eventBus().publish(TEST_GATEWAY, "Sending publishing message!"));
    }

    private <T> void listen(String address, Handler<Message<T>> replyHandler) {
        consumers.put(address, vertx.eventBus().consumer(address, replyHandler));
    }

    private <T> void gateway(String address, Handler<Message<T>> replyHandler) {
        gateways.put(address, vertx.eventBus().consumer(address, replyHandler));
    }

    // usage: reply((user, query_parameter_from_client) -> some_function_that_returns_future(), (user, result_of_that_function) -> do_something_with_result());
    // user = users email or serial
    // query_parameter = some parameter client adds with the request (could be null)
    // some_function_that_returns_future() = for example database.getAllUsers()
    // result_of_that_function = database.getAllUsers() result (json object in this case)
    // do_something_with_result() = change the result into int for example (json -> json.size())
    private <T> Handler<Message<T>> reply(BiFunction<String, String, Future<T>> processor,
                                          BiFunction<String, T, Object> compiler) {
        return msg -> processor.apply(msg.headers().get("user"), String.valueOf(msg.body())).setHandler(ar -> {
            System.out.println(messageToString(msg));
            if (ar.succeeded()) {
                msg.reply(compiler.apply(msg.headers().get("user"), ar.result()));
            } else {
                msg.reply("Failure: " + ar.cause().getMessage());
            }
        });
    }

    // same as above but without changing the result
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
        return msg -> log.info("Gateway: " + messageToString(msg));
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
            // TODO: 23/02/2017 for all types
            if (event.type() == SEND) {
                System.out.println("--------send-----------");
                String email = ((Pac4jUser) event.socket().webUser())
                        .pac4jUserProfiles().values().stream()
                        .findAny()
                        .orElse(null)
                        .getEmail();
                // TODO: 23/02/2017 works for idcardprofile?
                event.setRawMessage(event.getRawMessage().put("headers", new JsonObject().put("user", email)));
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
