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

  private final Envelope envelope;
  private final Context context;
  private final EnvelopeListener envelopeListener;

  /**
   * Ctor
   * 
   * @see #EnveloperRunner(Envelope, Consumer)
   * @param envelope the {@link Envelope}
   */
  public EnveloperRunner(Envelope envelope) {
    this(envelope, null, null);
  }

  /**
   * Ctor
   * 
   * @param envelope the {@link Envelope}
   * @param context the current {@link Context}
   * @param envelopeListener an {@link EnvelopeListener} to be
   *        notified of different stages of running an envelope.
   */
  public EnveloperRunner(Envelope envelope, Context context, EnvelopeListener envelopeListener) {
    this.envelope = envelope;
    this.context = context;
    this.envelopeListener = envelopeListener;
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
      onOpen();
      Object result = ((Callable<?>) msg).call();
      response.complete(result);
    } catch (Throwable e) {
      response.completeExceptionally(e);
    } finally {
      onComplete();
    }
  }

  private void executeRunnableEnvelope(final Object msg, final Fut response) {
    try {
      onOpen();
      ((Runnable) msg).run();
      response.complete(null);
    } catch (Throwable e) {
      response.completeExceptionally(e);
    } finally {
      onComplete();
    }
  }

  private void onComplete() {
    if (envelopeListener != null) {
      envelopeListener.onComplete(envelope, context);
    }
  }

  private void onOpen() {
    if (envelopeListener != null) {
      envelopeListener.onOpen(envelope, context);
    }
  }

}
