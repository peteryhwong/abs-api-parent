package abs.api.remote;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Objects;

/**
 * IO Utility functions.
 */
final class IOUtils {

  private static final int BUF_SIZE = 1024;

  static byte[] toByteArray(InputStream in) {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      copy(in, out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  static <T> T readObject(InputStream in) {
    try (ObjectInputStream ois = new ObjectInputStream(in)) {
      return (T) ois.readObject();
    } catch (IOException | ClassNotFoundException e) {
      throw new IllegalArgumentException(e);
    }
  }

  static byte[] toByteArray(Object o) {
    return o == null ? new byte[0] : o.toString().getBytes(Charset.defaultCharset());
  }

  private static long copy(InputStream from, OutputStream to) throws IOException {
    Objects.requireNonNull(from);
    Objects.requireNonNull(to);
    byte[] buf = new byte[BUF_SIZE];
    long total = 0;
    while (true) {
      int r = from.read(buf);
      if (r == -1) {
        break;
      }
      to.write(buf, 0, r);
      total += r;
    }
    return total;
  }



  private IOUtils() {}

}
