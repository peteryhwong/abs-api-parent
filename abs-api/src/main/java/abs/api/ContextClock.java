package abs.api;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

class ContextClock extends Clock {

  /**
   * 
   */
  public static final Clock CLOCK = new ContextClock();

  /**
   * The internal system {@link Clock}
   */
  private static final Clock SYSTEM_CLOCK = Clock.systemUTC();

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
