package common.verticle.rx;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rx.java.RxHelper;
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
import rx.Subscriber;
import java.util.HashSet;
import java.util.Set;

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
    return publish(HttpEndpoint.createRecord(name, host, port, "/",
        new JsonObject().put("api.name", config().getString("api.name", ""))));
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

  protected Single<Void> publishApiGateway(String host, int port) {
    return publish(HttpEndpoint
        .createRecord("api-gateway", false, host, port, "/", null)
        .setType("api-gateway"));
  }

  private Single<Void> publish(Record record) {
    return discovery.rxPublish(record)
                    .doOnSuccess(this::storeAndLogPublished)
                    .map(rec -> null);
  }

  private void storeAndLogPublished(Record record) {
    records.add(record);
    log.info("Service <" + record.getName() + "> published.");
  }

  protected final Single<Void> publishLogEvent(String type, JsonObject data) {
    log.info("Event <Type: " + type + "; data: " + data.encode() + ">");
    vertx.eventBus().publish(ADDRESS_LOG_EVENT, new JsonObject()
        .put("type", type)
        .put("message", data));
    return Single.just(null);
  }

  @Override
  public void stop(Future<Void> future) throws Exception {
    Observable.from(records)
              .flatMap(record -> discovery.rxUnpublish(record.getRegistration()).toObservable())
              .reduce((Void) null, (a, b) -> null)
              .subscribe(toSubscriber(future));
  }

  protected final <T> Subscriber<T> toSubscriber(Handler<AsyncResult<T>> handler) {
    return RxHelper.toSubscriber(handler);
  }
}