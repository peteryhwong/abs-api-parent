package abs.api;

import java.time.Instant;
import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A dedicated {@link Inbox} for a receiver of an
 * {@link Envelope}. This implementation is backed by using an
 * instance of {@link BlockingQueue} for the receiving
 * {@link Envelope}'s and an instance of {@link AtomicReference}
 * of {@link Envelope} for the one currently being processed at
 * a given time.
 * 
 * <p>
 * Note that either {@link #post(Envelope, Object)} or
 * {@link #open(Envelope, Object)} should be used from outside
 * this object. When {@link #run()} is used, an attempt is made
 * to retrieve the next envelope and run it. On every completion
 * of an {@link Envelope}, using {@link Consumer#accept(Object)}
 * clears the object's queue for the next envelope to be
 * executed.
 * 
 * @see ContextInbox
 */
class ObjectInbox extends AbstractInbox
    implements Opener, Runnable, Supplier<Envelope>, EnvelopeListener {

  /**
   * An executor to ensure the semantics of "run-to-completion"
   * for an object's message queue and avoid multiple
   * simultaneous message execution.
   */
  protected static class ObjectInboxExecutor implements Runnable {

    private final Object mutex = new Object();
    private final BlockingQueue<EnveloperRunner> queue =
        new PriorityBlockingQueue<>(512, ENVELOPE_RUNNER_COMPARATOR);
    private final ExecutorService executor;

    public ObjectInboxExecutor(ExecutorService executor) {
      this.executor = executor;
    }

    protected void enqueue(EnveloperRunner er) {
      queue.offer(er);
      run();
    }

    @Override
    public void run() {
      synchronized (mutex) {
        while (!queue.isEmpty()) {
          EnveloperRunner er = queue.poll();
          if (er == null) {
            continue;
          }
          execute(er);
        }
      }
    }

    protected void execute(EnveloperRunner er) {
      try {
        executor.submit(er).get();
      } catch (InterruptedException | ExecutionException e) {
        // Ignore
      }
    }

  }

  protected static final Comparator<Envelope> ENVELOPE_COMPARATOR =
      (e1, e2) -> Long.compare(e1.sequence(), e2.sequence());
  protected static final Comparator<EnveloperRunner> ENVELOPE_RUNNER_COMPARATOR =
      (er1, er2) -> ENVELOPE_COMPARATOR.compare(er1.envelope(), er2.envelope());
  private final Object receiver;
  private final ObjectInboxExecutor executor;
  private final BlockingQueue<Envelope> unprocessed =
      new PriorityBlockingQueue<>(512, ENVELOPE_COMPARATOR);
  private final BlockingQueue<Envelope> awaiting = new LinkedBlockingQueue<>();
  private final AtomicReference<Envelope> processing = new AtomicReference<>(null);
  private final AtomicBoolean sweeping = new AtomicBoolean(false);

  /**
   * Ctor
   * 
   * @param receiver the receiver object (owner of the envelope
   *        queue)
   * @param executor the {@link ExecutorService}
   */
  public ObjectInbox(Object receiver, ExecutorService executor) {
    this.receiver = receiver;
    this.executor = new ObjectInboxExecutor(executor);
  }

  @Override
  public <V> Future<V> post(Envelope envelope, Object receiver) {
    assert receiver == this.receiver : "Mismatch " + this.receiver + " : " + receiver;
    unprocessed.offer(envelope);
    return null;
  }

  @Override
  public <V> Future<V> open(Envelope envelope, Object target) {
    super.onOpen(envelope, this, target);
    executor.enqueue(createEnvelopeRunner(envelope));
    return envelope.response();
  }

  public void run() {
    if (isBusy() || !sweeping.compareAndSet(false, true)) {
      return;
    }
    for (Envelope envelope = get(); envelope != null; envelope = get()) {
      super.onOpen(envelope, this, receiver);
      EnveloperRunner runner = createEnvelopeRunner(envelope);
      runner.run();
//      System.out.println("RUN --- " + Thread.currentThread().getName() + " " + receiver + " "
//          + envelope.sequence() + " "
//          + unprocessed.stream().map(e -> Long.valueOf(e.sequence())).collect(Collectors.toList())
//          + " => " + envelope.response().getValue() + "   " + envelope.response());
    }
    sweeping.getAndSet(false);
  }

  @Override
  public Envelope get() {
    if (isBusy()) {
      return null;
    }
    final Envelope envelope = nextEnvelope(unprocessed);
    if (envelope == null) {
      return null;
    }
    if (!processing.compareAndSet(null, envelope)) {
      return null;
    }
    return envelope;
  }

  @Override
  public void onOpen(Envelope envelope, Context context) {
    this.unprocessed.remove(envelope);
    if (envelope instanceof AwaitEnvelope) {
      notifyStartAwait(envelope, context);
    }
  }

  @Override
  public void onComplete(Envelope envelope, Context context) {
    this.processing.getAndSet(null);
    if (envelope instanceof AwaitEnvelope) {
      notifyEndAwait(envelope, context);
    }
  }

  protected void onAwaitStart(Envelope envelope, Context context) {
    this.awaiting.offer(envelope);
    this.processing.getAndSet(null);
  }

  protected void onAwaitEnd(Envelope envelope, Context context) {
    this.awaiting.remove(envelope);
  }

  protected boolean isBusy() {
    return processing.get() != null;
  }

  protected boolean isAwaiting() {
    return !awaiting.isEmpty();
  }

  protected boolean isRunning() {
    return sweeping.get();
  }

  protected Envelope lastAwaitingEnvelope() {
    return this.awaiting.peek();
  }

  protected EnveloperRunner createEnvelopeRunner(Envelope envelope) {
    return new EnveloperRunner(envelope, context, this);
  }

  protected void notifyStartAwait(Envelope envelope, Context context) {
    // The sender (from) is going to await on this envelope.
    // This means that the sender's ObjectInbox should be
    // notified such that the sender would be able to execute
    // other messages while awaiting for this envelope.
    ObjectInbox senderObjectInbox = senderInbox(envelope, context);
    senderObjectInbox.onAwaitStart(envelope, context);
  }

  protected void notifyEndAwait(Envelope envelope, Context context) {
    // When the envelope is processed (success or failure), the
    // sender of the envelope should be notified again in order
    // to unblock the processing of the next messages for
    // sender.
    ObjectInbox senderObjectInbox = senderInbox(envelope, context);
    senderObjectInbox.onAwaitEnd(envelope, context);
  }

  protected ObjectInbox senderInbox(Envelope envelope, Context context) {
    ContextInbox inbox = (ContextInbox) context.inbox(envelope.from());
    // sender can be null (NOBODY)
    Object sender = envelope.from() == null ? null : context.object(envelope.from());
    ObjectInbox senderObjectInbox = inbox.inbox(sender);
    return senderObjectInbox;
  }

  protected Envelope nextEnvelope(BlockingQueue<Envelope> q) {
    if (isBusy() || q.isEmpty()) {
      return null;
    }
    Envelope env = q.peek();
    return env;
  }

  private void log(Object o) {
    System.err.println(String.format("%s %s %s %s", Instant.now().toString(),
        Thread.currentThread().getName(), receiver, o.toString()));
  }

}
