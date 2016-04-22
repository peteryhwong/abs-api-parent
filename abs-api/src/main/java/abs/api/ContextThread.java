package abs.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import abs.api.ContextInbox.InboxSweeperThread;
import abs.api.LoggingRouter.LoggingThread;
import net.openhft.affinity.Affinity;

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
          // Interrupt only the thread types that we know we are
          // allowed.
          t.interrupt();
        }
      } catch (Throwable x) {
        // Ignore
      }
    }
  }

  private static final int CPUS = Runtime.getRuntime().availableProcessors();
  private static final AtomicLong COUNTER = new AtomicLong(1);
  private static final AtomicInteger CPU_AFFINITY = new AtomicInteger(1);

  /**
   * Ctor
   * 
   * @param target the {@link Runnable} instance
   * @param isThreadManagementEnabled
   */
  public ContextThread(Runnable target, boolean isThreadManagementEnabled) {
    super(target, createThreadName());
    if (isThreadManagementEnabled) {
      int cpu = CPU_AFFINITY.get();
      Affinity.setAffinity(cpu);
      CPU_AFFINITY.getAndSet((cpu + 1) % CPUS);
    }
    setDaemon(false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void run() {
    try {
      super.run();
    } finally {
      yield();
    }
  }

  private static String createThreadName() {
    return "jabs-" + COUNTER.incrementAndGet();
  }

}
