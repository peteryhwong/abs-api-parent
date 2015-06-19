package abs.api;

/**
 * Awaits until it receives an {@link #interrupt()} through
 * {@link InterruptedException} or {@link #isInterrupted()},
 * then shut downs {@link Context} through
 * {@link ContextThread#shutdown()}.
 * 
 * @author Behrooz Nobakht
 */
public class ThreadInterruptWatchdog extends Thread {

  private final Runnable interruptCallback;

  /**
   * Ctor
   * 
   * @param interruptCallback is executed upon the first receipt
   *        of {@link InterruptedException} or
   *        {@link #isInterrupted()} being {@code false}
   */
  public ThreadInterruptWatchdog(Runnable interruptCallback) {
    super("interrupt-watchdog-thread");
    this.interruptCallback = interruptCallback;
    start();
  }

  @Override
  public void run() {
    while (true) {
      if (isInterrupted()) {
        executeInteruptCallback();
        yield();
        return;
      } else {
        try {
          sleep(50);
        } catch (InterruptedException e) {
          executeInteruptCallback();
          yield();
          return;
        }
      }
    }
  }

  protected void executeInteruptCallback() {
    this.interruptCallback.run();
  }

}
