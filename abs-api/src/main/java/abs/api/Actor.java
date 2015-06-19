package abs.api;

import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * An actor is a reference that exposes a set of methods to send
 * messages to another actor. There are a number of ways that a message
 * would have meaning as an executable entity in this implementation:
 * <ul>
 * <li>an instance of {@link Runnable} or {@link Callable} exposed by
 * {@link #ask(Object, Object)}
 * <li>the recipient of the message is an instance of {@link Behavior}
 * which leads to running {@link Behavior#respond(Object)}
 * </ul>
 * 
 * <p>
 * Every actor is registered with an instance of {@link Context}. A
 * gathers different layers of the actor system to be used by any actor
 * such as routing or executing messages.
 * 
 * <p>
 * This interface in this version exposes methods as {@code ask} which
 * allows to capture the result of the message into a future value.
 * However, to have the model of {@code tell} in actor (fire and
 * forget), simply the result of the message can be ignored.
 * 
 * @see Reference
 * @see MethodReference
 * 
 * @author Behrooz Nobakht
 * @since 1.0
 */
public interface Actor extends Reference, Comparable<Reference> {

	/**
	 * The prefix for all actors created in a context.
	 */
	String NS = "abs://";

	/**
	 * NOBODY refers to any recipient that is not recognized by its
	 * {@link #name()} in the system.
	 */
	Actor NOBODY = new Actor() {
		private static final long serialVersionUID = 6203481166223651274L;

		private final URI name = URI.create(NS + "NOBODY");

		@Override
		public URI name() {
			return name;
		}
		
		@Override
		public String simpleName() {
		  return "NOBODY";
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (obj == this) {
				return true;
			}
			if (obj instanceof Reference == false) {
				return false;
			}
			return name.equals(((Reference) obj).name());
		};

		@Override
		public String toString() {
			return name.toASCIIString();
		}
	};

	/**
	 * A default implementation of {@link Reference#name()} that uses
	 * {@link #NOBODY}.
	 * 
	 * @implSpec the default implementation returns {@link NOBODY}'s
	 *           name
	 * @return the name of this actor
	 */
	@Override
	default URI name() {
		return NOBODY.name();
	}

    /**
     * A default implementation of {@link Reference#simpleName()}
     * that uses {@link #NOBODY}.
     * 
     * @return the simple name of this actor
     */
	@Override
	default String simpleName() {
	  return NOBODY.simpleName();
	}

	/**
	 * Provides the context of this actor. By default, it uses the
	 * context from {@link SystemContext} which is expected to be
	 * initialized in the beginning of an application using this API.
	 * 
	 * @see SystemContext
	 * @return the context to which this actor is registered with.
	 */
	default Context context() {
		return SystemContext.context();
	}

    /**
     * Sends a general message to a recipient and captures the
     * result into an instance of {@link Future}.
     * 
     * @see Context
     * @see Router
     * 
     * @param <V> the type of the result expected from the future
     *        value
     * @param to the receiver of the message
     * @param message the message to be sent to the receiver
     * @return a future value to capture the result of processing
     *         the message. The future value may throw exception
     *         is {@link Future#get()} is used as a result of
     *         either failure in processing the message or
     *         actually the processing of the message decided to
     *         fail the message result. The user of the future
     *         value may inspect into causes of the exception to
     *         identify the reasons.
     * @throws Exception if the response {@link Future}
     *         fails. In this case, the cause is wrapped inside
     *         the thrown exception.
     */
    default <V> V ask(Object to, Object message) throws Exception {
      final Future<V> response = send(to, message);
      return response.get();
    }

    /**
     * Sends a message to a reference.
     *
     * @param <V> the type of the future value of the response of
     *        the message
     * @param to the receiver of the message that can be either
     *        the {@link Reference} to the receiver or the object
     *        itself
     * @param message the message itself
     * @return the future value to capture the result of the
     *         message
     */
    default <V> Future<V> send(Object to, Object message) {
      final Reference from = self();
      final Reference toRef = reference(to);
      final Envelope envelope = new SimpleEnvelope(from, toRef, message);
      context().execute(() -> context().router().route(envelope));
      return envelope.response();
    }
    
    /**
     * Replies to the {@link #sender()} of this message with
     * another message.
     * 
     * @param message the reply message
     * @param <V> The expected type of the response
     * @return the response of the reply message
     */
    default <V> Future<V> reply(Object message) {
      Future<V> response = send(sender(), message);
      return response;
    }

	/**
	 * Provides access to the reference registered for this actor
	 * object.
	 * 
	 * @return the reference of this object
	 */
	default Reference self() {
		if (this instanceof ContextActor) {
			return this;
		}
		return reference(this);
	}

	/**
	 * Provides the sender of the <i>current</i> message that is being
	 * invoked/processed by the receiver object.
	 * 
	 * @see Context
	 * @see ContextActor
	 * @see EnvelopeContext
	 * 
	 * @return the sender of the current message or {@link #NOBODY} if
	 *         there is no sender for this message
	 */
	default Reference senderReference() {
		try {
			final Reference ref = self();
			if (ref instanceof ContextActor) {
				ContextActor caref = (ContextActor) ref;
				Context context = caref.context();
				if (context != null || context instanceof EnvelopeContext) {
					return ((EnvelopeContext) context).sender();
				}
			}
			if (!NOBODY.equals(this)) {
				Context context = context();
				if (context != null && context instanceof EnvelopeContext) {
					return ((EnvelopeContext) context).sender();
				}
			}
		} catch (Exception e) {
			// Ignore
		}
		return NOBODY;
	}
	
    /**
     * Provides access to the sender object of the current
     * message.
     * 
     * @see #senderReference()
     * @param <T> The expected type of the sender object
     * @return the sender object of the current processes message
     *         with expected type <code>T</code>
     */
    default <T> T sender() {
      return object(senderReference());
    }
	
	/**
	 * Delegates to {@link Context#object(Reference)}.
	 * 
	 * @see Context#object(Reference)
	 * 
	 * @param <T>
	 *            the expected type of the object
	 * @param reference
	 *            the reference of the object
	 * @return See {@link Context#object(Reference)}
	 */
	default <T> T object(Reference reference) {
		return context().object(reference);
	}
	
	/**
	 * Delegates to {@link Context#reference(Object)}.
	 * 
	 * @see Context#reference(Object)
	 * 
	 * @param object
	 *            the object to find the reference for
	 * @return the reference of the object or {@code null}
	 */
	default Reference reference(Object object) {
		return context().reference(object);
	}

	/**
	 * The implementation is not different from
	 * {@link Reference#compareTo(Reference)}.
	 * 
	 * @param o
	 *            the reference to compare to
	 * @return the default semantics specified by {@link Comparable}
	 */
	@Override
	default int compareTo(Reference o) {
		return name().compareTo(o.name());
	}

}
