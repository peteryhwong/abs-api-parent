package abs.api;

/**
 * A simple implementation of {@link Envelope}.
 * 
 * @author Behrooz Nobakht
 * @since 1.0
 */
public class SimpleEnvelope implements Envelope {

	private static final Sequencer SEQUENCER = Sequencer.of(0L);

	private final Reference sender;
	private final Reference receiver;
	private final Object message;
	private final Response<Object> future;
	private final long sequence;

	/**
	 * <p>
	 * Constructor for SimpleEnvelope.
	 * </p>
	 *
	 * @param sender
	 *            a {@link abs.api.Reference} object.
	 * @param receiver
	 *            a {@link abs.api.Reference} object.
	 * @param message
	 *            a {@link java.lang.Object} object.
	 */
	public SimpleEnvelope(Reference sender, Reference receiver, Object message) {
		this.sender = sender;
		this.receiver = receiver;
		this.message = message;
		this.future = createResponse();
		this.sequence = SEQUENCER.get();
	}

  /** {@inheritDoc} */
	@Override
	public Reference from() {
		return sender;
	}

	/** {@inheritDoc} */
	@Override
	public Reference to() {
		return receiver;
	}

	/** {@inheritDoc} */
	@Override
	public Object message() {
		return message;
	}

	/** {@inheritDoc} */
	@Override
	public <V> Response<V> response() {
		return (Response<V>) future;
	}

	/** {@inheritDoc} */
	@Override
	public long sequence() {
		return sequence;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
      StringBuilder sb = new StringBuilder("Envelope(");
      sb.append("id: ").append(sequence).append(", ");
      sb.append("from: ").append(sender).append(", ");
      sb.append("to: ").append(receiver).append(", ");
      sb.append("message: ").append(messageString(message)).append(", ");
      sb.append("future: ").append(future);
      return sb.append(")").toString();
	}

    protected <V> Response<V> createResponse() {
      return new ContextResponse<V>();
    }
    
    protected String messageString(Object message) {
      String simpleName = message.getClass().getSimpleName();
      if (simpleName.contains("Lambda")) {
        return simpleName;
      }
      return simpleName + "@" + Integer.toHexString(message.hashCode());
    }
}
