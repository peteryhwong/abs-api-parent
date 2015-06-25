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
    implements Opener, Runnable, Supplier<Envelope>, Consumer<Envelope> {

  private final Object receiver;
  private final ExecutorService executor;
  private final BlockingQueue<Envelope> unprocessed = new LinkedBlockingQueue<>();
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
    if (processing.get() != null) {
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
  public void accept(Envelope envelope) {
    unprocessed.remove(envelope);
    processing.getAndSet(null);
  }

  protected EnveloperRunner createEnvelopeRunner(Envelope envelope) {
    return new EnveloperRunner(envelope, this::accept);
  }

}
