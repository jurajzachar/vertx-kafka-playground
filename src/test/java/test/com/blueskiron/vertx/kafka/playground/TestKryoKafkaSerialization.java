package test.com.blueskiron.vertx.kafka.playground;

import java.math.BigDecimal;
import java.util.Base64;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.blueskiron.vertx.kafka.playground.KryoKafkaSerialization;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Unit test for simple App.
 */
@RunWith(JUnit4.class)
public class TestKryoKafkaSerialization {

  @Test
  public void testCase_shouldSerializeDeserializeNestedImmutableObject() {
    Payload payload = new Payload("someString", 1, true, BigDecimal.ONE);
    Envelope<Payload> envelope = new Envelope<>("some meta data go here", payload);
    System.out.println("sample immutable envelope object: " + envelope);
    
    KryoKafkaSerialization<Envelope<Payload>> serialization = new KryoKafkaSerialization<>();
    
    //serialize
    byte[] data = serialization.serialize("some topic name", envelope);
    System.out.println("serialized: " + new String(Base64.getEncoder().encode(data)));
    
    //deserialize
    Envelope<Payload> deserializedEnvelope = serialization.deserialize("some topic name", data);
    System.out.println("deserialized: " + deserializedEnvelope);
    
    Assert.assertEquals(envelope, deserializedEnvelope);
    
  }
  
  @EqualsAndHashCode @ToString
  private static final class Envelope<T> {
    @Getter
    private final String meta;
    @Getter
    private final T payload; 
    
    private Envelope(String meta, T payload) {
      this.meta = meta;
      this.payload = payload;
    }
  }
  
  @EqualsAndHashCode @ToString
  private static final class Payload {
    @Getter
    private final String a;
    @Getter
    private final int b;
    @Getter
    private final boolean c;
    @Getter
    private final BigDecimal d;

    private Payload(String a, int b, boolean c, BigDecimal d) {
      super();
      this.a = a;
      this.b = b;
      this.c = c;
      this.d = d;
    }
  }
}
