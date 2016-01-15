package abs.api.remote;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import abs.api.Envelope;
import abs.api.Reference;
import abs.api.SimpleEnvelope;

/**
 * An implementation of {@link Envelope} to encapsulate
 * communication with a remote actor.
 */
class RemoteEnvelope extends SimpleEnvelope implements Envelope {

  private final Envelope envelope;
  private final WebTarget target;
  private Future<Response> response;

  public RemoteEnvelope(Envelope envelope, WebTarget target) {
    super(envelope.from(), envelope.to(), envelope.message());
    this.envelope = envelope;
    this.target = target;
  }

  @Override
  public <V> abs.api.Response<V> response() {
    final abs.api.Response<V> cf = envelope.response();
    try {
      final Response result = this.response.get(30, TimeUnit.SECONDS);
      Status status = Status.fromStatusCode(result.getStatus());
      switch (status) {
        case OK:
          return envelope.response();
        case BAD_REQUEST:
          cf.completeExceptionally(
              new IllegalArgumentException("Invalid message: " + result.readEntity(String.class)));
        case NOT_FOUND:
          cf.completeExceptionally(new IllegalArgumentException(
              "Remote actor not found: " + result.readEntity(String.class)));
        default:
          cf.completeExceptionally(new IllegalStateException(
              "Unknown error: " + status + " : " + result.readEntity(String.class)));
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

  protected void send() {
    try {
      Object msg = envelope.message();
      Entity<InputStream> message = Entity.entity(
          new ByteArrayInputStream(IOUtils.toByteArray(msg)), MediaType.APPLICATION_OCTET_STREAM);

      String from = Reference.encode(envelope.from());
      String to = Reference.encode(envelope.to());
      WebTarget path = target.path("actors").path(to).path(from);

      this.response =
          path.request().accept(MediaType.TEXT_PLAIN).async().put(message, Response.class);
    } catch (Throwable e) {
      ((CompletableFuture<?>) envelope.response()).completeExceptionally(e);
    }
  }

}
