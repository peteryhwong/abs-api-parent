package abs.api.remote;

import java.io.InputStream;
import java.util.function.BiConsumer;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import abs.api.Actor;
import abs.api.Context;
import abs.api.Envelope;
import abs.api.Reference;
import abs.api.SimpleEnvelope;

/**
 * @author Behrooz Nobakht
 */
@Provider
public class ActorResource {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final Context context;
  private final Actor actor;
  private final Object actorObject;
  private final MessageConverter converter;
  private final BiConsumer consumer;

  public ActorResource(Context context, Actor actor, MessageConverter converter,
      BiConsumer consumer, Object actorObject) {
    this.context = context;
    this.actor = actor;
    this.converter = converter;
    this.consumer = consumer;
    this.actorObject = actorObject;
  }

  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.TEXT_PLAIN)
  @Path("{from}")
  @PUT
  public Response send(@PathParam("from") String from, InputStream msg) {
    try {
      Reference sender = Reference.decode(from);
      Object messageParam = convertMessage(msg, converter);
      logger.debug("Received a message parameter from {} to {}: {}", sender, this.actor,
          messageParam);
      abs.api.Response<Object> response = consumeMessage(sender, this.actor, this.actorObject,
          this.consumer, messageParam, this.context);
      logger.debug("Remote envelope sent to {} from {}: {}", this.actor.toString(),
          sender.toString(), response);
      return Response.ok(response.get(), MediaType.TEXT_PLAIN).build();
    } catch (Throwable e) {
      return Response.status(Status.INTERNAL_SERVER_ERROR).type(MediaType.TEXT_PLAIN)
          .entity(e.toString()).build();
    }
  }

  protected abs.api.Response<Object> consumeMessage(Reference sender, Actor receiver,
      Object actorObject, BiConsumer consumer, Object messageParam, Context context) {
    if (consumer == null) {
      Envelope e = new SimpleEnvelope(sender, receiver, messageParam);
      context.router().route(e);
      return e.response();
    } else {
      Actor senderActor = new Actor() {
        @Override
        public Reference self() {
          return sender;
        }
      };
      Runnable message = () -> consumer.accept(actorObject, messageParam);
      abs.api.Response<Object> response = senderActor.send(receiver, message);
      return response;
    }
  }

  protected Object convertMessage(InputStream in, MessageConverter converter) {
    if (converter == null) {
      return MessageConverter.readObject(in);
    }
    return converter.apply(in);
  }

}
