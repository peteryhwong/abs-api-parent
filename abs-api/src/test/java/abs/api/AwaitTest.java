package abs.api;

import static com.google.common.truth.Truth.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;

/**
 * Tests around "await" API.
 */
public class AwaitTest {

  static {
    System.setProperty(Configuration.PROPERTY_THREAD_MANAGEMENT, "false");
  }

  private final Random random = new Random(System.currentTimeMillis());

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

    @Override
    public String toString() {
      return simpleName();
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

    @Override
    public String toString() {
      return simpleName();
    }
  }

  static class Gateway implements Actor {
    private static final long serialVersionUID = 1L;

    private final Network network;

    public Gateway(Network network) {
      this.network = network;
    }

    public List<Long> relay(final int size) {
      List<Response<Long>> result = new ArrayList<>();
      for (int i = 1; i <= size; ++i) {
        Packet packet = new Packet(network);
        context().newActor(packet.simpleName(), packet);
        Callable<Long> message = () -> packet.transmit();
        Response<Long> res = await(packet, message);
        result.add(res);
      }
      List<Long> tokens = new ArrayList<>();
      for (int i = 0; i < result.size(); ++i) {
        Response<Long> r = result.get(i);
        // System.out.println("Waiting= " + r);
        Long t = r.getValue();
        // System.out.println("t=" + t);
        assertThat(t).isNotNull();
        tokens.add(t);
      }
      return tokens;
    }

    @Override
    public String simpleName() {
      return "gateway";
    }

    @Override
    public String toString() {
      return simpleName();
    }

  }

  static class GuardHolder implements Actor {
    private static final long serialVersionUID = 1L;

    private final AtomicInteger releaseCounter = new AtomicInteger(0);
    private final AtomicInteger guardCounter = new AtomicInteger(0);
    public final List<Boolean> guards;

    public GuardHolder(final int size) {
      this.guards = IntStream.range(0, size).boxed().map(i -> false).collect(Collectors.toList());
    }

    public synchronized void progress() {
      if (guardCounter.get() < guards.size()) {
        if (guards.get(guardCounter.get())) {
          guardCounter.incrementAndGet();
          return;
        }
        Supplier<Boolean> condition = () -> guards.get(guardCounter.get());
        await(this, condition);
        guardCounter.incrementAndGet();
      }
    }

    public synchronized void release() {
      if (releaseCounter.get() < guards.size()) {
        guards.set(releaseCounter.get(), true);
        releaseCounter.incrementAndGet();
      }
    }

    @Override
    public String simpleName() {
      return "guard-holder";
    }

  }

  public static class ReleaseMsg implements Runnable {
    private final GuardHolder gh;

    public ReleaseMsg(GuardHolder gh) {
      this.gh = gh;
    }

    @Override
    public void run() {
      gh.release();
    }
  }

  public static class ProgressMsg implements Runnable {
    private final GuardHolder gh;

    public ProgressMsg(GuardHolder gh) {
      this.gh = gh;
    }

    @Override
    public void run() {
      gh.progress();
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

    ObjectInbox oi2 = oi1.senderInbox(o1_e1, context);
    oi2.onAwaitStart(o1_e1, context);

    assertThat(oi2.isAwaiting()).isTrue();
    assertThat(oi2.isProcessingEnvelope()).isFalse();
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

    Network network = new Network();
    context.newActor("network", network);

    Gateway gateway = new Gateway(network);
    context.newActor("gateway", gateway);

    Callable<List<Long>> message = () -> gateway.relay(1);
    Response<List<Long>> r = context.await(gateway, message);
    List<Long> tokens = r.getValue();
    assertThat(r).isNotNull();
    assertThat(tokens.get(0)).isEqualTo(1);
  }

  @Test
  public void relayPacketSequence() throws Exception {
    Context context = Configuration.newConfiguration().buildContext();

    Network network = new Network();
    context.newActor("network", network);

    Gateway gateway = new Gateway(network);
    context.newActor("gateway", gateway);

    final int size = random.nextInt(50) + 1;
    Callable<List<Long>> msg = () -> gateway.relay(size);
    Response<List<Long>> r = context.await(gateway, msg);
    assertThat(r).isNotNull();
    List<Long> tokens = r.getValue();
    assertThat(tokens).isNotEmpty();
    assertThat(tokens).hasSize(size);
    assertThat(tokens).containsNoDuplicates();
    assertThat(tokens).isStrictlyOrdered();
  }

  @Test
  public void awaitBoolean() throws Exception {
    ExecutorService E = Executors.newCachedThreadPool();
    Context context = Configuration.newConfiguration().buildContext();
    final int size = 16 + random.nextInt(512);
    GuardHolder gh = new GuardHolder(size);
    context.newActor(gh.simpleName(), gh);

    List<CompletableFuture<Void>> progresses = new ArrayList<>();
    Runnable progressMsg = () -> {
      for (int i = 0; i < size; i++) {
        Response<Void> r = context.await(gh, new ProgressMsg(gh));
        progresses.add((CompletableFuture<Void>) r);
      }
    };

    List<CompletableFuture<Void>> releases = new ArrayList<>();
    Runnable releaseMsg = () -> {
      for (int i = 0; i <= size; i++) {
        Response<Void> r = context.send(gh, new ReleaseMsg(gh));
        releases.add((CompletableFuture<Void>) r);
      }
    };

    Future<?> release = E.submit(releaseMsg);
    Future<?> progress = E.submit(progressMsg);

    CompletableFuture.allOf(releases.toArray(new CompletableFuture[0]));
    CompletableFuture.allOf(progresses.toArray(new CompletableFuture[0]));
    release.get();
    progress.get();

    assertThat(release.isDone()).isTrue();
    assertThat(gh.guards).doesNotContain(false);
    assertThat(gh.releaseCounter.get()).isEqualTo(gh.guards.size());
  }

}
