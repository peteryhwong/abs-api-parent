package abs.api.event.sample;

import abs.api.Actor;
import abs.api.event.EventContext;

/**
 * @author Behrooz Nobakht
 */
public class Main {

	private EventContext context = new EventContext();
	private Echo e1;
	private Echo e2;
	private Actor a1;
	private Actor a2;

	public Main() {
		e1 = new Echo(1);
		e2 = new Echo(2);

		a1 = context.newActor("e1", e1);
		a2 = context.newActor("e2", e2);

	}

	public void start() {
		a1.ask(a2, "a random message");
	}

}
