package abs.api;

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
  private static final ObjectInbox NULL_RECEIVER_INBOX = new ObjectInbox(NULL_RECEIVER, null);

  /**
   * A dedicated that goes through all {@link ObjectInbox} in
   * the {@link Context} and executes the next {@link Envelope}
   * if there's any. The {@link #run()} delegates to
   * {@link ObjectInbox#run()}.
   */
  static class InboxSweeperThread extends Thread {
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Ctor
     * 
     * @param sweepRunnable the sweeping task
     */
    public InboxSweeperThread(Runnable sweepRunnable) {
      super(sweepRunnable, "inbox-sweeper");
      setDaemon(false);
      start();
    }

    @Override
    public void run() {
      if (!running.compareAndSet(false, true)) {
        return;
      }
      while (running.get()) {
        try {
          sleep(0, 1);
          super.run();
        } catch (Throwable e) {
        }
      }
    }

    @Override
    public void interrupt() {
      running.getAndSet(false);
      super.interrupt();
    }
  }

  private final ConcurrentMap<Object, ObjectInbox> inboxes = new ConcurrentHashMap<>();
  private final ExecutorService executor;
  private final InboxSweeperThread sweeper;

  /**
   * Ctor
   * 
   * @param executor the {@link ExecutorService}
   */
  public ContextInbox(ExecutorService executor) {
    this.executor = executor;
    this.inboxes.putIfAbsent(NULL_RECEIVER, NULL_RECEIVER_INBOX);
    this.sweeper = new InboxSweeperThread(this::execute);
  }

  @Override
  public <V> Future<V> post(Envelope envelope, Object receiver) {
    // queue the message to receiver
    ObjectInbox inbox = inbox(receiver);
    inbox.post(envelope, receiver);
    executeObjectInbox(inbox);

    // if an await message, free the sender
    if (envelope instanceof AwaitEnvelope) {
      Object sender = context.object(envelope.from());
      ObjectInbox senderInbox = inbox(sender);
      senderInbox.onAwaitStart(envelope, context);
      executeObjectInbox(senderInbox);
    }

    return envelope.response();
  }

  @Override
  public void bind(Context context) {
    super.bind(context);
    NULL_RECEIVER_INBOX.bind(context);
  }

  protected ObjectInbox inbox(Object receiver) {
    if (receiver == null || receiver == NULL_RECEIVER) {
      return NULL_RECEIVER_INBOX;
    }
    if (inboxes.containsKey(receiver)) {
      return inboxes.get(receiver);
    }
    inboxes.putIfAbsent(receiver, new ObjectInbox(receiver, executor));
    final ObjectInbox oi = inboxes.get(receiver);
    oi.bind(context);
    return oi;
  }

  protected void execute() {
    inboxes.values().stream().forEach(this::executeObjectInbox);
  }

  protected synchronized void executeObjectInbox(ObjectInbox oi) {
    try {
      if (oi == NULL_RECEIVER_INBOX) {
        return;
      }
      executor.submit(oi);
    } catch (Throwable e) {
      if (executor.isShutdown()) {
        sweeper.interrupt();
      }
    }
  }

}
