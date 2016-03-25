package abs.api;

import java.util.Comparator;
import java.util.Deque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

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

  protected static final Comparator<Envelope> ENVELOPE_COMPARATOR =
      (e1, e2) -> Long.compare(e1.sequence(), e2.sequence());
  protected static final Comparator<EnveloperRunner> ENVELOPE_RUNNER_COMPARATOR =
      (er1, er2) -> ENVELOPE_COMPARATOR.compare(er1.envelope(), er2.envelope());

  private final Object receiver;
  private final BlockingQueue<Envelope> unprocessed =
      new PriorityBlockingQueue<>(512, ENVELOPE_COMPARATOR);

  // Current non-self non-await message from another actor
  private final AtomicReference<Envelope> current = new AtomicReference<>(null);
  // Queue of await messages to self/others
  private final Deque<Envelope> awq = new ConcurrentLinkedDeque<>();
  // A lock to prevent multiple executions at the same over the queue
  private final AtomicBoolean executing = new AtomicBoolean(false);

  /**
   * Ctor
   * 
   * @param receiver the receiver object (owner of the envelope
   *        queue)
   * @param executor the {@link ExecutorService}
   */
  public ObjectInbox(Object receiver, ExecutorService executor) {
    this.receiver = receiver;
  }

  @Override
  public <V> Future<V> post(Envelope envelope, Object receiver) {
    assert receiver == this.receiver : "Mismatch " + this.receiver + " : " + receiver;
    unprocessed.offer(envelope);
    return envelope.response();
  }

  @Override
  public <V> Future<V> open(Envelope envelope, Object target) {
    return envelope.response();
  }

  public void run() {
    if (!executing.compareAndSet(false, true)) {
      return;
    }
    for (Envelope envelope = get(); envelope != null; envelope = get()) {
      super.onOpen(envelope, this, receiver);
      EnveloperRunner runner = createEnvelopeRunner(envelope);
      runner.run();
    }
    executing.getAndSet(false);
    Thread.yield();
  }

  @Override
  public Envelope get() {
    final Envelope e = nextEnvelope(unprocessed);
    if (e == null) {
      return null;
    }
    if (isProcessingEnvelope()) {
      return null;
    }
    this.current.getAndSet(e);
    return e;
  }

  @Override
  public void onOpen(Envelope envelope, Context context) {
    this.unprocessed.remove(envelope);
    // if (isAwaitEnvelope(envelope)) {
    // notifyStartAwait(envelope, context);
    // }
  }

  @Override
  public void onComplete(Envelope envelope, Context context) {
    if (isAwaitEnvelope(envelope)) {
      notifyEndAwait(envelope, context);
    }
    if (isNormalEnvelope(envelope)) {
      this.current.getAndSet(null);
    }
  }

  @Override
  public String toString() {
    Reference ref = context.reference(receiver);
    boolean busy = isProcessingEnvelope();
    int size = unprocessed.size();
    return "ObjectInbox[owner=" + ref + ",busy=" + busy + ",queue=" + size + "]";
  }

  protected void onAwaitStart(Envelope envelope, Context context) {
    this.awq.push(envelope);
    this.current.getAndSet(null);
  }

  protected void onAwaitEnd(Envelope envelope, Context context) {
    this.awq.remove(envelope);
  }

  protected boolean isProcessingEnvelope() {
    Envelope e = current.get();
    return e != null && isNormalEnvelope(e);
  }

  protected boolean isAwaiting() {
    return this.awq.isEmpty() == false;
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
    if (q.isEmpty()) {
      return null;
    }
    Envelope env = q.peek();
    return env;
  }

  private boolean isNormalEnvelope(Envelope e) {
    return e instanceof AwaitEnvelope == false && e.isSelfEnvelope() == false;
  }

  private boolean isAwaitEnvelope(Envelope e) {
    return e instanceof AwaitEnvelope == true;
  }

}
