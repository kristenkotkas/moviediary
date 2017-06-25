package server.router;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.ext.web.handler.sockjs.BridgeEventType;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.Message;
import io.vertx.rxjava.core.eventbus.MessageConsumer;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.handler.sockjs.SockJSHandler;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.vertx.auth.Pac4jUser;
import rx.Single;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static io.vertx.core.logging.LoggerFactory.getLogger;
import static io.vertx.ext.web.handler.sockjs.BridgeEventType.RECEIVE;
import static java.lang.String.valueOf;
import static util.ConditionUtils.ifPresent;
import static util.ConditionUtils.ifTrue;

/**
 * Generic class that contains routes.
 */
public abstract class EventBusRoutable {
  public static final String EVENTBUS_ALL = "/eventbus/*";
  protected static final Map<String, MessageConsumer> CONSUMERS = new HashMap<>();
  protected static final Map<String, MessageConsumer> GATEWAYS = new HashMap<>();
  protected static final Map<String, Integer> CURRENT_USERS = new HashMap<>();
  protected static final String TRANSLATIONS = "translations";
  protected static final String MESSENGER = "messenger";
  protected static final String MESSENGER_CURRENT_USERS = "messenger_current_users";
  protected static final String MESSENGER_QUERY_USERS = "messenger_query_users";
  private static final Logger LOG = getLogger(EventBusRoutable.class);
  protected final Vertx vertx;

  public EventBusRoutable(Vertx vertx) {
    this.vertx = vertx;
  }

  /**
   * Permits messages to move on eventbus that have been specified in listeners and gateways.
   * Enables eventbus.
   * Adds current users username and firstname to message headers.
   */
  public static void startEventbus(Router router, Vertx vertx) {
    BridgeOptions options = new BridgeOptions();
    CONSUMERS.keySet().stream()
        .map(key -> new PermittedOptions().setAddress(key))
        .forEach(options::addInboundPermitted);
    GATEWAYS.keySet().stream()
        .map(key -> new PermittedOptions().setAddress(key))
        .forEach(permitted -> options.addInboundPermitted(permitted).addOutboundPermitted(permitted));
    router.route(EVENTBUS_ALL).handler(SockJSHandler.create(vertx).bridge(options, event -> {
      if (event.type() == BridgeEventType.SOCKET_CLOSED) {
        CommonProfile profile = ((Pac4jUser) event.socket().webUser().getDelegate())
            .pac4jUserProfiles().values().stream()
            .findAny()
            .orElse(null);
        CURRENT_USERS.compute(profile.getEmail(), (s, i) -> i == 1 ? null : i - 1);
        vertx.eventBus().publish("messenger_current_users", CURRENT_USERS.keySet().size());
      } else if (event.type() == BridgeEventType.SOCKET_CREATED) {
        CommonProfile profile = ((Pac4jUser) event.socket().webUser().getDelegate())
            .pac4jUserProfiles().values().stream()
            .findAny()
            .orElse(null);
        CURRENT_USERS.compute(profile.getEmail(), (s, i) -> i == null ? 1 : i + 1);
        vertx.eventBus().publish("messenger_current_users", CURRENT_USERS.keySet().size());
      }
      ifTrue(event.getRawMessage() != null && event.type() != RECEIVE, () ->
          ifPresent(((Pac4jUser) event.socket().webUser().getDelegate())
              .pac4jUserProfiles().values().stream()
              .findAny()
              .orElse(null), profile -> event.setRawMessage(event.getRawMessage()
              .put("headers", event.getRawMessage().getJsonObject("headers", new JsonObject())
                  .put("user", profile.getEmail())
                  .put("name", profile.getFirstName())))));
      event.complete(true);
    }));
  }

  public static void closeEventbus() throws Exception {
    Stream.of(CONSUMERS, GATEWAYS)
        .flatMap(map -> map.values().stream())
        .filter(Objects::nonNull)
        .forEach(MessageConsumer::unregister);
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
    CONSUMERS.put(address, vertx.eventBus().consumer(address, replyHandler));
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
    GATEWAYS.put(address, vertx.eventBus().consumer(address, replyHandler));
  }

  /**
   * Reply to message.
   *
   * @param processor to get some data from some service
   * @param compiler  to transform service data into usable form
   * @return replyHandler
   */
  protected <T> Handler<Message<T>> reply(BiFunction<String, String, Single<T>> processor,
                                          BiFunction<String, T, Object> compiler) {
    return msg -> processor.apply(msg.headers().get("user"), valueOf(msg.body()))
        .doOnError(err -> msg.reply("Failure: " + err.getMessage()))
        .subscribe(result -> msg.reply(compiler.apply(msg.headers().get("user"), result)));
  }

  protected <T> Handler<Message<T>> reply(BiFunction<String, String, Single<T>> processor) {
    return msg -> processor.apply(msg.headers().get("user"), valueOf(msg.body()))
        .subscribe(msg::reply, err -> msg.reply("Failure: " + err.getMessage()));
  }

  /**
   * Reply to message.
   *
   * @param processor to get some data from some service
   * @return replyHandler
   */
  protected <T> Handler<Message<T>> reply(Function<String, Single<T>> processor) {
    return msg -> processor.apply(valueOf(msg.body()))
        .subscribe(msg::reply, err -> msg.reply("Failure: " + err.getMessage()));
  }

  protected <T> Handler<Message<T>> log() {
    return msg -> LOG.info("Gateway: " + messageToString(msg));
  }

  protected <T> String messageToString(Message<T> msg) {
    return "Msg{address='" + msg.address() + '\'' +
        ", replyAddress='" + msg.replyAddress() + '\'' +
        ", body=" + msg.body() + '}';
  }
}
