package abs.api;

/**
 * A {@link Runnable} wrapper over an {@link Envelope} whose
 * message is an instance of {@link Runnable}. No exceptions are
 * propagated.
 * 
 * @author Behrooz Nobakht
 */
class CompletableRunnableEnvelope implements Runnable {

  private final Envelope envelope;

  /**
   * Ctor
   * 
   * @param envelope the message {@link Envelope}.
   */
  public CompletableRunnableEnvelope(Envelope envelope) {
    this.envelope = envelope;
  }

  @Override
  public void run() {
    final Fut response = envelope.response();
    final Runnable runnable = (Runnable) envelope.message();
    try {
      runnable.run();
      response.complete(null);
    } catch (Throwable e) {
      response.completeExceptionally(e);
    }
  }
  
  /**
   * @return the envelope of the task
   */
  protected final Envelope envelope() {
    return envelope;
  }

}
