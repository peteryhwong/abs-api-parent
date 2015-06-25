package abs.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicLong;

import abs.api.ContextInbox.InboxSweeperThread;
import abs.api.LoggingRouter.LoggingThread;

/**
 * A specialized {@link Thread} for {@link Context} that
 * {@link #yield()}s if an exception occurs during the run.
 * 
 * @author Behrooz Nobakht
 */
public final class ContextThread extends Thread {

  private static final Set<Class<? extends Thread>> INTERRUPTIBLE_THREADS =
      new HashSet<>(Arrays.asList(ContextThread.class, LoggingThread.class,
          InboxSweeperThread.class, ThreadInterruptWatchdog.class, ForkJoinWorkerThread.class));

  /**
   * Tries to {@link #interrupt()} all the live threads in the
   * runtime.
   */
  static void shutdown() {
    final Set<Thread> threads = Thread.getAllStackTraces().keySet();
    for (final Thread t : threads) {
      try {
        if (INTERRUPTIBLE_THREADS.contains(t.getClass())) {
          // Interrupt only the threads only by the context
          t.interrupt();
        }
      } catch (Throwable x) {
        // Ignore
      }
    }
  }

  private static final AtomicLong COUNTER = new AtomicLong(0);

  /**
   * Ctor
   * 
   * @param target the {@link Runnable} instance
   */
  public ContextThread(Runnable target) {
    super(target, createThreadName());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void run() {
    try {
      super.run();
    } catch (Throwable e) {
      // Ignore
    } finally {
      yield();
    }
  }

  private static String createThreadName() {
    return "abs-" + COUNTER.incrementAndGet();
  }

}
