package abs.api.remote;

import java.net.InetAddress;
import java.net.URI;
import java.util.Properties;

import javax.ws.rs.core.UriBuilder;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.glassfish.jersey.jetty.JettyHttpContainer;
import org.glassfish.jersey.jetty.JettyHttpContainerProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import abs.api.Context;
import abs.api.Lifecycle;

/**
 * @author Behrooz Nobakht
 */
public class ActorServer implements Lifecycle {

	static {
		LoggingConfiguration.configure();
	}

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final String host;
	private final Integer port;
	private final URI uri;
	private final ContextApplication application;
	private final Server server;

	public final Context context;

	public ActorServer(Properties properties) throws Exception {
		host = properties
				.getProperty("host", InetAddress.getLocalHost().getCanonicalHostName());
		port = Integer.valueOf(properties.getProperty("port", "7777"));
		uri = UriBuilder.fromUri("http://" + host).port(port).build();
		application = new ContextApplication(uri);
		ResourceConfig resourceConfig = ResourceConfig.forApplication(application);
		server = createServer(resourceConfig, uri);
		context = application.context;
		logger.info("ABS Actor Context started on: {}", uri);
	}

	@Override
	public void start() throws Exception {
	}

	@Override
	public void stop() throws Exception {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					server.stop();
				} catch (Exception e) {
					logger.error("Failed to stop server on {}: {}", uri, e);
				}
			}
		}).start();
	}

	@Override
	public void initialize() throws Exception {
	}

	/**
	 * @param resourceConfig
	 * @param uri
	 * @return
	 * @throws Exception
	 */
	protected Server createServer(ResourceConfig resourceConfig, URI uri) throws Exception {
		ThreadPool pool = new QueuedThreadPool(1024, 16);
		Server server = new Server(pool);
		HttpConfiguration http = new HttpConfiguration();
		ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(http));
		connector.setPort(uri.getPort());
		server.setConnectors(new Connector[] { connector });

		JettyHttpContainerProvider containerProvider = new JettyHttpContainerProvider();
		JettyHttpContainer handler = containerProvider.createContainer(
				JettyHttpContainer.class, resourceConfig);
		server.setHandler(handler);
		server.start();
		return server;
	}

	public static void main(String[] args) throws Exception {
		new ActorServer(System.getProperties());
	}

}
