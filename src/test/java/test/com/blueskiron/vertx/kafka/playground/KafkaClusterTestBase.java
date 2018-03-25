package test.com.blueskiron.vertx.kafka.playground;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

import org.apache.kafka.clients.producer.Producer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import io.debezium.kafka.KafkaCluster;
import io.debezium.util.Testing;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.kafka.client.consumer.KafkaReadStream;
import io.vertx.kafka.client.producer.KafkaWriteStream;

@RunWith(VertxUnitRunner.class)
public class KafkaClusterTestBase {

  private static File dataDir;
  protected static KafkaCluster kafkaCluster;

  protected static KafkaCluster kafkaCluster() {
    if (kafkaCluster != null) {
      throw new IllegalStateException();
    }
    dataDir = Testing.Files.createTestingDirectory("cluster");
    kafkaCluster = new KafkaCluster().usingDirectory(dataDir).withPorts(2181, 9092);
    return kafkaCluster;
  }

  @BeforeClass
  public static void setUp(TestContext ctx) throws IOException {
    kafkaCluster = kafkaCluster().deleteDataPriorToStartup(true).addBrokers(1).startup();
  }


  @AfterClass
  public static void tearDown(TestContext ctx) {
    if (kafkaCluster != null) {
      kafkaCluster.shutdown();
      kafkaCluster = null;
      boolean delete = dataDir.delete();
      // If files are still locked and a test fails: delete on exit to allow subsequent test execution
      if(!delete) {
        dataDir.deleteOnExit();
      }
    }
  }
  
  /**
   * @param ctx
   * @param producer
   */
  static void close(TestContext ctx, Consumer<Handler<AsyncResult<Void>>> producer) {
    if (producer != null) {
      Async closeAsync = ctx.async();
      producer.accept(v -> {
        closeAsync.complete();
      });
      closeAsync.awaitSuccess(10000);
    }
  }

  /**
   * @param ctx
   * @param producer
   */
  static void close(TestContext ctx, KafkaWriteStream<?, ?> producer) {
    if (producer != null) {
      close(ctx, handler -> producer.close(2000L, handler));
    }
  }

  /**
   * @param ctx
   * @param consumer
   */
  static void close(TestContext ctx, KafkaReadStream<?, ?> consumer) {
    if (consumer != null) {
      close(ctx, consumer::close);
    }
  }

  /**
   * @param cfg
   * @return
   */
  static Map<String, String> mapConfig(Properties cfg) {
    Map<String ,String> map = new HashMap<>();
    cfg.forEach((k, v) -> map.put("" + k, "" + v));
    return map;
  }

  /**
   * @param vertx
   * @param config
   * @return
   * @throws Exception
   */
  static <K, V> KafkaWriteStream<K, V> producer(Vertx vertx, Properties config) throws Exception {
    return KafkaWriteStream.create(vertx, config);
  }

  /**
   * @param vertx
   * @param config
   * @param keyType
   * @param valueType
   * @return
   */
  static <K, V> KafkaWriteStream<K, V> producer(Vertx vertx, Properties config, Class<K> keyType, Class<V> valueType) {
    return KafkaWriteStream.create(vertx, config, keyType, valueType);
  }

  /**
   * @param vertx
   * @param producer
   * @return
   */
  static <K, V> KafkaWriteStream<K, V> producer(Vertx vertx, Producer<K, V> producer) {
    return KafkaWriteStream.create(vertx, producer);
  }
}
