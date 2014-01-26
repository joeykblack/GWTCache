package jkb.gwt.client.cache.server;


import jkb.gwt.client.cache.shared.ICacheable;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.cfg.Rule;
import com.google.gwt.dev.cfg.RuleGenerateWith;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.javac.rebind.RebindResult;
import com.google.gwt.user.client.rpc.impl.RemoteServiceProxy;
import com.google.gwt.user.rebind.rpc.ServiceInterfaceProxyGenerator;

/**
 * These are some static utilities used by the generator.
 * 
 * @author joeykblack
 *
 */
public class GWTGeneratorUtil
{

	
	/**
	 * This method can find the synchronous interface even if 
	 * it is not in the class hierarchy
	 * 
	 * @param classType
	 * @param typeOracle
	 * @return Original class with that implements ICacheable or null
	 */
	public static JClassType findActualSynchronousInterfaceForService(JClassType classType)
	{
		TypeOracle typeOracle = classType.getOracle();
		JClassType targetClassType = null;
		while(targetClassType == null && classType!=null)
		{
			JClassType[] implementedInterfaces = classType.getImplementedInterfaces();
			for (JClassType interfaceClass : implementedInterfaces)
			{
				if(ICacheable.class.getCanonicalName().equals(interfaceClass.getQualifiedSourceName()))
				{
					targetClassType = classType;
				}
				else if (interfaceClass.getName().endsWith("Async"))
				{
					JClassType sync = typeOracle.findType(interfaceClass.getQualifiedSourceName().replace("Async", ""));
					targetClassType = findActualSynchronousInterfaceForService(sync);
				}
			}
			classType = classType.getSuperclass();
		}
		return targetClassType;
	}

	/**
	 * @param classType
	 * @return true if RemoteServiceProxy is not a supper type of this type
	 */
	public static boolean needToGenerateServiceInterfaceProxy(JClassType classType)
	{
		boolean notFound = true;
		String serviceInterfaceProxyTypeName = RemoteServiceProxy.class.getCanonicalName();
		
		for (JClassType type : classType.getFlattenedSupertypeHierarchy())
		{
			if ( type.getQualifiedSourceName().equals(serviceInterfaceProxyTypeName) )
			{
				notFound = false;
				break;
			}
		}
		
		return notFound;
	}
	


	/**
	 * Create proxied parent class if RemoteServiceProxy is not in 
	 * the class hierarchy. This should be called by all service generators.
	 * 
	 * @param logger
	 * @param context
	 * @param parentClassName - the class name passed into the generator
	 * @param parentClassType
	 * @param syncInterfaceName - the name of the class found by findActualDecorateWidgetClassForService
	 * 
	 * @return name of the class that needs to be extended
	 * @throws UnableToCompleteException
	 */
	public static String getProxiedServiceClassName(TreeLogger logger,
			GeneratorContext context, String parentClassName,
			JClassType parentClassType, String syncInterfaceName)
			throws UnableToCompleteException
	{
		if ( needToGenerateServiceInterfaceProxy(parentClassType) )
		{
			Rule rule = new RuleGenerateWith(ServiceInterfaceProxyGenerator.class);
			RebindResult result = rule.realize(logger, (StandardGeneratorContext) context, syncInterfaceName);
			
			((StandardGeneratorContext) context).finish(logger);
			
			parentClassName = result.getReturnedTypeName();
		}
		
		return parentClassName;
	}
}
