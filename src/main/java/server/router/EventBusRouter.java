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
import server.service.DatabaseService;
import server.util.StringUtils;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

public class EventBusRouter extends Routable {
    private static final Logger log = LoggerFactory.getLogger(EventBusRouter.class);

    public static final String EVENTBUS_ALL = "/eventbus/*";
    public static final String EVENTBUS = "/eventbus";

    public static final String DATABASE_USERS = "database_users";
    public static final String DATABASE_USERS_SIZE = "database_users_size";
    public static final String TEST_GATEWAY = "go_right_through";
    public static final String DATABASE_GET_HISTORY = "database_get_history";

    //consumer -> ainult sissetulevad sõnumid lubatud (ja vastused)
    //gateway -> kõik sõnumid lubatud
    private final ConcurrentHashMap<String, MessageConsumer> consumers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, MessageConsumer> gateways = new ConcurrentHashMap<>();

    public EventBusRouter(Vertx vertx, DatabaseService database) {
        super(vertx);
        listen(DATABASE_USERS, reply(param -> database.getAllUsers()));
        listen(DATABASE_USERS_SIZE, reply(param -> database.getAllUsers(), JsonObject::size));
        listen(DATABASE_GET_HISTORY, reply(param -> database.getAllViews(), new Function<JsonObject, Object>() {
            @Override
            public Object apply(JsonObject json) {
                json.remove("results");
                JsonArray array = json.getJsonArray("rows");
                for (int i = 0; i < array.size(); i++) {
                    array.getJsonObject(i).put("Start", StringUtils.getNormalDTFromDB(
                            array.getJsonObject(i).getString("Start"), StringUtils.SHORT_DATE));
                    array.getJsonObject(i).put("End", StringUtils.getNormalDTFromDB(
                            array.getJsonObject(i).getString("End"), StringUtils.SHORT_DATE));
                    array.getJsonObject(i).put("WasFirst", StringUtils.getNormalBoolean(
                            array.getJsonObject(i).getBoolean("WasFirst")));
                    array.getJsonObject(i).put("WasCinema", StringUtils.getNormalBoolean(
                            array.getJsonObject(i).getBoolean("WasCinema")));
                }
                return json;
            }
        }));
        gateway(TEST_GATEWAY, log());

        // TODO: 20.02.2017 remove
        //saadab stringi sellele aadressile iga 2 sekundi tagant
        vertx.setPeriodic(2000L, timer -> vertx.eventBus().publish(TEST_GATEWAY, "Sending publishing message!"));
    }

    private <T> void listen(String address, Handler<Message<T>> replyHandler) {
        consumers.put(address, vertx.eventBus().consumer(address, replyHandler));
    }

    private <T> void gateway(String address, Handler<Message<T>> replyHandler) {
        gateways.put(address, vertx.eventBus().consumer(address, replyHandler));
    }

    // usage: reply(query_parameter_from_client -> some_function_that_returns_future(), result_of_that_function -> do_something_with_result());
    // query_parameter = some parameter client adds with the request (could be null)
    // some_function_that_returns_future() = for example database.getAllUsers()
    // result_of_that_function = database.getAllUsers() result (json object in this case)
    // do_something_with_result() = change the result into int for example (json -> json.size())
    private <T> Handler<Message<T>> reply(Function<String, Future<T>> processor, Function<T, Object> compiler) {
        return msg -> processor.apply(String.valueOf(msg.body())).setHandler(ar -> {
            if (ar.succeeded()) {
                msg.reply(compiler.apply(ar.result()));
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
        router.route(EVENTBUS_ALL).handler(SockJSHandler.create(vertx).bridge(options));
    }

    @Override
    public void close() throws Exception {
        Stream.of(consumers, gateways)
                .flatMap(map -> map.values().stream())
                .filter(Objects::nonNull)
                .forEach(MessageConsumer::unregister);
    }
}
