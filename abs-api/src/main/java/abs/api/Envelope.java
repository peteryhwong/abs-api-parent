package abs.api;

import java.util.Objects;

/**
 * An abstraction to represent a message composing of a sender
 * reference, a recipient reference, the message, the eventual
 * result of the message as a future value and a unique
 * sequence.
 *
 * @author Behrooz Nobakht
 */
public interface Envelope {

  /**
   * Provides the sender of the envelope.
   *
   * @return the reference who sent the envelope
   */
  Reference from();

  /**
   * Provides the recipient of the envelope.
   *
   * @return the reference who should receive the envelope.
   */
  Reference to();

  /**
   * The message that is carried by the envelope. See
   * {@link abs.api.Actor} about the types of message that can
   * be sent.
   *
   * @return the message of this envelope
   */
  Object message();

  /**
   * Provides the response of the envelope as a future value.
   *
   * @return the response of the envelope as a future value.
   * @param <V> a T object.
   */
  <V> Response<V> response();

  /**
   * Provides a unique sequence that is assigned to this
   * envelope.
   *
   * @return the unique sequence of this envelope
   */
  long sequence();

  /**
   * Checks if this is a message from an actor to itself.
   * 
   * @return <code>true</code> if {@link #to()} and
   *         {@link #from()} at the same; otherwise
   *         <code>false</code>.
   */
  default boolean isSelfEnvelope() {
    return Objects.equals(from(), to());
  }

}
