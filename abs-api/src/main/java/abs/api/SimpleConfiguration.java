package abs.api;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * A value object of {@link Configuration} while building a
 * {@link Context}.
 */
class SimpleConfiguration implements Configuration {

  private final Router envelopeRouter;
  private final Opener envelopeOpener;
  private final Inbox inbox;
  private final ReferenceFactory referenceFactory;
  private final ExecutorService executorService;
  private final ThreadFactory threadFactory;
  private final boolean isLoggingEnabled;
  private final String logPath;
  private final boolean isRemoteMessagingEnabled;

  /**
   * Ctor.
   * 
   * @param envelopeRouter
   * @param envelopeOpener
   * @param inbox
   * @param referenceFactory
   * @param executorService
   * @param threadFactory
   * @param isLoggingEnabled
   * @param logPath
   * @param isRemoteMessagingEnabled
   */
  public SimpleConfiguration(Router envelopeRouter, Opener envelopeOpener, Inbox inbox,
      ReferenceFactory referenceFactory, ExecutorService executorService,
      ThreadFactory threadFactory, final boolean isLoggingEnabled, String logPath,
      final boolean isRemoteMessagingEnabled) {
    this.envelopeRouter = envelopeRouter;
    this.envelopeOpener = envelopeOpener;
    this.inbox = inbox;
    this.referenceFactory = referenceFactory;
    this.executorService = executorService;
    this.threadFactory = threadFactory;
    this.isLoggingEnabled = isLoggingEnabled;
    this.logPath = logPath;
    this.isRemoteMessagingEnabled = isRemoteMessagingEnabled;
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
  public ReferenceFactory getReferenceFactory() {
    return referenceFactory;
  }

  @Override
  public ExecutorService getExecutorService() {
    return executorService;
  }

  @Override
  public ThreadFactory getThreadFactory() {
    return threadFactory;
  }

  @Override
  public boolean isLoggingEnabled() {
    return isLoggingEnabled;
  }

  @Override
  public String getLogPath() {
    return logPath;
  }

  @Override
  public boolean isRemoteMessagingEnabled() {
    return isRemoteMessagingEnabled;
  }

}
