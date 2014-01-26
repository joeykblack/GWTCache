GWT Client Side Caching
by Joey K. Black

This is a client side caching library that can be used with 
GWT RPC services.

To make your service cached:

1. Add GWTCache.jar to your GWT project.

2. Add the following to your gwt.xml file:
<inherits name='jkb.gwt.client.cache.GWTCache'/>

3. On your GWT RPC service synchronous interface extend:
jkb.gwt.client.cache.shared.ICacheable

4. Add the @Cache annotation to any method you would like
to have cached.


Example:

import jkb.gwt.client.cache.shared.Cache;
import jkb.gwt.client.cache.shared.ICacheable;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("greet")
public interface GreetingService extends RemoteService, ICacheable
{
	@Cache
	String greetServer(String name);
}


