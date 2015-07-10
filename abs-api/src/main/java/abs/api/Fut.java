package abs.api;

import java.util.concurrent.CompletableFuture;

/**
 * An internal implementation of {@link Response} extending over
 * {@link CompletableFuture}.
 * 
 * @author Behrooz Nobakht
 * @since 1.0
 */
class Fut<V> extends CompletableFuture<V> implements Response<V> {

  private final boolean await;

  /**
   * Ctor.
   */
  public Fut() {
    this(false);
  }

  /**
   * Ctor.
   * 
   * @param await if this is a response for an
   *        {@link AwaitEnvelope}.
   */
  public Fut(boolean await) {
    this.await = await;
  }

  @Override
  public boolean isDone() {
    await();
    return super.isDone();
  }

  @Override
  public boolean isCancelled() {
    await();
    return super.isCancelled();
  }

  @Override
  public boolean isCompletedExceptionally() {
    await();
    return super.isCompletedExceptionally();
  }

  @Override
  public boolean isCompleted() {
    await();
    if (!isDone()) {
      return false;
    }
    try {
      get();
      return true;
    } catch (Throwable e) {
      return false;
    }
  }

  @Override
  public V getValue() {
    try {
      return get();
    } catch (Throwable e) {
      completeExceptionally(e);
      return null;
    }
  }

  @Override
  public <E extends Throwable> E getException() {
    if (!isCompletedExceptionally()) {
      return null;
    }
    try {
      get();
      throw new IllegalStateException("Should have completed exceptionally: " + this);
    } catch (Throwable e) {
      return e.getCause() == null ? (E) e : (E) e.getCause();
    }
  }

  /**
   * Await on this response if necessary
   * until it's done or it fails.
   */
  protected synchronized void await() {
    if (!await) {
      return;
    }
    try {
      V object = get();
      complete(object);
    } catch (Throwable e) {
      completeExceptionally(e);
    }
  }

}
