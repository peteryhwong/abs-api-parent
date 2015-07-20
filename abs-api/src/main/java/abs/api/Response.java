package abs.api;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

/**
 * An extension over {@link Future} and {@link CompletionStage}.
 * 
 * @param <V> the type of value encapsulated by the response
 * 
 * @see Future
 * @see CompletionStage
 * @see CompletableFuture
 * 
 * @author nobeh
 */
public interface Response<V> extends Future<V>, CompletionStage<V> {

  /**
   * Check if this response is complete with success.
   * 
   * @return <code>true</code> if the response is ready with
   *         success; or <code>false</code> could mean it's
   *         still not finished.
   */
  boolean isCompleted();

  /**
   * Check if this response is complete with an exception.
   * 
   * @return <code>true</code> if the response is ready with an
   *         exception; or <code>false</code> could mean it's
   *         still not finished.
   */
  boolean isCompletedExceptionally();

  /**
   * Complete the response with an exception.
   * 
   * @param t the exception for this response
   * @return if the completion with exception was done or not
   */
  boolean completeExceptionally(Throwable t);

  /**
   * Complete the response with a value.
   * 
   * @param value the value for this response
   * @return if te completion with the value was done or not
   */
  boolean complete(V value);

  /**
   * Equivalent to {@link #get()} with no exception.
   * 
   * @return the value or <code>null</code> if the response
   *         holds an exception
   */
  V getValue();

  /**
   * Potentially await until this response is ready. The await
   * can be specified by a deadline which is a {@link Duration}.
   * The deadline can be <code>null</code> and different
   * implementation might or might honor this deadline. If the
   * implementation honors the deadline, then after returning,
   * {@link #getValue()} should also return immediately.
   * 
   * @param deadline the maximum duration accepted to wait until
   *        this response is ready and {@link #getValue()} would
   *        return immediately
   */
  void await(Duration deadline);

  /**
   * Get the exception of this response.
   * 
   * @return the exception or <code>null</code> if the response
   *         holds a successful value
   */
  <E extends Throwable> E getException();

}
