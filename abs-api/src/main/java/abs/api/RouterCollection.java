package abs.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * A router that unifies routing over a collection of routers.
 */
public class RouterCollection implements Router, Iterable<Router> {

  private final List<Router> routers;

  /**
   * Ctor
   * 
   * @param routers the routers
   */
  public RouterCollection(Collection<Router> routers) {
    this.routers = new ArrayList<>(routers);
  }

  /**
   * Ctor
   * 
   * @param routers the routers
   */
  public RouterCollection(Router... routers) {
    this.routers = Arrays.asList(routers);
  }

  @Override
  public void route(Envelope envelope) {
    for (Router router : this.routers) {
      router.route(envelope);
    }
  }

  @Override
  public void bind(Context context) {
    for (Router router : this.routers) {
      router.bind(context);
    }
  }

  @Override
  public Iterator<Router> iterator() {
    return this.routers.iterator();
  }

}
