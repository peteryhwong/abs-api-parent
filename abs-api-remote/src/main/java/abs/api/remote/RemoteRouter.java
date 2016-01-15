package abs.api.remote;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;

import abs.api.Context;
import abs.api.Envelope;
import abs.api.Router;

/**
 * @author Behrooz Nobakht
 * @since 1.0
 */
public class RemoteRouter implements Router {

	private final ConcurrentMap<URI, WebTarget> targets = new ConcurrentHashMap<>(4096);
	private final URI uri;
	private Context context;

	public RemoteRouter(URI uri) {
		this.uri = uri;
	}

	@Override
	public void route(Envelope envelope) {
		URI uri = getRemoteURI(envelope);
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

	protected URI getRemoteURI(Envelope envelope) {
		String name = envelope.to().name().toASCIIString();
		int index = name.indexOf('@');
		if (index == -1) {
			return null;
		}
		try {
			return URI.create(name.substring(index + 1));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
