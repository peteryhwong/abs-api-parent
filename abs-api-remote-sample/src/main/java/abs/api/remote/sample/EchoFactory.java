package abs.api.remote.sample;

import abs.api.Factory;

/**
 * @author Behrooz Nobakht
 */
public class EchoFactory implements Factory {

	@Override
	public Object create(String fqcn, String... ctorArguments) {
		Integer i = Integer.parseInt(ctorArguments[0]);
		return new Echo(i);
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return Echo.class.isAssignableFrom(clazz);
	}

}
