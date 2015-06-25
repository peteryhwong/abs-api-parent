package abs.api;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An {@link Inbox} implementation and manages all the
 * {@link ObjectInbox} inside one {@link LocalContext}.
 */
class ContextInbox extends AbstractInbox {

  /**
   * We allow the receiver of an {@link Envelope} to be
   * <code>null</code>. Thus, the {@link ObjectInbox} for such
   * receiver is represented by this value.
   */
  private static final Object NULL_RECEIVER = new Object();

  /**
   * A dedicated that goes through all {@link ObjectInbox} in
   * the {@link Context} and executes the next {@link Envelope}
   * if there's any. The {@link #run()} delegates to
   * {@link ObjectInbox#run()}.
   */
  class InboxSweeperThread extends Thread {
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Collection<ObjectInbox> inboxes;

    /**
     * Ctor
     * 
     * @param inboxes the {@link Collection} of {@link Inbox}'s
     *        to sweep.
     */
    public InboxSweeperThread(Collection<ObjectInbox> inboxes) {
      super("inbox-sweeper");
      this.inboxes = inboxes;
    }

    @Override
    public void run() {
      if (!running.compareAndSet(false, true)) {
        return;
      }
      while (running.get()) {
        execute(inboxes, inboxes.size() > 1000);
      }
    }

    @Override
    public void interrupt() {
      running.getAndSet(true);
    }
  }

  private final ConcurrentMap<Object, ObjectInbox> inboxes = new ConcurrentHashMap<>();
  private final ExecutorService executor;

  /**
   * Ctor
   * 
   * @param executor the {@link ExecutorService}
   */
  public ContextInbox(ExecutorService executor) {
    this.executor = executor;
    inboxes.putIfAbsent(NULL_RECEIVER, new ObjectInbox(NULL_RECEIVER, executor));
  }

  protected void execute(Collection<ObjectInbox> inboxes, final boolean parallel) {
    if (parallel) {
      inboxes.parallelStream().forEach(oi -> executor.submit(oi));
    } else {
      inboxes.stream().forEach(oi -> executor.submit(oi));
    }
  }

  @Override
  public <V> Future<V> post(Envelope envelope, Object receiver) {
    inbox(receiver).post(envelope, receiver);
    return envelope.response();
  }

  protected ObjectInbox inbox(Object receiver) {
    if (receiver == null) {
      return inboxes.get(NULL_RECEIVER);
    }
    if (inboxes.containsKey(receiver)) {
      return inboxes.get(receiver);
    }
    inboxes.putIfAbsent(receiver, new ObjectInbox(receiver, executor));
    return inboxes.get(receiver);
  }

}
