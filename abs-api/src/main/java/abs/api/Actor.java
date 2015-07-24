package abs.api;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * An actor is a reference that exposes a set of methods to send
 * messages to another actor. There are a number of ways that a message
 * would have meaning as an executable entity in this implementation.
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
		private static final String NAME = "NOBODY";

		private final URI name = URI.create(NS + NAME);

		@Override
		public URI name() {
			return name;
		}
		
		@Override
		public String simpleName() {
		  return NAME;
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
		
		@Override
		public Context context() {
		  return SystemContext.context();
		}
		
		@Override
		public Reference self() {
		  Reference ref = reference(this);
		  if (ref == null) {
		    context().newActor(NAME, this);
		  }
		  return reference(this);
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
    default <V> Response<V> send(Object to, Object message) {
      final Reference from = self();
      final Reference toRef = reference(to);
      final Envelope envelope = new SimpleEnvelope(from, toRef, message);
      context().execute(() -> context().router().route(envelope));
      return envelope.response();
    }
    
    /**
     * Sends a message to a reference with an additional property
     * that the sender of the message awaits on the response. The
     * usage of this method when necessary ensures that, for an
     * object, the object might be awaiting on an arbitrary number
     * of envelopes. However, it is ensured that only one message
     * at a time is processed inside the receiver object.
     * 
     * @see #await(Object, Object, Duration)
     * 
     * @param <V> the type of the future value of the response of
     *        the message
     * @param to the receiver of the message
     * @param message the message itself
     * @return the future value to capture the result of the
     *         message
     */
    default <V> Response<V> await(Object to, Object message) {
      return await(to, message, null);
    }
    
    /**
     * Semantics hold similar to that of
     * {@link #await(Object, Object)}. Additionally, await should
     * complete within the time boundaries specified by provided
     * deadline. If await fails with a timeout, then
     * {@link Response#getException()} holds the timeout
     * exception.
     * 
     * @param <V> the type of the future value of the response
     * @param to the receiver of the message
     * @param message the message itself
     * @param deadline the duration within which the response
     *        should complete
     * @return the response of the message
     */
    default <V> Response<V> await(Object to, Object message, Duration deadline) {
      final Reference from = self();
      final Reference toRef = reference(to);
      final Envelope envelope = new AwaitEnvelope(from, toRef, message);
      context().execute(() -> context().router().route(envelope));
      envelope.response().await(deadline);
      return envelope.response();
    }
    
    /**
     * Similar to {@link #await(Object, Object)} and different in
     * the sense that the await property holds on a "boolean"
     * expression encapsulated as an instance of {@link Supplier}.
     * Awaits continues until the supplier provides a
     * <code>true</code> value.
     * 
     * @param to the receiver of the message
     * @param condition the supplier of a boolean condition
     * @return the response of this await over a {@link Boolean}
     *         value
     */
    default Response<Boolean> await(Object to, Supplier<Boolean> condition) {
      final Predicate<Supplier<Boolean>> predicate = supplier -> {
        Boolean currentValue = supplier.get();
        if (currentValue != null && currentValue) {
          return true;
        }
        return await(to, condition).getValue();
      };
      final Callable<Boolean> message = () -> predicate.test(condition);
      final Reference from = self();
      final Reference toRef = reference(to);
      final Envelope envelope = new AwaitEnvelope(from, toRef, message);
      context().execute(() -> context().router().route(envelope));
      context().execute(() -> envelope.response().await(null));
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
    default <V> Response<V> reply(Object message) {
      Response<V> response = send(sender(), message);
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
