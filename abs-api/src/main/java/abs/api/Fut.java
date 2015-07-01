package abs.api;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * TODO An internal API to be documented
 * 
 * @author Behrooz Nobakht
 * @since 1.0
 */
class Fut extends CompletableFuture<Object> {

  private final boolean await;

  public Fut() {
    this(false);
  }

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

  protected synchronized void await() {
    if (!await) {
      return;
    }
    try {
      Object object = get();
      complete(object);
    } catch (InterruptedException | ExecutionException e) {
      completeExceptionally(e);
    }
  }

}
