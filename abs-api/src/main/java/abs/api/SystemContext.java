package abs.api;

import java.util.concurrent.ForkJoinPool;

/**
 * A system context is an entry-point to an actor context. The default
 * constructor creates a context with default configuration.
 *
 * @author Behrooz Nobakht
 * @since 1.0
 */
public class SystemContext implements Context, Contextual {

	private static final Object MUTEX = new Object();
	private static Context context;

	public static Context context() {
		return context;
	}
	
	static {
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        if (context() != null) {
          try {
            context().stop();
            ForkJoinPool.commonPool().shutdown();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
      } , "context-shutdown"));
	}

	/**
	 * <p>
	 * Constructor for SystemContext.
	 * </p>
	 */
	public SystemContext() {
	}

	/** {@inheritDoc} */
	@Override
	public Actor newActor(String name, Object object) {
		return context.newActor(name, object);
	}

	/** {@inheritDoc} */
	@Override
	public Router router() {
		return context.router();
	}

	/** {@inheritDoc} */
	@Override
	public Notary notary() {
		return context.notary();
	}

	/** {@inheritDoc} */
	@Override
	public Inbox inbox(Reference reference) {
		return context.inbox(reference);
	}

	/** {@inheritDoc} */
	@Override
	public Opener opener(Reference reference) {
		return context.opener(reference);
	}
	
	/** {@inheritDoc} */
	@Override
	public void execute(Runnable command) {
	  context.execute(command);
	}

	/** {@inheritDoc} */
	@Override
	public void stop() throws Exception {
		context.stop();
	}

	@Override
	public void bind(Context context) {
		synchronized (MUTEX) {
			SystemContext.context = context;
		}
	}

}
