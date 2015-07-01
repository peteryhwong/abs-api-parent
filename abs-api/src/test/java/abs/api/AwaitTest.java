package abs.api;

import static com.google.common.truth.Truth.assertThat;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

/**
 * Tests around "await" API.
 */
public class AwaitTest {

  static class Network implements Actor {
    private static final long serialVersionUID = 1L;

    private final AtomicLong token = new AtomicLong(0);

    public Long newToken() {
      return token.incrementAndGet();
    }

    @Override
    public String simpleName() {
      return "network";
    }
  }

  static class Packet implements Actor {
    private static final long serialVersionUID = 1L;

    private final Network network;

    public Packet(Network network) {
      this.network = network;
    }

    public void transmit() {
      Callable<Long> message = () -> network.newToken();
      Future<Long> future = await(network, message);
      try {
        Long token = future.get();
        System.out.println("Token = " + token);
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
    }

    @Override
    public String simpleName() {
      return "packet[" + Integer.toHexString(hashCode()) + "]";
    }
  }

  static class Gateway implements Actor {
    private static final long serialVersionUID = 1L;

    private final Network network;

    public Gateway(Network network) {
      this.network = network;
    }

    public void relay() {
      Packet packet = new Packet(network);
      Runnable message = () -> packet.transmit();
      Future<Void> done = await(packet, message);
      try {
        done.get();
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
    }

    @Override
    public String simpleName() {
      return "gateway";
    }

  }

  @Test
  public void onOpenPutsEnvelopeOnHeadOfSendersAwaitingList() throws Exception {
    ExecutorService executor = Executors.newCachedThreadPool();
    Configuration configuration =
        Configuration.newConfiguration().withExecutorService(executor).build();
    Context context = new LocalContext(configuration);

    Network o1 = new Network();
    Actor o1a = context.newActor("n1", o1);

    Gateway o2 = new Gateway(o1);
    Actor o2a = context.newActor("g1", o2);

    ObjectInbox oi1 = new ObjectInbox(o1, executor);
    Callable<Long> message = () -> o1.newToken();
    Envelope o1_e1 = new AwaitEnvelope(o2a, o1a, message);
    oi1.onOpen(o1_e1, context);

    ObjectInbox oi2 = oi1.senderInbox(o1_e1, context);
    assertThat(oi2.isAwaiting()).isTrue();
    assertThat(oi2.lastAwaitingEnvelope()).isEqualTo(o1_e1);
    assertThat(oi2.isBusy()).isFalse();
  }

  @Test
  public void onCompleteRemovesEnvelopeFromHeadOfSendersAwaitingList() throws Exception {
    ExecutorService executor = Executors.newCachedThreadPool();
    Configuration configuration =
        Configuration.newConfiguration().withExecutorService(executor).build();
    Context context = new LocalContext(configuration);

    Network o1 = new Network();
    Actor o1a = context.newActor("n1", o1);

    Gateway o2 = new Gateway(o1);
    Actor o2a = context.newActor("g1", o2);

    ObjectInbox oi1 = new ObjectInbox(o1, executor);
    Callable<Long> message = () -> o1.newToken();
    Envelope o1_e1 = new AwaitEnvelope(o2a, o1a, message);
    oi1.onOpen(o1_e1, context);
    oi1.onComplete(o1_e1, context);

    ObjectInbox oi2 = oi1.senderInbox(o1_e1, context);
    assertThat(oi2.isAwaiting()).isFalse();
    assertThat(oi2.lastAwaitingEnvelope()).isNull();
  }

  @Test
  public void awaitEnsuresEnvelopeProcessedOnFutureAccess() throws Exception {
    ExecutorService executor = Executors.newCachedThreadPool();
    Configuration configuration =
        Configuration.newConfiguration().withExecutorService(executor).build();
    Context context = new LocalContext(configuration);

    Network o1 = new Network();
    Actor o1a = context.newActor("n1", o1);

    Callable<Long> message = () -> o1.newToken();
    Future<Long> f = context.await(o1a, message);
    assertThat(f.isDone()).isTrue();
    Long l = f.get();
    assertThat(l).isNotNull();
    assertThat(l).isEqualTo(o1.token.longValue());
  }

}
