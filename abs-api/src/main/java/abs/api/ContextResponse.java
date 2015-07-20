package abs.api;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An internal implementation of {@link Response} extending over
 * {@link CompletableFuture}.
 * 
 * @author Behrooz Nobakht
 * @since 1.0
 */
class ContextResponse<V> extends CompletableFuture<V>implements Response<V> {

  private final boolean await;

  /**
   * Ctor.
   */
  public ContextResponse() {
    this(false);
  }

  /**
   * Ctor.
   * 
   * @param await if this is a response for an
   *        {@link AwaitEnvelope}.
   */
  public ContextResponse(boolean await) {
    this.await = await;
  }

  @Override
  public boolean isDone() {
    return super.isDone();
  }

  @Override
  public boolean isCancelled() {
    return super.isCancelled();
  }

  @Override
  public boolean isCompletedExceptionally() {
    return super.isCompletedExceptionally();
  }

  @Override
  public boolean isCompleted() {
    return isDone() && !isCompletedExceptionally();
  }

  @Override
  public V get() throws InterruptedException, ExecutionException {
    try {
      await();
      V v = super.get();
      complete(v);
      return v;
    } catch (InterruptedException e) {
      completeExceptionally(e);
      throw e;
    } catch (ExecutionException e) {
      completeExceptionally(e);
      throw e;
    }
  }

  @Override
  public V getValue() {
    try {
      V v = get();
      return v;
    } catch (Throwable e) {
      return null;
    }
  }

  @Override
  public <E extends Throwable> E getException() {
    if (!isCompletedExceptionally()) {
      return null;
    }
    try {
      await();
      throw new IllegalStateException("Should have completed exceptionally: " + this);
    } catch (Throwable e) {
      return e.getCause() == null ? (E) e : (E) e.getCause();
    }
  }

  @Override
  public void await(Duration deadline) {
    if (!await) {
      return;
    }
    if (Functional.isDurationInfinite(deadline)) {
      await();
      return;
    }
    try {
      V object = super.get(deadline.toMillis(), TimeUnit.MILLISECONDS);
      complete(object);
    } catch (Throwable e) {
      doCompleteExceptionally(e);
    }
  }

  /**
   * Await on this response if necessary until it's done or it
   * fails.
   */
  protected synchronized final void await() {
    if (!await) {
      return;
    }
    try {
      V object = super.get();
      complete(object);
    } catch (Throwable e) {
      doCompleteExceptionally(e);
    }
  }

  protected void doCompleteExceptionally(Throwable t) {
    if (t instanceof TimeoutException) {
      completeExceptionally(t);
      return;
    }
    Throwable throwable = t;
    Throwable cause;
    while ((cause = throwable.getCause()) != null) {
      throwable = cause;
    }
    completeExceptionally(throwable);
  }

}
