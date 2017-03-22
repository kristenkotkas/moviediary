package server.router;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.vertx.auth.Pac4jUser;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static io.vertx.ext.web.handler.sockjs.BridgeEventType.RECEIVE;

/**
 * Generic class that contains routes.
 */
public abstract class EventBusRoutable {
    private static final Logger LOG = LoggerFactory.getLogger(EventBusRoutable.class);
    protected final Vertx vertx;

    public static final String EVENTBUS_ALL = "/eventbus/*";
    public static final String EVENTBUS = "/eventbus";

    protected static final Map<String, MessageConsumer> consumers = new HashMap<>();
    protected static final Map<String, MessageConsumer> gateways = new HashMap<>();

    public EventBusRoutable(Vertx vertx) {
        this.vertx = vertx;
    }

    public abstract void route(Router router);

    /**
     * Request-response type of messaging.
     * Listens on address and replies.
     * Only inbound messages are allowed (and replies).
     *
     * @param address      to listen on
     * @param replyHandler to call for reply handling
     */
    protected <T> void listen(String address, Handler<Message<T>> replyHandler) {
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
    protected <T> void gateway(String address, Handler<Message<T>> replyHandler) {
        gateways.put(address, vertx.eventBus().consumer(address, replyHandler));
    }

    /**
     * Listen for messages on address and processes them.
     *
     * @param address to listen on
     */
    protected  <T> void listen(String address, BiFunction<String, String, Future<T>> processor) {
        consumers.put(address, vertx.eventBus().consumer(address,
                msg -> processor.apply(msg.headers().get("user"), String.valueOf(msg.body()))));
    }

    /**
     * Reply to message.
     *
     * @param processor to get some data from some service
     * @param compiler  to transform service data into usable form
     * @return replyHandler
     */
    protected  <T> Handler<Message<T>> reply(BiFunction<String, String, Future<T>> processor,
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
    protected <T> Handler<Message<T>> reply(Function<String, Future<T>> processor) {
        return msg -> processor.apply(String.valueOf(msg.body())).setHandler(ar -> {
            if (ar.succeeded()) {
                msg.reply(ar.result());
            } else {
                msg.reply("Failure: " + ar.cause().getMessage());
            }
        });
    }

    protected <T> Handler<Message<T>> log() {
        return msg -> LOG.info("Gateway: " + messageToString(msg));
    }

    protected <T> String messageToString(Message<T> msg) {
        return "Msg{address='" + msg.address() + '\'' +
                ", replyAddress='" + msg.replyAddress() + '\'' +
                ", body=" + msg.body() + '}';
    }

    /**
     * Permits messages to move on eventbus that have been specified in listeners and gateways.
     * Enables eventbus.
     * Adds current users username and firstname to message headers.
     */
    public static void startEventbus(Router router, Vertx vertx) {
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

    public static void closeEventbus() throws Exception {
        Stream.of(consumers, gateways)
                .flatMap(map -> map.values().stream())
                .filter(Objects::nonNull)
                .forEach(MessageConsumer::unregister);
    }
}