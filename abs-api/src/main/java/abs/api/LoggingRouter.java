package abs.api;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * A logging {@link Router} implementation that creates a log of
 * actor messages in the format
 * 
 * <pre>
 * TIME,FROM,TO,MESSAGE_ID
 * </pre>
 * 
 * in which <code>MESSAGE_ID</code> is the object hash code of
 * the message passed.
 * <p>
 * The logging can be enabled by system property
 * {@link #JABS_LOGGING_ENABLED} and the path of the log file
 * can be configured via {@link #JABS_LOGGING_PATH}. By the
 * default, if enabled and no path is provided, a log file is
 * created in the Java Temp directory.
 */
public class LoggingRouter implements Router {

  /**
   * System property to determine if logging is enabled or not.
   */
  public static final String JABS_LOGGING_ENABLED = "jabs.logging.enabled";
  /**
   * The full path to the logging file for jabs
   */
  public static final String JABS_LOGGING_PATH = "jabs.log.path";
  static final String DEFAULT_LOG_PATH =
      System.getProperty("java.io.tmpdir") + "/jabs-log-" + System.currentTimeMillis() + ".log";

  /**
   * When the system started.
   */
  static final Instant TIME_ORIGIN = Instant.now();

  static final class LoggingEvent {
    private final long time;
    private final String from;
    private final String to;
    private final String message;
    private final String toString;

    LoggingEvent(String from, String to, String message) {
      final Instant now = Instant.now();
      this.time = now.toEpochMilli();
      this.from = from;
      this.to = to;
      this.message = message;
      final long relativeTime = Duration.between(TIME_ORIGIN, now).toMillis();
      this.toString = String.join(";", Long.toString(time), Long.toString(relativeTime), this.from,
          this.to, this.message);
    }

    @Override
    public String toString() {
      return toString;
    }
  }

  static final class LoggingThread extends Thread {
    private static final int BUFFER_FLUSH_SIZE = 8 * 1024;
    private static final long PERIOD = Duration.ofMillis(100).toMillis();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final BlockingQueue<LoggingEvent> events;
    private final Path logPath;

    LoggingThread(BlockingQueue<LoggingEvent> events, Path logPath) {
      super("jabs-logging");
      this.events = events;
      this.logPath = logPath;
    }

    @Override
    public void run() {
      if (!running.compareAndSet(false, true)) {
        throw new IllegalStateException("Cannot run while running is not enabled");
      }
      while (running.get()) {
        try {
          Thread.sleep(PERIOD);
        } catch (InterruptedException e) {
          // Ignored
        }
        flush();
      }
      flush();
    }

    @Override
    public void interrupt() {
      running.getAndSet(false);
    }

    private void flush() {
      List<LoggingEvent> buffer = new ArrayList<>();
      fillBuffer(buffer, events);
      if (buffer.isEmpty()) {
        return;
      }
      writeBuffer(buffer, logPath);
    }

    private void writeBuffer(List<LoggingEvent> buffer, Path path) {
      try (BufferedWriter w =
          Files.newBufferedWriter(path, StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {
        List<String> lines = buffer.stream().map(e -> e.toString()).collect(Collectors.toList());
        lines.forEach(line -> {
          try {
            w.write(line);
            w.newLine();
          } catch (Exception ignored) {
            // Ignored
          }
        });
      } catch (IOException e) {
        // Ignored
      }
    }

    private void fillBuffer(List<LoggingEvent> buffer, BlockingQueue<LoggingEvent> events) {
      if (events.size() <= BUFFER_FLUSH_SIZE) {
        buffer.addAll(events);
        events.clear();
      } else {
        for (int i = 0; i < BUFFER_FLUSH_SIZE; ++i) {
          LoggingEvent e = events.poll();
          if (e == null) {
            break;
          }
          buffer.add(e);
        }
      }
    }
  }

  private final BlockingQueue<LoggingEvent> events = new LinkedBlockingQueue<>();
  private final boolean enabled;

  /**
   * Ctor.
   * 
   * @see #LoggingRouter(boolean, String)
   */
  public LoggingRouter() {
    this(Boolean.getBoolean(JABS_LOGGING_ENABLED),
        System.getProperty(JABS_LOGGING_PATH, DEFAULT_LOG_PATH));
  }

  /**
   * Ctor
   * 
   * @param enabled if the logging is enabled
   */
  LoggingRouter(final boolean enabled) {
    this(enabled, DEFAULT_LOG_PATH);
  }

  /**
   * Ctor
   * 
   * @param enabled if the logging is enabled
   * @param logFilePath the full path to the log file
   */
  public LoggingRouter(final boolean enabled, String logFilePath) {
    this.enabled = enabled;
    if (!enabled) {
      return;
    }
    try {
      LoggingThread lt = new LoggingThread(events, Paths.get(logFilePath));
      lt.start();
    } catch (Exception e) {
      // Ignore
    }
  }

  @Override
  public void route(Envelope envelope) {
    if (!this.enabled) {
      return;
    }
    LoggingEvent event = create(envelope);
    events.offer(event);
  }

  private LoggingEvent create(Envelope e) {
    String from = e.from() == null ? "NOBODY" : e.from().simpleName();
    String to = e.to().simpleName();
    String message = toString(e.message());
    return new LoggingEvent(from, to, message);
  }

  @Override
  public void bind(Context context) {}

  protected String toString(Object o) {
    if (o == null) {
      return "null";
    }
    final String hashCode = "@" + Integer.toHexString(o.hashCode());
    if (o.toString().contains("Lambda")) {
      return "Msg" + hashCode;
    }
    if (o instanceof Runnable || o instanceof Callable) {
      return o.getClass().getSimpleName() + hashCode;
    }
    return "Msg" + hashCode;
  }

}
