package abs.api;

import java.util.concurrent.ExecutorService;
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
   * Does the configuration support optimized thread management?
   * 
   * @return
   */
  boolean isThreadManagementEnabled();

  /**
   * Creates an instance of {@link abs.api.ConfigurationBuilder}
   * to build an instance of {@link abs.api.Configuration}.
   *
   * @return an instance of builder for a
   *         {@link abs.api.Configuration}
   */
  static ConfigurationBuilder newConfiguration() {
    return new ConfigurationBuilder();
  }

}
