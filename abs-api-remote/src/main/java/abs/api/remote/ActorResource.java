package abs.api.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

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

	public ActorResource(Context context, Actor actor) {
		this.context = context;
		this.actor = actor;
	}

	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Produces(MediaType.TEXT_PLAIN)
	@Path("{from}")
	@PUT
	public Response send(@PathParam("from") String from, InputStream msg) {
		try {
			Reference sender = Reference.decode(from);
			Object message = readObject(msg);
			logger.debug("Received a message from {} to {}: {}", sender, this.actor, message);
			Envelope e = new SimpleEnvelope(sender, actor, message);
			context.router().route(e);
			logger.debug("Remote envelope sent to {} from {}", this.actor.toString(),
					sender.toString());
			return Response.ok(sender.toString(), MediaType.TEXT_PLAIN).build();
		} catch (Exception e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).type(MediaType.TEXT_PLAIN)
					.entity(e.toString()).build();
		}
	}

	protected Object readObject(InputStream is) throws IOException, ClassNotFoundException {
		try (ObjectInputStream ois = new ObjectInputStream(is)) {
			return ois.readObject();
		}
	}

}
