package abs.api;

import static com.google.common.truth.Truth.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
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
      Long t = token.incrementAndGet();
      return t;
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

    public Long transmit() {
      Callable<Long> message = () -> network.newToken();
      Response<Long> future = await(network, message);
      Long token = future.getValue();
      assert token != null;
      return token;
    }

    @Override
    public String simpleName() {
      return "Packet@" + Integer.toHexString(hashCode());
    }
  }

  static class Gateway implements Actor {
    private static final long serialVersionUID = 1L;

    private final Network network;

    public Gateway(Network network) {
      this.network = network;
    }

    public Long relay() {
      Packet packet = new Packet(network);
      context().newActor(packet.simpleName(), packet);
      Callable<Long> message = () -> packet.transmit();
      Response<Long> done = await(packet, message);
      Long token = done.getValue();
      assert token != null;
      return token;
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
    context.newActor("n1", o1);

    Callable<Long> message = () -> o1.newToken();
    Future<Long> f = context.await(o1, message);
    assertThat(f.isDone()).isTrue();
    Long l = f.get();
    assertThat(l).isNotNull();
    assertThat(l).isEqualTo(o1.token.longValue());
  }

  @Test
  public void relaySinglePacket() throws Exception {
    ExecutorService executor = Executors.newCachedThreadPool();
    Configuration configuration =
        Configuration.newConfiguration().withExecutorService(executor).build();
    Context context = new LocalContext(configuration);

    Network o1 = new Network();
    context.newActor("n1", o1);

    Gateway o2 = new Gateway(o1);
    context.newActor("g1", o2);

    Callable<Long> message = () -> o2.relay();
    Future<Long> f = context.await(o2, message);
    assertThat(f).isNotNull();
    assertThat(f.isDone()).isTrue();
    assertThat(f.get()).isEqualTo(o1.token.longValue());
  }

  @Test
  public void relayPacketSequence() throws Exception {
    ExecutorService executor = Executors.newCachedThreadPool();
    Configuration configuration =
        Configuration.newConfiguration().withExecutorService(executor).build();
    Context context = new LocalContext(configuration);

    Network o1 = new Network();
    context.newActor("n1", o1);

    Gateway o2 = new Gateway(o1);
    context.newActor("g1", o2);

    final int size = new Random(System.currentTimeMillis()).nextInt(256);
    List<Future<Long>> futures = new ArrayList<>();
    for (int i = 0; i < size; ++i) {
      Callable<Long> msg = () -> o2.relay();
      Future<Long> f = context.await(o2, msg);
      futures.add(f);
    }
    assertThat(futures).isNotNull();
    assertThat(futures).isNotEmpty();
    assertThat(futures).hasSize(size);

    List<Long> results = new ArrayList<>();
    for (Future<Long> f : futures) {
      try {
        Long n = f.get();
        results.add(n);
      } catch (Exception e) {
        results.add(-1L);
      }
    };
    assertThat(results).isNotNull();
    assertThat(results).isNotEmpty();
    assertThat(results).containsNoDuplicates();
    assertThat(results).isStrictlyOrdered();
    assertThat(results.get(size - 1)).isEqualTo(o1.token.get());
  }

}
