package abs.api.event;

import reactor.core.Environment;
import reactor.core.Reactor;
import reactor.core.spec.Reactors;
import reactor.event.Event;
import reactor.event.selector.Selector;
import reactor.event.selector.Selectors;
import reactor.function.Consumer;
import abs.api.DefaultOpener;
import abs.api.Opener;

/**
 * An {@link Opener} that executes asynchronous messages using an
 * instance of {@link Reactor}.
 * 
 * @see Opener
 * @see Reactor
 * @see Event
 * @see Selector
 * @see Consumer
 * 
 * @author Behrooz Nobakht
 * @since 1.0
 */
public class EventOpener extends DefaultOpener {

	private static final String KEY = "abs-message";
	private static final Selector SELECTOR = Selectors.object(KEY);
	private static final Consumer<Event<Runnable>> CONSUMER = e -> e.getData().run();

	private final Reactor reactor;
	private final Environment env;

	public EventOpener() {
		env = new Environment();
		reactor = Reactors.reactor(env, Environment.RING_BUFFER);

		reactor.on(SELECTOR, CONSUMER);
	}

	@Override
	protected void executeEnvelopeTask(Runnable task) {
		reactor.notify(KEY, Event.wrap(task));
	}

}
