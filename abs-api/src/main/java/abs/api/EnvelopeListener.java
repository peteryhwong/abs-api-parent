package abs.api;

/**
 * An event listener for {@link Envelope}.
 */
public interface EnvelopeListener {

  /**
   * Provides a way to execute code before actually processing
   * the encapsulated message inside envelope.
   * 
   * @param envelope the envelope
   * @param context the current context
   */
  void onOpen(Envelope envelope, Context context);

  /**
   * Provides a way to execute code after finishing the
   * execution of the encapsulated message inside envelope.
   * 
   * @param envelope the envelope
   * @param context the current context
   */
  void onComplete(Envelope envelope, Context context);

}
