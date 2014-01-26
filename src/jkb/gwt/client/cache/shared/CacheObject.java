/**
 * 
 */
package jkb.gwt.client.cache.shared;


/**
 * @author joeykblack
 * 
 * Object Used for caching
 *
 */
public class CacheObject
{
	
	private Object object;
	private long stamp;
	private CacheKey cacheKey;
	private int timeToLive;
	
	public CacheObject(CacheKey cacheKey, Object object, int timeToLive)
	{
		this.object = object;
		this.cacheKey = cacheKey;
		this.timeToLive = timeToLive;
		stamp = System.currentTimeMillis();
	}
	

	public Object getObject()
	{
		return object;
	}
	public Object[] getObjectArray()
	{
		return (Object[])object;
	}

	public void setObject(Object object)
	{
		this.object = object;
	}

	public long getStamp()
	{
		return stamp;
	}

	public void setStamp(long stamp)
	{
		this.stamp = stamp;
	}

	public CacheKey getCacheKey()
	{
		return cacheKey;
	}

	public void setCacheKey(CacheKey ck)
	{
		this.cacheKey = ck;
	}


	public int getTimeToLive()
	{
		return timeToLive;
	}


	public void setTimeToLive(int timeToLive)
	{
		this.timeToLive = timeToLive;
	}
	
}
