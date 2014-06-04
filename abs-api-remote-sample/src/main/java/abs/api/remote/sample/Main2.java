package abs.api.remote.sample;

import java.net.UnknownHostException;
import java.util.Properties;

import abs.api.Actor;
import abs.api.Reference;
import abs.api.remote.ActorServer;

/**
 * @author Behrooz Nobakht
 */
public class Main2 {

	public static void main(String[] args) throws UnknownHostException {

		Properties props2 = new Properties();
		props2.put("host", "localhost");
		props2.put("port", "8888");
		ActorServer server2 = new ActorServer(props2);

		Echo e2 = new Echo(2);
		Actor a2 = server2.context.newActor("echo-2", e2);

		a2.send(Reference.from("abs://echo-1@http://localhost:7777"), "a msg from echo-2");

	}
}
