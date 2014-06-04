package abs.api.event;

import abs.api.Configuration;
import abs.api.LocalContext;

/**
 * @author Behrooz Nobakht
 * @since 1.0
 */
public class EventContext extends LocalContext {

	public EventContext() {
		super(Configuration.newConfiguration().withEnvelopeOpener(new EventOpener())
				.withInbox(new EventInbox()).build());
	}

}
