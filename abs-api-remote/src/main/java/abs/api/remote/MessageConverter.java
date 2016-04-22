package abs.api.remote;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.charset.Charset;
import java.util.function.Function;

/**
 * A {@link FunctionalInterface} to sit between the transport
 * layer of actors and the actors themselves, to translate the
 * receiving messages to proper types and instances of objects.
 *
 * @param <T> the type of the target type to translate to
 */
@FunctionalInterface
public interface MessageConverter<T> extends Function<InputStream, T> {

  /**
   * A {@link MessageConverter} from {@link InputStream} to
   * {@link String}
   */
  MessageConverter<String> TO_STRING =
      in -> new String(IOUtils.toByteArray(in), Charset.forName("UTF-8"));

  /**
   * A {@link MessageConverter} from {@link InputStream} to
   * {@link Boolean}
   */
  MessageConverter<Boolean> TO_BOOLEAN = in -> Boolean.parseBoolean(TO_STRING.convert(in));

  /**
   * A {@link MessageConverter} from {@link InputStream} to
   * {@link Long}.
   */
  MessageConverter<Long> TO_LONG = in -> Long.parseLong(TO_STRING.convert(in));

  /**
   * Uses {@link ObjectInputStream} to read the
   * {@link InputStream} and try to cast the result to type
   * <code>C</code>.
   * 
   * @param in the input stream from remote message
   * @param <C> the expected type of created object
   * @return an instance of <code>C</code>
   */
  static <C> C readObject(final InputStream in) {
    MessageConverter<C> mc = is -> IOUtils.readObject(is);
    return mc.convert(in);
  }

  /**
   * Convert a remote message input stream to target type.
   * 
   * @param remoteMessage the remote message as an
   *        {@link InputStream}
   * @return the created instance of type <code>T</code>
   */
  T convert(InputStream remoteMessage);

  @Override
  default T apply(InputStream t) {
    return convert(t);
  }

}
