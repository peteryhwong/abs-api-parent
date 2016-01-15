package abs.api.remote.sample;

import java.util.Properties;
import java.util.function.BiConsumer;

import abs.api.Actor;
import abs.api.Configuration;
import abs.api.Context;
import abs.api.Reference;
import abs.api.remote.ActorServer;
import abs.api.remote.MessageConverter;

/**
 * @author Behrooz Nobakht
 */
public class Main2 {

  public static void main(String[] args) throws Exception {
    System.setProperty(Configuration.PROPERTY_DEBUG, "true");
    System.setProperty(Configuration.PROPERTY_THREAD_MANAGEMENT, "false");

    Properties props2 = new Properties();
    props2.put("host", "localhost");
    props2.put("port", "8888");

    Echo e2 = new Echo(2);
    BiConsumer<Echo, String> messageHandler = (e, s) -> {
      e2.echo(s);
    };

    ActorServer server2 = new ActorServer(props2);
    server2.registerParamConverter(Echo.class, MessageConverter.TO_STRING);
    server2.registerMessageConsumer(Echo.class, messageHandler);

    Context context = server2.context;
    Actor a2 = context.newActor("echo-2", e2);

    e2.send(Reference.from("abs://echo-1@http://localhost:7777"), "a msg from echo-2");

  }
}
