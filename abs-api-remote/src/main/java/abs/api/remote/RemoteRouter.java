package abs.api.remote;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import abs.api.Context;
import abs.api.Envelope;
import abs.api.Reference;
import abs.api.Router;

/**
 * @author Behrooz Nobakht
 * @since 1.0
 */
public class RemoteRouter implements Router {

	private static final Logger logger = LoggerFactory.getLogger(RemoteRouter.class);

	static class RemoteEnvelope implements Envelope {

		private final Envelope envelope;
		private final WebTarget target;
		private Future<Response> response;

		public RemoteEnvelope(Envelope envelope, WebTarget target) {
			this.envelope = envelope;
			this.target = target;
		}

		@Override
		public Reference from() {
			return envelope.from();
		}

		@Override
		public Reference to() {
			return envelope.to();
		}

		@Override
		public Object message() {
			return envelope.message();
		}

		@Override
		public <V> abs.api.Response<V> response() {
			final abs.api.Response<V> cf = envelope.response();
			try {
				final Response result = this.response.get(30, TimeUnit.SECONDS);
				Status status = Status.fromStatusCode(result.getStatus());
				logger.debug("Route result: {}", status);
				switch (status) {
				case OK:
					return envelope.response();
				case BAD_REQUEST:
					cf.completeExceptionally(new IllegalArgumentException("Invalid message: "
							+ result.readEntity(String.class)));
				case NOT_FOUND:
					cf.completeExceptionally(new IllegalArgumentException(
							"Remote actor not found: " + result.readEntity(String.class)));
				default:
					cf.completeExceptionally(new IllegalStateException("Unknown error: "
							+ status + " : " + result.readEntity(String.class)));
				}
				result.close();
			} catch (InterruptedException e) {
				cf.completeExceptionally(e);
			} catch (ExecutionException e) {
				cf.completeExceptionally(e);
			} catch (TimeoutException e) {
				cf.completeExceptionally(e);
			} catch (ProcessingException e) {
				// ignore for closing result response
			}
			return envelope.response();
		}

		@Override
		public long sequence() {
			return envelope.sequence();
		}

		public void send() {
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
					ObjectOutputStream oos = new ObjectOutputStream(baos)) {
				oos.writeObject(envelope.message());
				Entity<InputStream> message = Entity.entity(
						new ByteArrayInputStream(baos.toByteArray()),
						MediaType.APPLICATION_OCTET_STREAM);

				String from = Reference.encode(envelope.from());
				String to = Reference.encode(envelope.to());

				WebTarget path = target.path("actors").path(to).path(from);
				logger.debug("Routing to {}",
						URLDecoder.decode(path.getUri().toString(), "UTF-8"));

				this.response = path.request().accept(MediaType.TEXT_PLAIN).async()
						.put(message, Response.class);
			} catch (Exception e) {
				((CompletableFuture<?>) envelope.response()).completeExceptionally(e);
				logger.error("Failed to send remote message to {}: {}", envelope.to(), e);
			}
		}

	}

	private final ConcurrentMap<URI, WebTarget> targets = new ConcurrentHashMap<>(4096);
	private final URI uri;
	private Context context;

	public RemoteRouter(URI uri) {
		this.uri = uri;
	}

	@Override
	public void route(Envelope envelope) {
		URI uri = getURI(envelope);
		WebTarget target = getWebTargetClient(envelope, uri);
		RemoteEnvelope renv = new RemoteEnvelope(envelope, target);
		renv.send();
	}

	@Override
	public void bind(Context context) {
		this.context = context;
	}

	protected WebTarget getWebTargetClient(Envelope envelope, URI uri) {
		WebTarget target = targets.get(uri);
		if (target != null) {
			return target;
		}
		System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
		Integer timeout = Long.valueOf(TimeUnit.MINUTES.toMillis(10)).intValue();
		ClientConfig cc = new ClientConfig().property(ClientProperties.READ_TIMEOUT, timeout)
				.property(ClientProperties.CONNECT_TIMEOUT, timeout)
				.connectorProvider(new HttpUrlConnectorProvider().useSetMethodWorkaround());
		Client client = ClientBuilder.newClient(cc);
		target = client.target(uri);
		targets.put(uri, target);
		return target;
	}

	protected URI getURI(Envelope envelope) {
		String name = envelope.to().name().toASCIIString();
		int index = name.indexOf('@');
		if (index == -1) {
			return null;
		}
		try {
			return URI.create(name.substring(index + 1));
		} catch (Exception e) {
			// ignore
		}
		return null;
	}

}
