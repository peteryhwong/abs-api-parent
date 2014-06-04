package abs.api.event;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.junit.Test;

import reactor.core.Environment;
import reactor.core.Reactor;
import reactor.core.spec.Reactors;
import reactor.event.Event;
import reactor.event.selector.Selectors;
import reactor.function.Consumer;

/**
 * @author Behrooz Nobakht
 */
public class ReactorEventSequenceTest {

	static final int SIZE = 100000;

	static final Supplier<Long> SEQUENCER = new Supplier<Long>() {

		private final AtomicLong counter = new AtomicLong(0L);

		@Override
		public Long get() {
			return counter.incrementAndGet();
		}

	};

	static class Data {
		final long id = SEQUENCER.get();
		final String data = UUID.randomUUID().toString();
	}

	final LinkedBlockingQueue<Data> actualSequence = new LinkedBlockingQueue<>();

	class DataConsumer implements Consumer<Event<Data>> {

		@Override
		public void accept(Event<Data> t) {
			actualSequence.offer(t.getData());
		}

	}

	@Test
	public void testSequence() throws Exception {
		DataConsumer consumer = new DataConsumer();
		Environment env = new Environment();
		Reactor reactor = Reactors.reactor(env, Environment.RING_BUFFER);
		reactor.on(Selectors.$("data"), consumer);

		// Build data
		final BlockingDeque<Data> dq = new LinkedBlockingDeque<>();
		for (int i = 0; i < SIZE; ++i) {
			Data d = new Data();
			if (!dq.isEmpty() && d.id <= dq.peek().id) {
				throw new IllegalArgumentException("Sequence generation invalid.");
			}
			dq.offer(d);
		}

		// Dispatch events (agree, this can be improved using another
		// event consumer/executor service)
		for (int i = 0; i < 10; ++i) {
			new Thread(() -> {
				for (int j = 0; j < SIZE / 10; ++j) {
					if (!dq.isEmpty()) {
						reactor.notify("data", Event.wrap(dq.poll()));
					}
				}
			}).start();
		}

		while (actualSequence.size() < SIZE) {
			// forcibly wait until all data is consumed!
		}
		assertTrue(actualSequence.size() == SIZE);

		List<Data> list = new ArrayList<>(actualSequence);
		for (int i = 0; i < SIZE; ++i) {
			if (i < SIZE - 1) {
				long currentId = list.get(i).id;
				long nextId = list.get(i + 1).id;
				if (currentId >= nextId) {
					fail("Actual processed sequence failed: " + currentId + " >= " + nextId);
				}
			}
		}

	}
}
