package abs.api;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * The {@link Clock} used in {@link Timed} and {@link Context}.
 */
class ContextClock extends Clock {

  /**
   * The internal system {@link Clock}
   */
  private static final Clock SYSTEM_CLOCK = Clock.system(ZoneOffset.UTC);

  /**
   * The {@link Clock} used by {@link Context}
   */
  public static final Clock CLOCK = new ContextClock();

  /**
   * The time of the start of the system
   */
  public static final Instant T0 = CLOCK.instant();

  /**
   * The uptime of the clock of the context
   * 
   * @return the duration in which the context has been up and
   *         running
   */
  public static Duration uptime() {
    return Duration.between(T0, CLOCK.instant());
  }

  private ContextClock() {}

  @Override
  public ZoneId getZone() {
    return SYSTEM_CLOCK.getZone();
  }

  @Override
  public Clock withZone(ZoneId zone) {
    return this;
  }

  @Override
  public Instant instant() {
    return SYSTEM_CLOCK.instant();
  }

  @Override
  public long millis() {
    return super.millis();
  }

  @Override
  public String toString() {
    return "Clock[" + SYSTEM_CLOCK + "]";
  }

}
