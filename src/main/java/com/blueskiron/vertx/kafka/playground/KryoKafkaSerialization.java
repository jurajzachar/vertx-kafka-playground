package com.blueskiron.vertx.kafka.playground;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.objenesis.strategy.StdInstantiatorStrategy;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;

import io.netty.util.internal.StringUtil;
import lombok.Data;

/**
 * Kryo serialization context which implements Kafka {@link Serializer} and
 * {@link Deserializer}.
 * 
 * @author juraj.zachar@gmail.com
 *
 * @param <T>
 */
public class KryoKafkaSerialization<T> implements Serializer<T>, Deserializer<T> {

  private KryoPool pool = new KryoPool.Builder(new DefaultKryoFactory()).build();

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    String kryoFactory = Options.KRYO_FACTORY;
    if (configs.containsKey(kryoFactory)) {
      Options options = new Options((String) configs.get(kryoFactory));
      KryoFactory factory;
      try {
        factory = options.instantiate();
        pool = new KryoPool.Builder(factory).build();
      } catch (ClassNotFoundException | ClassCastException | InstantiationException | IllegalAccessException e) {
        throw new IllegalStateException("cannot configure serialization context, becase " + e);
      }
    }
  }

  @Override
  public byte[] serialize(String topic, T data) {
    final ByteArrayOutputStream stream = new ByteArrayOutputStream();
    final Output output = new Output(stream);
    Kryo kryo = pool.borrow();
    kryo.writeClassAndObject(output, data);
    output.close(); // also calls output.flush()
    pool.release(kryo);
    return stream.toByteArray(); // serialization done, get bytes
  }

  @SuppressWarnings("unchecked")
  @Override
  public T deserialize(String topic, byte[] data) {
    // reverse the above process
    final ByteArrayInputStream stream = new ByteArrayInputStream(data);
    final Input input = new Input(stream);
    Kryo kryo = pool.borrow();
    Object object = kryo.readClassAndObject(input);
    input.close();
    pool.release(kryo);
    return (T) object;
  }

  @Override
  public void close() {
    // nothing happens here
  }

  /**
   * @author juraj.zachar@gmail.com
   *
   */
  private static final class DefaultKryoFactory implements KryoFactory {

    @Override
    public Kryo create() {
      Kryo kryo = new Kryo();
      kryo.setWarnUnregisteredClasses(true);
      kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
      return kryo;
    }

  }

  @Data
  public static final class Options {
    public static final String KRYO_FACTORY = "kryo_factory";
    private final String kryoFactory;

    public KryoFactory instantiate()
        throws ClassNotFoundException, ClassCastException, InstantiationException, IllegalAccessException {
      if (StringUtil.isNullOrEmpty(kryoFactory)) {
        throw new IllegalStateException("kryo factory name cannot be null or empty");
      }
      Class<?> klazz = Class.forName(kryoFactory);
      return (KryoFactory) klazz.newInstance();
    }
  }
}
