package abs.api;

import java.time.Duration;
import java.time.Instant;

/**
 * An abstraction for time measurement for an active object such
 * as {@link Context}.
 */
public interface Timed {

  /**
   * The start of this context.
   * 
   * @return an instant of time for the start of the context
   */
  default Instant startTime() {
    return ContextClock.T0;
  }

  /**
   * Now.
   * 
   * @return the current time
   */
  default Instant now() {
    return ContextClock.CLOCK.instant();
  }

  /**
   * The uptime of this timed instance.
   * 
   * @return a duration between {@link #startTime()} and now.
   */
  default Duration upTime() {
    return ContextClock.uptime();
  }

}
