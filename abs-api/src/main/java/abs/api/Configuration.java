package abs.api;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * An configuration specifies different ingredients of an instance of
 * {@link abs.api.Context} to be created.
 *
 * <p>
 * Note that all of the specified classes to be used for the creation of
 * the context are expected to have a <i>default</i> constructor.
 *
 * @author Behrooz Nobakht
 * @since 1.0
 */
public interface Configuration {

	/**
	 * Provides the router of the context.
	 *
	 * @return the {@link abs.api.Router} of the context
	 */
	Router getRouter();

	/**
	 * Provides the opener of the context
	 *
	 * @return the {@link abs.api.Opener} of the context
	 */
	Opener getOpener();

	/**
	 * Provides the inbox(es) of the context
	 *
	 * @return the {@link abs.api.Inbox} of the context
	 */
	Inbox getInbox();

	/**
	 * Provides the type of notary of the context
	 *
	 * @return the {@link java.lang.Class} of the {@link abs.api.Notary}
	 *         of the context
	 */
	Class<? extends Notary> getNotary();

	/**
	 * Provides the reference factory of the context.
	 * 
	 * @return the {@link ReferenceFactory} of the context
	 */
	ReferenceFactory getReferenceFactory();
	
	/**
	 * @return the {@link ExecutorService} of the context
	 */
	ExecutorService geExecutorService();
	
	/**
	 * @return the {@link ThreadFactory} of the context
	 */
	ThreadFactory geThreadFactory();

	/**
	 * Creates an instance of
	 * {@link abs.api.Configuration.ConfigurationBuilder} to build an
	 * instance of {@link abs.api.Configuration}.
	 *
	 * @return an instance of builder for a
	 *         {@link abs.api.Configuration}
	 */
	static ConfigurationBuilder newConfiguration() {
		return new ConfigurationBuilder();
	}

	/**
	 * A simple builder pattern for {@link Configuration}
	 */
	static class ConfigurationBuilder {

		private Router envelopeRouter = null;
		private Opener envelopeOpener = null;
		private Inbox inbox = new AsyncInbox();
		private Class<? extends Notary> notaryClass = LocalNotary.class;
		private ReferenceFactory referenceFactory = ReferenceFactory.DEFAULT;
		private ThreadFactory threadFactory = r -> new ContextThread(r);
		private ExecutorService executorService = Executors.newCachedThreadPool(threadFactory);

		ConfigurationBuilder() {
		}

		public ConfigurationBuilder withEnvelopeRouter(Router router) {
			this.envelopeRouter = router;
			return this;
		}

		public ConfigurationBuilder withEnvelopeOpener(Opener opener) {
			this.envelopeOpener = opener;
			return this;
		}

		public ConfigurationBuilder withInbox(Inbox inbox) {
			this.inbox = inbox;
			return this;
		}

		public ConfigurationBuilder withNotary(Class<? extends Notary> notary) {
			this.notaryClass = notary;
			return this;
		}

		public ConfigurationBuilder withReferenceFactory(ReferenceFactory referenceFactory) {
			this.referenceFactory = referenceFactory;
			return this;
		}
		
		public ConfigurationBuilder withExecutorService(ExecutorService executorService) {
		  this.executorService = executorService;
		  return this;
		}
		
		public ConfigurationBuilder withThreadFactory(ThreadFactory threadFactory) {
		  this.executorService = Executors.newCachedThreadPool(threadFactory);
		  this.threadFactory = threadFactory;
		  return this;
		}

		public Configuration build() {
			return new SimpleConfiguration(envelopeRouter, envelopeOpener, inbox, notaryClass,
					referenceFactory, executorService, threadFactory);
		}

		private static class SimpleConfiguration implements Configuration {

			private final Router envelopeRouter;
			private final Opener envelopeOpener;
			private final Inbox inbox;
			private final Class<? extends Notary> notaryClass;
			private final ReferenceFactory referenceFactory;
			private final ExecutorService executorService;
			private final ThreadFactory threadFactory;

			public SimpleConfiguration(Router envelopeRouter, Opener envelopeOpener,
					Inbox inbox, Class<? extends Notary> notaryClass,
					ReferenceFactory referenceFactory, ExecutorService executorService,
					ThreadFactory threadFactory) {
				this.envelopeRouter = envelopeRouter;
				this.envelopeOpener = envelopeOpener;
				this.inbox = inbox;
				this.notaryClass = notaryClass;
				this.referenceFactory = referenceFactory;
                this.executorService = executorService;
                this.threadFactory = threadFactory;
			}

			@Override
			public Router getRouter() {
				return this.envelopeRouter;
			}

			@Override
			public Opener getOpener() {
				return this.envelopeOpener;
			}

			@Override
			public Inbox getInbox() {
				return this.inbox;
			}

			@Override
			public Class<? extends Notary> getNotary() {
				return this.notaryClass;
			}

			@Override
			public ReferenceFactory getReferenceFactory() {
				return referenceFactory;
			}
			
			@Override
			public ExecutorService geExecutorService() {
			  return executorService;
			}
			
			@Override
			public ThreadFactory geThreadFactory() {
			  return threadFactory;
			}

		}

	}

}
