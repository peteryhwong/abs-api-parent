package abs.api;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * An configuration specifies different ingredients of an
 * instance of {@link abs.api.Context} to be created.
 *
 * <p>
 * Note that all of the specified classes to be used for the
 * creation of the context are expected to have a <i>default</i>
 * constructor.
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
   * Provides the reference factory of the context.
   * 
   * @return the {@link ReferenceFactory} of the context
   */
  ReferenceFactory getReferenceFactory();

  /**
   * The executor service.
   * 
   * @return the {@link ExecutorService} of the context
   */
  ExecutorService getExecutorService();

  /**
   * The thread factory.
   * 
   * @return the {@link ThreadFactory} of the context
   */
  ThreadFactory getThreadFactory();

  /**
   * Is logging enabled?
   * 
   * @return if the logging of {@link Actor} messages is
   *         enabled.
   */
  boolean isLoggingEnabled();

  /**
   * Get log path.
   * 
   * @see #isLoggingEnabled()
   * @return the full path of the log file for {@link Actor}
   *         messages.
   */
  String getLogPath();

  /**
   * Is remote messaging enabled?
   * 
   * @return <code>true</code> if building context for a remote
   *         setup; otherwise <code>false</code> for a local
   *         context.
   */
  boolean isRemoteMessagingEnabled();

  /**
   * Creates an instance of
   * {@link abs.api.Configuration.ConfigurationBuilder} to build
   * an instance of {@link abs.api.Configuration}.
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

    private ThreadFactory threadFactory = r -> new ContextThread(r);
    private ExecutorService executorService = Executors.newCachedThreadPool(threadFactory);
    private Router envelopeRouter = new LocalRouter();
    private Opener envelopeOpener = new DefaultOpener();
    private Inbox inbox = new ContextInbox(executorService);
    private ReferenceFactory referenceFactory = ReferenceFactory.DEFAULT;
    private boolean isLoggingEnabled = false;
    private String logPath = LoggingRouter.DEFAULT_LOG_PATH;
    private boolean isRemoteEnabled = false;

    ConfigurationBuilder() {}

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

    public ConfigurationBuilder enableLogging() {
      this.isLoggingEnabled = true;
      return this;
    }

    public ConfigurationBuilder setLogPath(String logPath) {
      this.logPath = logPath;
      return this;
    }

    public ConfigurationBuilder enableRemoteMessaging() {
      this.isRemoteEnabled = true;
      return this;
    }

    public final Configuration build() {
      return new SimpleConfiguration(envelopeRouter, envelopeOpener, inbox, referenceFactory,
          executorService, threadFactory, isLoggingEnabled, logPath, isRemoteEnabled);
    }

    /**
     * Build a {@link LocalContext}.
     * 
     * @return the {@link LocalContext} for this configuration
     */
    public Context buildContext() {
      return new LocalContext(build());
    }

  }

}
