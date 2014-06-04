package abs.api.event.sample;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import abs.api.Actor;
import abs.api.Behavior;

/**
 * @author Behrooz Nobakht
 */
public class Echo implements Actor, Behavior {

	private static final long serialVersionUID = 1L;

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Integer index;

	public Echo(Integer i) {
		this.index = i;
	}

	@Override
	public Object respond(Object message) {
		logger.error("echo#{} --- message: {} --- from: {}", index, message, sender());
		send(sender(), "an echo from " + index);
		return null;
	}

}
