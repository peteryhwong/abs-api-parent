package abs.api.remote.sample;

import java.util.Properties;

import abs.api.Actor;
import abs.api.Configuration;
import abs.api.remote.ActorServer;
import abs.api.remote.MessageConverter;

/**
 * @author Behrooz Nobakht
 */
public class Main1 {

  public static void main(String[] args) throws Exception {
    System.setProperty(Configuration.PROPERTY_DEBUG, "true");
    System.setProperty(Configuration.PROPERTY_THREAD_MANAGEMENT, "false");

    Properties props1 = new Properties();
    props1.put("host", "localhost");
    props1.put("port", "7777");

    Echo e1 = new Echo(1);

    ActorServer server1 = new ActorServer(props1);
    Actor a1 = server1.context.newActor("echo-1", e1);
    server1.registerMessageConsumer(Echo.class, (Echo e, String s) -> {
      e.echo(s);
    });
    server1.registerParamConverter(Echo.class, MessageConverter.TO_STRING);

    System.out.println(" === actor: " + a1.name());

  }

}
