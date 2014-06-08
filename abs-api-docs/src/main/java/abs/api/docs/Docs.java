package abs.api.docs;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Manifest;

/**
 * @author Behrooz Nobakht
 * @since 1.0
 */
public final class Docs {

	private Docs() {
	}

	public static String getVersion() {
		try {
			InputStream in = Docs.class.getClassLoader().getResourceAsStream(
					"/META-INF/MANIFEST.MF");
			Manifest manifest = new Manifest(in);
			String version = manifest.getMainAttributes().getValue("Implementation-Version");
			if (version == null || version.isEmpty()) {
				return "N/A";
			}
			return version;
		} catch (IOException e) {
		}
		return "N/A";
	}

}
