/**
 * 
 */
package jkb.gwt.client.cache.server;


import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jkb.gwt.client.cache.shared.Cache;
import jkb.gwt.client.cache.shared.CacheKey;
import jkb.gwt.client.cache.shared.CacheObject;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.dev.generator.NameFactory;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;


/**
 * 
 * Generator for @Cache methods in classes of type ICacheable
 * 
 * This will generate the normal gwt service proxy and then
 * generate a child class of the proxy that has impls of 
 * @Cache methods that wrap the call to the parent with caching.
 * 
 * 
 * @author joeykblack
 *
 */
public class CachedServiceGenerator extends Generator
{
	private static final String ASYNC_SUFIX = "Async";
	private static final String GENERATED_SUFIX = "_Cached";
	private static final String HASH_VAR_NAME = "jkbCachingHashMap";
	private static final String TIMER_VAR_NAME = "jkbCacheClearingTimer";
	
	
	/* (non-Javadoc)
	 * @see com.google.gwt.core.ext.Generator#generate(com.google.gwt.core.ext.TreeLogger, com.google.gwt.core.ext.GeneratorContext, java.lang.String)
	 */
	@Override
	public String generate(TreeLogger logger, GeneratorContext context,
			String parentClassName) throws UnableToCompleteException
	{
		String generatedQualifiedName = null;
		try
		{
			// Get class type
			JClassType parentClassType = context.getTypeOracle().getType(parentClassName);
			JClassType interfaceClassType = GWTGeneratorUtil.
				findActualSynchronousInterfaceForService(parentClassType);
			String syncInterfaceName = interfaceClassType.getQualifiedSourceName();
			
			// Get my class info
			String packageName = parentClassType.getPackage().getName();
			String simpleName = parentClassType.getSimpleSourceName() + GENERATED_SUFIX;

			// Create parent class if RemoteServiceProxy is not in 
			// the class hierarchy
			parentClassName = GWTGeneratorUtil.getProxiedServiceClassName(logger, context,
					parentClassName, parentClassType, syncInterfaceName);
			
			// Start class
			SourceWriter sw = getSourceWriter(parentClassName, packageName,
					simpleName, syncInterfaceName, context, logger);
			
			// If sw==null then the file has already been generated
			// See:  GeneratorContext.tryCreate
			if (sw != null)
			{
			
				// Get async class
				JClassType classTypeAsync = context.getTypeOracle().getType(syncInterfaceName + ASYNC_SUFIX);
				
				// Add member variables, constructor and default methods  
				addMemberVariables(sw);
				addConstructor(sw, simpleName);
				addAsyncFactoryMethod(sw);
				addGetResultMethod(sw);
				
				// Find sync interface methods with annotation and add as cacheable
				JMethod[] asyncMethods = classTypeAsync.getMethods();
				JMethod[] syncMethods = interfaceClassType.getMethods();
				for (JMethod syncMethod : syncMethods)
				{
					// If annotated with @Cacheable
					Cache cacheable = syncMethod.getAnnotation(Cache.class);
					if (cacheable != null)
					{
						// Add method with cached wrapper
						addCachedMethod(sw, asyncMethods, syncMethod, cacheable);
					}
				}
				
				// Commit file
				sw.commit(logger);
			}
			
			// Return fully qualified name
			generatedQualifiedName = packageName + "." + simpleName;
			
		}
		catch (NotFoundException e)
		{
			e.printStackTrace();
		}
		
		return generatedQualifiedName;
	}




	/**
	 * Writes member variables
	 * 
	 * @param sw
	 */
	private void addMemberVariables(SourceWriter sw)
	{
		sw.println("private Map<CacheKey, CacheObject> " + HASH_VAR_NAME + ";");
		sw.println("private Timer " + TIMER_VAR_NAME + ";");
	}
	
	
	
	/**
	 * Writes constructor
	 * 
	 * Initializes hash map and timer for clearing cache
	 * 
	 * @param sw
	 * @param simpleName
	 */
	private void addConstructor(SourceWriter sw, String simpleName)
	{
		sw.println("public " + simpleName + "()");
		sw.println("{");
		sw.indent();
			sw.println("super();");
			sw.println(HASH_VAR_NAME + " = new HashMap<CacheKey, CacheObject>();");
			
			// Add timer to clean cache
			sw.println(TIMER_VAR_NAME + " = new Timer()");
			sw.println("{");
			sw.indent();
				sw.println("public void run()");
				sw.println("{");
				sw.indent();
					sw.println("Set<CacheKey> removalSet = new HashSet<CacheKey>();");
					
					sw.println("for (CacheKey key : "+HASH_VAR_NAME+".keySet())");
					sw.println("{");
					sw.indent();
						// bring out your dead
						sw.println("if ( (System.currentTimeMillis()-"+HASH_VAR_NAME+".get(key).getStamp()) > ("+HASH_VAR_NAME+".get(key).getTimeToLive()*1000) )");
						sw.println("{");
						sw.indent();
							sw.println("removalSet.add(key);");
						sw.outdent();
						sw.println("}");
					sw.outdent();
					sw.println("}");
					
					// clip dead items from cache
					sw.println("for (CacheKey key : removalSet)");
					sw.println("{");
					sw.indent();
						sw.println(HASH_VAR_NAME+".remove(key);");
					sw.outdent();
					sw.println("}");
					
				sw.outdent();
				sw.println("}");
			sw.outdent();
			sw.println("};");
			
			sw.println((TIMER_VAR_NAME + ".scheduleRepeating(60000);")); // clean cache once every minute
			
		sw.outdent();
		sw.println("}");
	}

	/**
	 * Adds instanceOfAsyncCallback method for creation of
	 * AsyncCallback that will cache result.
	 * 
	 * If a value is not in the cache, we need to call super (the actual service)
	 * and pass it an AsyncCallback that wraps the original AsyncCallback.
	 * Our AsyncCallback will handle caching before passing the results on to
	 * the original callback. 
	 * 
	 * @param sw
	 */
	private void addAsyncFactoryMethod(SourceWriter sw)
	{
		sw.println("private AsyncCallback instanceOfAsyncCallback(");
		sw.println("		final AsyncCallback callback, final CacheKey cacheKey, final int timeToLive)");
		sw.println("{");
		sw.indent();
				sw.println("return new AsyncCallback() {");
				sw.indent();
				
					sw.println("public void onSuccess(Object result)");
					sw.println("{");
					sw.indent();
						sw.println("CacheObject co = new CacheObject(cacheKey, result, timeToLive);");
						sw.println(HASH_VAR_NAME+".put(cacheKey, co);");
						sw.println("callback.onSuccess(result);");
					sw.outdent();
					sw.println("}");
					
					// onFailure: return exception without caching
					sw.println("public void onFailure(Throwable caught)");
					sw.println("{");
					sw.indent();
						sw.println("callback.onFailure(caught);");
					sw.outdent();
					sw.println("}");
				sw.outdent();
				sw.println("};");
		sw.outdent();
		sw.println("}");
	}

	
	/**
	 * Add a method that gets items from the cache while
	 * checking for collisions.
	 * 
	 * If we are checking for collisions (superCollisionProof) then
	 * we need to compare the cacheKeys explicitly in case they had
	 * the same hash but different values (this is highly unlikely).
	 * 
	 * @param sw
	 */
	private void addGetResultMethod(SourceWriter sw)
	{
		sw.println("private CacheObject getResult(CacheKey cacheKey, boolean superCollisionProof)");
		sw.println("{");
		sw.indent();
			sw.println("CacheObject result = "+HASH_VAR_NAME+".get(cacheKey);");
			sw.println("if (superCollisionProof && result!=null && !cacheKey.equals(result.getCacheKey()))");
			sw.println("{");
			sw.indent();
				sw.println("result = null;");
			sw.outdent();
			sw.println("}");
			sw.println("return result;");
		sw.outdent();
		sw.println("}");
	}
	
	

	/**
	 * Find async method and write cached wrapper.
	 * 
	 * We have to iterate through the synchronous methods
	 * to check for @Cache annotation, but we need the async
	 * method signature to generate the implementation.
	 * 
	 * @param sw
	 * @param asyncMethods
	 * @param syncMethod
	 * @param cacheable
	 */
	private void addCachedMethod(SourceWriter sw, JMethod[] asyncMethods,
			JMethod syncMethod, Cache cacheable)
	{
		for (JMethod asyncMethod : asyncMethods)
		{
			if (syncMethod.getName().equals(asyncMethod.getName()))
			{
				addCachedAsyncMethod(asyncMethod, cacheable, sw);
				break;
			}
		}
	}


	/**
	 * Creates override of @Cache annotated parent method
	 * that wraps the parent implementation with a cached version.
	 * 
	 * @param method
	 * @param cacheable
	 * @param w
	 */
	private void addCachedAsyncMethod(JMethod method, Cache cacheable,
			SourceWriter w)
	{
		// Start Method
		startMethod(w, method);
		
		// Initialize CacheKey
		w.print( "final CacheKey cacheKey = new CacheKey(\"" + method.getName() + "\"");
	    JParameter[] asyncParams = method.getParameters();
	    // Add all method parameters (except callback) to CacheKey constructor call
	    for (int i = 0; i < asyncParams.length; ++i) {
	    	JParameter param = asyncParams[i];
	    	if ( ! param.getType().getQualifiedSourceName()
	    			.equals("com.google.gwt.user.client.rpc.AsyncCallback") )
	    	{
		      w.print(", ");
		      String paramName = param.getName();
		      w.print(paramName);
	    	} 
	    }
		w.println( ");" );
		
		// Get result from cache
		w.println( "CacheObject result = getResult(cacheKey, " + cacheable.superCollisionProof() + "); \n" );
		
		// if cached value found
		w.println( "if ( result != null && (System.currentTimeMillis()-result.getStamp()) < (result.getTimeToLive()*1000) )" );
		w.println( "{" ); 
		w.indent();
		
			// return cached value

			w.println( "if (result.getObject() instanceof Object[])" );
			w.println( "{" );
			w.indent();
				w.println( "callback.onSuccess(result.getObjectArray());" );
			w.outdent(); 
			w.println( "}" );
			w.println( "else" );
			w.println( "{" );
			w.indent();
				w.println( "callback.onSuccess(result.getObject());" );
			w.outdent(); 
			w.println( "}" );
			
		w.outdent(); 
		w.println( "}" );
		
		// else call super
		w.println( "else" );
		w.println( "{" );
		w.indent();
		
			// Call to super
			w.print("super." + method.getName() + "(");
			boolean needsComma = false;
		    asyncParams = method.getParameters();
		    for (int i = 0; i < asyncParams.length; ++i) {
		      JParameter param = asyncParams[i];

		      if (needsComma) {
		        w.print(", ");
		      } else {
		        needsComma = true;
		      }

		      String paramName = param.getName();
		      if (paramName.equals("callback"))
		      {
		    	  // Replace callback param with new callback
		    	  // that caches result before passing it on
		    	  w.print("instanceOfAsyncCallback(callback, cacheKey, " + cacheable.timeToLive() + ")");
		      }
		      else
		      {
		    	  w.print(paramName);
		      }
		    }
		    w.println(");");
		    
		w.outdent(); 
		w.println( "}" );
		
		// End Method
		w.outdent();
		w.println("}");
		w.outdent();
	}

	
	/**
	 * Copied from ProxyCreator. 
	 * Writes:
	 * public returnType methodName(paramList) {
	 * 
	 * @param w
	 * @param asyncMethod
	 */
	private void startMethod(SourceWriter w, JMethod asyncMethod)
	{
		w.println();

	    // Write the method signature
	    JType asyncReturnType = asyncMethod.getReturnType().getErasedType();
	    w.print("public ");
	    w.print(asyncReturnType.getQualifiedSourceName());
	    w.print(" ");
	    w.print(asyncMethod.getName() + "(");

	    boolean needsComma = false;
	    NameFactory nameFactory = new NameFactory();
	    JParameter[] asyncParams = asyncMethod.getParameters();
	    for (int i = 0; i < asyncParams.length; ++i) {
	      JParameter param = asyncParams[i];

	      if (needsComma) {
	        w.print(", ");
	      } else {
	        needsComma = true;
	      }

	      /*
	       * Ignoring the AsyncCallback parameter, if any method requires a call to
	       * SerializationStreamWriter.writeObject we need a try catch block
	       */
	      JType paramType = param.getType();
	      paramType = paramType.getErasedType();

	      w.print(paramType.getQualifiedSourceName());
	      w.print(" ");

	      String paramName = param.getName();
	      nameFactory.addName(paramName);
	      w.print(paramName);
	    }

	    w.println(") {");
	    w.indent();
	}
	
	
	/**
	 * Sets up class and return new SourceWriter
	 * 
	 * @param classType
	 * @param context
	 * @param logger 
	 * @param printWriter2 
	 * @return SourceWriter
	 * @throws UnableToCompleteException 
	 */
	private SourceWriter getSourceWriter( String generatedParentClassName, String packageName, String simpleName, 
			String syncInterfaceName, GeneratorContext context, TreeLogger logger ) throws UnableToCompleteException
	{
		// if the printWriter is null, then the class has already been created
		PrintWriter printWriter = context.tryCreate(logger, packageName, simpleName);
		SourceWriter sourceWriter = null;
		
		if (printWriter != null) 
		{
			ClassSourceFileComposerFactory composer = new ClassSourceFileComposerFactory(
					packageName, simpleName);
			
			// Setup inheritance
			composer.setSuperclass(generatedParentClassName);
			composer.addImplementedInterface(syncInterfaceName + ASYNC_SUFIX);
			
			// Need to add whatever imports your generated class needs.
			composer.addImport(HashMap.class.getCanonicalName());
			composer.addImport(Map.class.getCanonicalName());
			composer.addImport(Set.class.getCanonicalName());
			composer.addImport(HashSet.class.getCanonicalName());
			composer.addImport(CacheKey.class.getCanonicalName());
			composer.addImport(CacheObject.class.getCanonicalName());
			composer.addImport(GWT.class.getCanonicalName());
			composer.addImport(Dictionary.class.getCanonicalName());
			composer.addImport(AsyncCallback.class.getCanonicalName());
			composer.addImport(Timer.class.getCanonicalName());
			
			sourceWriter = composer.createSourceWriter(context, printWriter);
		}
		
		return sourceWriter;
	}


}
