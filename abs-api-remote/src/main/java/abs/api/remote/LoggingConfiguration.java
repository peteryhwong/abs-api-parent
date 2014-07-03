package abs.api.remote;

import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;

/**
 * @author Behrooz Nobakht
 * @since 1.0
 */
public final class LoggingConfiguration {

	static {

		final Boolean enableDebugMode = Boolean.getBoolean("abs.debug");

		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();

		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.reset();

		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setContext(context);
		encoder.setPattern("[%d] [%5level] [%thread] %msg \\(%logger{0}:%L\\)%n%ex");
		encoder.start();

		ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
		appender.setContext(context);
		appender.setEncoder(encoder);
		appender.setName("Console");
		appender.start();

		Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		logger.setLevel(enableDebugMode ? Level.DEBUG : Level.INFO);
		logger.addAppender(appender);

		logger = (Logger) LoggerFactory.getLogger("org.eclipse.jetty");
		logger.setLevel(enableDebugMode ? Level.INFO : Level.ERROR);

		logger = (Logger) LoggerFactory.getLogger("org.glassfish.jersey");
		logger.setLevel(enableDebugMode ? Level.INFO : Level.ERROR);

		context.start();
	}

	private LoggingConfiguration() {
	}

	/**
	 * 
	 */
	public static void configure() {
	}

}
