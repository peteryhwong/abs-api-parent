package abs.api;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * An implementation of {@link Inbox} that uses a dedicated
 * thread to await on the next message for <i>any</i> object and
 * then completes its execution and move on to the next.
 * 
 * @author Behrooz Nobakht
 */
class ThreadInbox extends AbstractInbox {

  private static class TargettedEnvelope {
    private final Envelope envelope;
    private final Object target;

    TargettedEnvelope(Envelope envelope, Object target) {
      this.envelope = envelope;
      this.target = target;
    }
  }

  class SelectorThread extends Thread {
    public SelectorThread() {
      super("message-queue");
    }

    @Override
    public void run() {
      while (true) {
        try {
          TargettedEnvelope te = messages.take();
          final Envelope envelope = te.envelope;
          final Object target = te.target;
          doOpen(envelope, target);
          final Future<?> f = envelope.response();
          f.get();
        } catch (ExecutionException e) {
          // The future already has failed and the caller will
          // get the exception. Proceed to the next.
        } catch (InterruptedException e) {
          // We need to know "who" is the interrupter!
          try {
            context.stop();
            return;
          } catch (Exception stoppingException) {
            // We cannot do anything at this moment.
            return;
          }
        }
      }
    }
  }

  private final BlockingQueue<TargettedEnvelope> messages;
  private final ExecutorService executor;

  /**
   * Ctor
   * 
   * @param executor the executor service for the messages
   */
  public ThreadInbox(ExecutorService executor) {
    this.executor = executor;
    this.messages = new LinkedBlockingQueue<>();
  }

  @Override
  protected void open(Opener opener, Envelope envelope, Object receiver) {
    messages.offer(new TargettedEnvelope(envelope, receiver));
  }

  protected void doOpen(final Envelope envelope, final Object target) {
    final Opener opener = opener(envelope, target);
    executor.submit(() -> opener.open(envelope, target));
  }
}
