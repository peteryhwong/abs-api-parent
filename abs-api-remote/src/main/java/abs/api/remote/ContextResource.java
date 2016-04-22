package abs.api.remote;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Collection;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import abs.api.Actor;
import abs.api.Context;
import abs.api.FactoryLoader;
import abs.api.Reference;

/**
 * @author Behrooz Nobakht
 * @since 1.0
 */
@Provider
@Path("actors")
public class ContextResource {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final Context context;
  private final URI uri;
  private final FactoryLoader factoryLoader = new FactoryLoader();

  private final Integer maxLocalActors;
  private final ConcurrentMap<Class, MessageConverter> paramConverters;
  private final ConcurrentMap<Class, BiConsumer> messageConsumers;

  public ContextResource(Context context, URI uri, Integer maxLocalActors,
      ConcurrentMap<Class, MessageConverter> paramConverters,
      ConcurrentMap<Class, BiConsumer> messageConsumers) {
    this.context = context;
    this.uri = uri;
    this.maxLocalActors = maxLocalActors;
    this.paramConverters = paramConverters;
    this.messageConsumers = messageConsumers;
  }

  @GET
  public Response get() {
    return Response.status(Status.OK).entity(context.notary().size()).build();
  }

  @Path("{to}")
  public ActorResource to(@PathParam("to") String to) {
    try {
      Reference target = Reference.decode(to);
      Actor actor = (Actor) context.notary().identify(target);
      Object actorObject = context.notary().get(actor);
      MessageConverter converter = getParamConverter(actorObject, paramConverters);
      BiConsumer consumer = getMessageConsumer(actorObject, messageConsumers);
      return new ActorResource(context, actor, converter, consumer, actorObject);
    } catch (UnsupportedEncodingException e) {
      logger.error("No actor found: {}", to);
      throw new RuntimeException(e);
    }
  }

  protected BiConsumer getMessageConsumer(Object actorObject,
      ConcurrentMap<Class, BiConsumer> messageConsumers) {
    BiConsumer consumer = messageConsumers.get(actorObject.getClass());
    return consumer;
  }

  protected MessageConverter getParamConverter(Object actorObject,
      ConcurrentMap<Class, MessageConverter> paramConverters) {
    MessageConverter converter = paramConverters.get(actorObject.getClass());
    return converter;
  }

  @Consumes(MediaType.TEXT_PLAIN)
  @Produces(MediaType.TEXT_PLAIN)
  @PUT
  @Path("{name}")
  public Response create(@PathParam("name") String name, @QueryParam("class") String fqcn,
      Collection<String> params) {
    if (context.notary().size() >= maxLocalActors) {
      return Response.status(Status.NOT_ACCEPTABLE).entity("Maximum local actors reached.").build();
    }
    try {
      final Object object = factoryLoader.create(fqcn, params.toArray(new String[] {}));
      final Actor actor = context.newActor(name, object);
      return Response.status(Status.CREATED).entity(actor.name().toString()).build();
    } catch (Exception e) {
      return Response.status(Status.BAD_REQUEST)
          .entity(e.getMessage() + (e.getCause() != null ? e.getCause().getMessage() : "")).build();
    }
  }

}
