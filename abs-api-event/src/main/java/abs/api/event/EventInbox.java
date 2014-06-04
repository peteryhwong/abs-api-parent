package abs.api.event;

import abs.api.AbstractInbox;
import abs.api.Envelope;
import abs.api.Opener;

/**
 * @author Behrooz Nobakht
 */
public class EventInbox extends AbstractInbox {

	private final Opener opener = new EventOpener();

	public EventInbox() {
	}

	@Override
	protected Opener opener(Envelope envelope, Object receiver) {
		return opener;
	}

}
