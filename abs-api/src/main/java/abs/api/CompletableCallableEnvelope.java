package abs.api;

import java.util.concurrent.Callable;

/**
 * A {@link Runnable} wrapper over an {@link Envelope} whose
 * message is an instance of {@link Callable}. In case of
 * failure, this runnable, fails the response and does
 * <i>not</i> propagate exceptions.
 * 
 * @author Behrooz Nobakht
 */
class CompletableCallableEnvelope implements Runnable {

  private final Envelope envelope;

  /**
   * Ctor
   * 
   * @param envelope the message {@link Envelope}
   */
  public CompletableCallableEnvelope(Envelope envelope) {
    this.envelope = envelope;
  }

  @Override
  public void run() {
    final Fut response = envelope.response();
    final Callable<?> callable = (Callable<?>) envelope.message();
    try {
      Object result = callable.call();
      response.complete(result);
    } catch (Throwable e) {
      response.completeExceptionally(e);
    }
  }

}
