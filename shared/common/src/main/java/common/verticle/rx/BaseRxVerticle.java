package common.verticle.rx;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;
import io.vertx.servicediscovery.types.EventBusService;
import io.vertx.servicediscovery.types.HttpEndpoint;
import io.vertx.servicediscovery.types.JDBCDataSource;
import io.vertx.servicediscovery.types.MessageSource;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.Single;
import java.util.HashSet;
import java.util.Set;
import static common.util.ConditionUtils.ifFalse;
import static common.util.ConditionUtils.ifTrue;
import static common.util.rx.RxUtils.single;
import static common.util.rx.RxUtils.toSubscriber;
import static java.lang.String.format;
import static java.util.Objects.isNull;

/**
 * @author <a href="https://github.com/kristjanhk">Kristjan Hendrik Küngas</a>
 */
@Slf4j
public abstract class BaseRxVerticle extends AbstractVerticle {
  private static final String ADDRESS_LOG_EVENT = "events.log"; // TODO: 22.08.2017 to config
  protected final Set<Record> records = new HashSet<>();
  protected ServiceDiscovery discovery;

  @Override
  public void start() throws Exception {
    discovery = ServiceDiscovery.create(vertx, new ServiceDiscoveryOptions().setBackendConfiguration(config()));
  }

  protected Single<Void> publishHttpEndpoint(String name, String host, int port) {
    ifTrue(config().getInteger("http.port") == null,
        () -> log.error(format("Could not find port for http endpoint %s, using host %s, port %s.", name, host, port)));
    return publish(HttpEndpoint.createRecord(name, host, port, "/", new JsonObject().put("api.name", name)));
  }

  protected Single<Void> publishHttpEndpoint(int port) {
    return publishHttpEndpoint(config().getString("api.name", ""), "localhost", port);
  }

  protected Single<Void> publishMessageSource(String name, String address) {
    return publish(MessageSource.createRecord(name, address));
  }

  protected Single<Void> publishJDBCDataSource(String name, JsonObject location) {
    return publish(JDBCDataSource.createRecord(name, location, new JsonObject()));
  }

  protected Single<Void> publishEventBusService(String name, String address, Class serviceClass) {
    return publish(EventBusService.createRecord(name, address, serviceClass));
  }

  protected Single<Void> publishEventBusService(Class serviceClass) {
    String apiName = config().getString("api.name", "");
    return publish(EventBusService.createRecord(apiName, apiName, serviceClass));
  }

  protected Single<Void> publishApiGateway(String host, int port) {
    return publish(HttpEndpoint
        .createRecord("api-gateway", false, host, port, "/", null)
        .setType("api-gateway"));
  }

  private Single<Void> publish(Record record) {
    if (isNull(discovery)) {
      return Single.error(new Throwable(format("ServiceDiscovery is null, did you forget to call super()?, record: %s",
          record.getLocation().encode())));
    }
    return discovery.rxPublish(record)
                    .doOnSuccess(this::storeAndLogPublished)
                    .map(rec -> null);
  }

  private void storeAndLogPublished(Record record) {
    records.add(record);
    ifFalse("eventbus-service-proxy".equals(record.getType()),
        () -> log.info(format("Service <%s> published @ <%s>",
            record.getName(),
            record.getLocation().getString("endpoint")))
    );
  }

  protected final Single<Void> publishLogEvent(String type, JsonObject data) {
    log.info(format("Event <Type: %s; data: %s>", type, data.encode()));
    vertx.eventBus().publish(ADDRESS_LOG_EVENT, new JsonObject()
        .put("type", type)
        .put("message", data));
    return single();
  }

  @Override
  public void stop(Future<Void> future) throws Exception {
    Observable.from(records)
              .flatMap(record -> discovery.rxUnpublish(record.getRegistration()).toObservable())
              .reduce((Void) null, (a, b) -> null)
              .subscribe(toSubscriber(future));
  }
}