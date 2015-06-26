package abs.api;

/**
 * An extension of {@link SimpleEnvelope} to present a message
 * for which the sender object is awaiting for the response
 * before proceeding its execution. An object might be awaiting
 * an arbitrary number of envelopes at a given time. An await
 * envelope does not change any behavior and it is mostly a
 * marker extension.
 */
class AwaitEnvelope extends SimpleEnvelope {

  /**
   * Ctor
   * 
   * @param sender the sender {@link Reference}
   * @param receiver the receiver {@link Reference}
   * @param message the message to the receiver
   */
  public AwaitEnvelope(Reference sender, Reference receiver, Object message) {
    super(sender, receiver, message);
  }

}
