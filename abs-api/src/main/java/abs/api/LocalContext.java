package abs.api;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;

/**
 * A local context provides a default implementation of
 * {@link abs.api.Context} using a default {@link abs.api.Configuration}
 * or a provided configuration that utilizes
 * {@link java.util.ServiceLoader} to provision instances based on the
 * configuration classes.
 *
 * @see Context
 * @see Configuration
 * @author Behrooz Nobakht
 * @since 1.0
 */
public class LocalContext implements Context {

	private final SystemContext systemContext;
	private final Configuration configuration;
	private Router router;
	private Opener opener;
	private Inbox inbox;
	private Notary notary;
	private ExecutorService executor;
	private ReferenceFactory referenceFactory;

	/**
	 * <p>
	 * Constructor for LocalContext.
	 * </p>
	 */
	public LocalContext() {
		this(Configuration.newConfiguration().build());
	}

	/**
	 * <p>
	 * Constructor for LocalContext.
	 * </p>
	 *
	 * @param configuration
	 *            a {@link abs.api.Configuration} object.
	 */
	public LocalContext(Configuration configuration) {
		this.configuration = configuration;
		try {
			initialize();
			systemContext = new SystemContext();
			systemContext.bind(this);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/** {@inheritDoc} */
	@PostConstruct
	@Override
	public void initialize() throws Exception {
		this.executor = configuration.getExecutorService();
		
		Router messageRouter = configuration.getRouter();
		if (messageRouter == null) {
          throw new IllegalArgumentException("No " + Router.class + " is defined for this context");
		}
        LoggingRouter loggingRouter =
            new LoggingRouter(this.configuration.isLoggingEnabled(), this.configuration.getLogPath());
		this.router = new RouterCollection(messageRouter, loggingRouter);
		this.router.bind(this);

		this.opener = configuration.getOpener();
		if (this.opener == null) {
		  throw new IllegalArgumentException("No " + Opener.class + " is defined for this context");
		}

		this.inbox = configuration.getInbox();
		if (this.inbox == null) {
          throw new IllegalArgumentException("No " + Inbox.class + " is defined for this context");
		}
		this.inbox.bind(this);

		this.notary = new LocalNotary();
		this.referenceFactory = configuration.getReferenceFactory();
	}

	/** {@inheritDoc} */
	@Override
	public Actor newActor(String name, Object object) {
		try {
			final Reference reference = referenceFactory.create(name);
			final Actor ref = ContextActor.of(reference, this);
			notary.add(ref, object);
			return ref;
		} catch (RuntimeException e) {
			throw e;
		}
	}

	/** {@inheritDoc} */
	@Override
	public Notary notary() {
		return notary;
	}

	/** {@inheritDoc} */
	@Override
	public Router router() {
		return router;
	}

	/** {@inheritDoc} */
	@Override
	public Opener opener(Reference reference) {
		return opener;
	}

	/** {@inheritDoc} */
	@Override
	public Inbox inbox(Reference reference) {
		return inbox;
	}
	
	/** {@inheritDoc} */
	@Override
	public void execute(Runnable command) {
      try {
      // Semantics:
      // Every message to an object should be queued in the
      // order that it is received. Here, #get() ensures such
      // order of queueing.
        executor.submit(command).get();
      } catch (InterruptedException | ExecutionException e) {
        // Ignore: What can we do??!
      }
	}

	/** {@inheritDoc} */
	@Override
	public void stop() throws Exception {
		try {
			List<Runnable> tasks = executor.shutdownNow();
			for (Runnable task : tasks) {
              if (task instanceof EnveloperRunner) {
                EnveloperRunner er = (EnveloperRunner) task;
                Fut f = er.envelope().response();
                f.cancel(true);
              }
			}
			ContextThread.shutdown();
		} catch (Exception e) {
		  // Ignore
		}
	}

}
