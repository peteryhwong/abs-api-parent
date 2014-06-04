package abs.api.remote.sample;

import java.net.UnknownHostException;
import java.util.Properties;

import abs.api.Actor;
import abs.api.remote.ActorServer;

/**
 * @author Behrooz Nobakht
 */
public class Main1 {

	public static void main(String[] args) throws UnknownHostException {

		Properties props1 = new Properties();
		props1.put("host", "localhost");
		props1.put("port", "7777");
		ActorServer server1 = new ActorServer(props1);

		Echo e1 = new Echo(1);
		Actor a1 = server1.context.newActor("echo-1", e1);
		System.out.println(" === actor: " + a1.name());

	}

}
