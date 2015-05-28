package abs.api;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A specialized {@link Thread} for {@link Context} that
 * {@link #yield()}s if an exception occurs during the run.
 * 
 * @author Behrooz Nobakht
 */
public final class ContextThread extends Thread {

  /**
   * Tries to {@link #interrupt()} all the live threads in the
   * runtime.
   */
  static void shutdown() {
    final Set<Thread> threads = Thread.getAllStackTraces().keySet();
    for (final Thread t : threads) {
      try {
        t.interrupt();
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
