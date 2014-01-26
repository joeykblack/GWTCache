/**
 * 
 */
package jkb.gwt.client.cache.shared;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author joeykblack
 *
 *	This annotation goes on the methods of the 
 *	interface for your service that you want to 
 *  have cached.
 *  
 *  See also: jkb.gwt.client.cache.ICacheable
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Cache
{
	/**
	 * Time to live in seconds.
	 * Default: 60 sec
	 */
	int timeToLive() default 60; 
	
	/**
	 * Use collision detection?
	 * Default: false
	 */
	boolean superCollisionProof() default false;
}
