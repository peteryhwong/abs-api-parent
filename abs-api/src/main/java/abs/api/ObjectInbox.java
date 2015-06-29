package abs.api;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
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

  private final Object receiver;
  private final ExecutorService executor;
  private final BlockingQueue<Envelope> unprocessed = new LinkedBlockingQueue<>();
  private final BlockingQueue<Envelope> awaiting = new LinkedBlockingQueue<>();
  private final AtomicReference<Envelope> processing = new AtomicReference<>(null);

  /**
   * Ctor
   * 
   * @param receiver the receiver object (owner of the envelope
   *        queue)
   * @param executor the {@link ExecutorService}
   */
  public ObjectInbox(Object receiver, ExecutorService executor) {
    this.receiver = receiver;
    this.executor = executor;
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
    executor.submit(createEnvelopeRunner(envelope));
    return envelope.response();
  }

  public void run() {
    Envelope envelope = get();
    if (envelope != null) {
      open(envelope, receiver);
    }
  }

  @Override
  public Envelope get() {
    if (isBusy()) {
      return null;
    }
    final Envelope envelope = unprocessed.peek();
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
    if (envelope instanceof AwaitEnvelope) {
      notifyStartAwait(envelope, context);
    }
  }

  @Override
  public void onComplete(Envelope envelope, Context context) {
    this.unprocessed.remove(envelope);
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
    Object sender = context.object(envelope.from());
    ObjectInbox senderObjectInbox = inbox.inbox(sender);
    return senderObjectInbox;
  }

}
