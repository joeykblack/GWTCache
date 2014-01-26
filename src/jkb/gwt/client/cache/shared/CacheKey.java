/**
 * 
 */
package jkb.gwt.client.cache.shared;

/**
 * @author joeykblack
 *
 *	Key Used for caching
 */
public class CacheKey
{
	private String methodName;
	private Object[] objects;

	/**
	 * Cache based on method name and parameter objects
	 * 
	 * @param methodName
	 * @param objects
	 */
	public CacheKey(String methodName, Object...objects)
	{
		this.methodName = methodName;
		this.objects = objects;
	}
	
	@Override
	public int hashCode()
	{
		int hash = 7;
		if (methodName!=null)
		{
			hash = (hash*31) + methodName.hashCode();
		}
		else
		{
    		hash *= 31;
		}
		
		if (objects!=null)
		{
			for (Object object : objects)
			{
		    	if (object!=null)
		    	{
		    		hash = (hash*31) + object.hashCode();
		    	}	
		    	else
		    	{
		    		hash *= 31;
		    	}
			}
		}
		return hash;
	}
	
	@Override
	public boolean equals(Object o)
	{
		boolean eq = false;
		
    	if (this == o)
    	{
    		eq = true;
    	}
    	else if ((o == null) || (o.getClass() != this.getClass()))
    	{
    		eq = false;
    	}
    	else 
    	{
    		CacheKey ck = (CacheKey) o;
    		
    		eq = (methodName==ck.getMethodName())
    			|| ( methodName!=null
    					&& ck.getMethodName()!=null
    					&& methodName.equals(ck.getMethodName()) );
    		
    		eq &= (objects==ck.getObjects()) 
    			|| ( objects!=null 
    					&& ck.getObjects()!=null 
    					&& (objects.length == ck.getObjects().length) );
    		
    		if (eq && objects!=null) 
    		{
    			for (int i = 0; (i < objects.length) && eq; i++)
				{
					eq &= ( objects[i]==ck.getObjects()[i] )
						|| ( (objects[i]!=null) 
								&& ck.getObjects()[i]!=null
								&& objects[i].equals(ck.getObjects()[i]) );
				}
    		}
    	}
    	
    	return eq;
	}

	public String getMethodName()
	{
		return methodName;
	}

	public void setMethodName(String methodName)
	{
		this.methodName = methodName;
	}

	public Object[] getObjects()
	{
		return objects;
	}

	public void setObjects(Object[] objects)
	{
		this.objects = objects;
	}
	
}
