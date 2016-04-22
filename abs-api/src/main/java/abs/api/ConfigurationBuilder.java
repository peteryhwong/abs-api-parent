package abs.api;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * A simple builder pattern for {@link Configuration}
 */
public final class ConfigurationBuilder {

  private boolean isThreadManagementEnabled =
      Boolean.parseBoolean(System.getProperty(Configuration.PROPERTY_THREAD_MANAGEMENT, "true"));
  private ThreadFactory threadFactory;
  private ExecutorService executorService;
  private Router envelopeRouter = new LocalRouter();
  private Opener envelopeOpener = new DefaultOpener();
  private Inbox inbox;
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

  public ConfigurationBuilder disableThreadManagement() {
    this.isThreadManagementEnabled = false;
    return this;
  }

  public final Configuration build() {
    if (threadFactory == null) {
      threadFactory = r -> new ContextThread(r, isThreadManagementEnabled);
      executorService = Executors.newCachedThreadPool(threadFactory);
    }
    if (inbox == null) {
      inbox = new ContextInbox(executorService, isThreadManagementEnabled);
    }
    return new SimpleConfiguration(envelopeRouter, envelopeOpener, inbox, referenceFactory,
        executorService, threadFactory, isLoggingEnabled, logPath, isRemoteEnabled,
        isThreadManagementEnabled);
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
