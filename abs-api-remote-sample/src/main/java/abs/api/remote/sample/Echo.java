package abs.api.remote.sample;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import abs.api.Actor;
import abs.api.Reference;

/**
 * @author Behrooz Nobakht
 */
public class Echo implements Actor {

  private static final long serialVersionUID = 1L;

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final Integer index;
  private final AtomicLong count = new AtomicLong(0);

  public Echo(Integer i) {
    this.index = i;
  }

  public void echo(String message) {
    Reference sender = senderReference();
    logger.error("sender: {}", sender);
    send(sender, "an echo from e-" + index + " -> " + count.incrementAndGet());
  }

}
