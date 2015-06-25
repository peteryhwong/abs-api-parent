package abs.api;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * A {@link Runnable} to execute an instance of {@link Envelope}
 * . The runner expects either a {@link Callable} or a
 * {@link Runnable} from {@link Envelope#message()}. Execution
 * of the message is contained and no exception is propagated.
 */
class EnveloperRunner implements Runnable {

  protected final Envelope envelope;
  private final Consumer<Envelope> completionConsumer;

  /**
   * Ctor
   * 
   * @see #EnveloperRunner(Envelope, Consumer)
   * @param envelope the {@link Envelope}
   */
  public EnveloperRunner(Envelope envelope) {
    this(envelope, null);
  }

  /**
   * Ctor
   * 
   * @param envelope the {@link Envelope}
   * @param completionConsumer the {@link Consumer} to apply
   *        when the {@link Envelope} processing is complete
   *        with <i>success</i> (i.e. no exception)
   */
  public EnveloperRunner(Envelope envelope, Consumer<Envelope> completionConsumer) {
    this.envelope = envelope;
    this.completionConsumer = completionConsumer;
  }

  @Override
  public final void run() {
    final Object msg = envelope.message();
    final Fut response = envelope.response();
    executeMessage(msg, response);
  }

  protected void executeMessage(final Object msg, final Fut response) {
    if (msg instanceof Runnable) {
      executeRunnableEnvelope(msg, response);
      return;
    }
    if (msg instanceof Callable) {
      executeCallableMessage(msg, response);
      return;
    }
    throw new IllegalArgumentException("Unknown executable envelope type: " + msg);
  }

  /**
   * @return the envelope of the task
   */
  protected final Envelope envelope() {
    return envelope;
  }

  private void executeCallableMessage(final Object msg, final Fut response) {
    try {
      Object result = ((Callable<?>) msg).call();
      response.complete(result);
      onComplete();
    } catch (Throwable e) {
      response.completeExceptionally(e);
    }
  }

  private void executeRunnableEnvelope(final Object msg, final Fut response) {
    try {
      ((Runnable) msg).run();
      response.complete(null);
      onComplete();
    } catch (Throwable e) {
      response.completeExceptionally(e);
    }
  }

  private void onComplete() {
    if (completionConsumer != null) {
      completionConsumer.accept(envelope);
    }
  }

}
