package com.prtech.triglav_plugins;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.ModelProcessor;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.ResourceMethod.Builder;
import org.glassfish.jersey.server.model.ResourceModel;
import org.json.JSONObject;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.prtech.svarog.ActionJAR;

import com.prtech.svarog.SvClassLoader;
import com.prtech.svarog.SvConf;
import com.prtech.svarog_common.ITriglavPlugin;

/**
 * A custom implementation of the Jersey ModelProcessor. This implementation
 * generates new webservices at webserver startup time based on .jar files
 * located in triglav.plugin_path property of svarog.properties. If it does not
 * find the a properly of that type it uses the current location of the deployed
 * class and finds the plugin folder. The jar files can contain either simple
 * java classes which will default to the GET httpMethod or classes that
 * implement the ITriglavPluginClass interface in order to access the more
 * advanced functionalities for generating dynamic webservices.
 *
 * @author Gjorgji Pejov
 * @see ITriglavPlugin
 * @see org.glassfish.jersey.server.model.ModelProcessor
 *
 */
@Provider
public class SvarogPluginExtender implements ModelProcessor {

	/**
	 * Log4j instance used for logging
	 */
	static final Logger log4j = LogManager.getLogger(SvarogPluginExtender.class.getName());

	/**
	 * A method that returns the new resourceModel after it does updates to it.
	 * The method scans for jars in a specific path from the svarog config file
	 * and loads each jar and generates webservices for it
	 *
	 * @return ResourceModel the new resource model
	 */
	@Override
	public ResourceModel processResourceModel(ResourceModel resourceModel, Configuration configuration) {

		ResourceModel.Builder newResourceModelBuilder = new ResourceModel.Builder(false);

		String printParam = SvConf.getParam("triglav.plugin_path");
		// get the current location of the class
		URL location = SvarogPluginExtender.class.getProtectionDomain().getCodeSource().getLocation();

		// String sep = System.getProperty("file.separator");
		String path = "";
		try {
			path = java.net.URLDecoder.decode(location.getPath()
					.replaceAll("WEB-INF/classes/com/prtech/triglav_plugins/SvarogPluginExtender.class", ""), "UTF-8");
			path += "plugins/";
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// if we find a configured location in the properties file use that
		if (printParam != null)
			path = printParam;

		File file = new File(path);
		if (!file.exists())
		{
			log4j.error("Plugin folder not found");
			return null;
		}
		// List all the jars found in the folder
		File[] listOfJars = file.listFiles();
		for (File f : listOfJars) {
			if (f.isFile() && f.getName().endsWith(".jar")) {
				try {
					log4j.info("Loading jar: "+f.getName());
					// for every jar generate the list of webservices
					List<Resource.Builder> dynBuilt = buildJar(f);
					for (Resource.Builder res : dynBuilt) {
						// add the webservices to our main model

						newResourceModelBuilder.addResource(res.build());
					}
				} catch (Exception e) {
					log4j.error("Could not load jar for "+f.getName(), e);
				}
			}

		}
		for (Resource rs : resourceModel.getResources()) {
			newResourceModelBuilder.addResource(rs);
		}
		final ResourceModel newResourceModel = newResourceModelBuilder.build();
		// and we return new model

		return newResourceModel;
	};

	/**
	 * A method that builds webservices for all the classes in the jar that
	 * satisfy certain conditions
	 *
	 * @param jrf
	 *            the jarfile for which to build the webservices. The method
	 *            scans for .class files and loads them for processing.
	 * @return List<Resource.Builder> the built webservices
	 */
	private List<Resource.Builder> buildJar(File mainjrf) throws Exception {
		List<Resource.Builder> listClasses = new ArrayList<>();
		SvClassLoader svcl = SvClassLoader.getInstance();
		JarFile jrf = new JarFile(mainjrf);
		byte[] data = Files.toByteArray(mainjrf);
		svcl.loadJar(data, "WS-" + jrf.getName());
		// iterate through the elements of the jar
		try {
		  for (Enumeration<JarEntry> entries = jrf.entries(); entries.hasMoreElements();) {
		  	JarEntry je = entries.nextElement();
			if (je.isDirectory() || !je.getName().endsWith(".class")) {
		  		continue;
		  	}
			// Build the webservice from the class
		  	Resource.Builder rsb = buildWebService(je,svcl);
		  	// add the generated webservice to the list that we will return.
	  		// NULL will be return if the system could not generate a
	  		// webservice for that class
		  		if (rsb != null)
		  			listClasses.add(rsb);
		  	
		  }
		
			
        } finally {
          jrf.close();
        }
		jrf.close();

		return listClasses;

	}
	/**
	 * A method that builds a webservice for a given class
	 *
	 * @param je
	 *            the jarentry for which to build the webservice. 
	 * @param svcl
	 *            the classLoader used to load the class
	 * @return Resource.Builder the built webservice
	 */
	private Resource.Builder buildWebService(JarEntry je, SvClassLoader svcl ) throws Exception {
		// if the jarentry is a directory or NOT a class file skip it
		Resource.Builder rsb = null;
	
	  	// get the instance of the SvarogClassLoader

	  	// parse the name of the jarentry to get its classname
	  	String jarClass = je.getName().substring(0, je.getName().length() - 6);
	  	jarClass = jarClass.replace('/', '.');

	  	// Load the class into our classloader
	  	Class c = svcl.loadClass(jarClass);

	  	if (!c.isAnonymousClass()) {
	  		// build a websvice for our class
	  	     rsb = buildClass(c);
	  		
	  	}
	  	return rsb;
	}
	
	

	/**
	 * A method that builds a webservice for a given class. It either builds
	 * default webservices which are GET webservices that produce JSON or in
	 * case the class implements ITriglavPluginClass builds the webservice
	 * according to the configuration given
	 *
	 * @param Class
	 *            the class for which we need to scan the methods and generate
	 *            webservices
	 * @return Resource.Builder the built webservice
	 */
	private Resource.Builder buildClass(Class<?> clazz) throws Exception {
		ResourceConfig newRes = new ResourceConfig(clazz);
		String wsName = "";
		LinkedHashMap<String, String> wsMethodNames = new LinkedHashMap<>();
		LinkedHashMap<String, String> wsMethodProds = new LinkedHashMap<>();
		LinkedHashMap<String, String> wsMethodCons = new LinkedHashMap<>();
		LinkedHashMap<String, String> wsMethodHttpMethods = new LinkedHashMap<>();
		List<Method> ignoreMethods = new ArrayList<Method>();
		String classType = "DEFAULT";
		Integer counter = 0;

		Resource.Builder main = Resource.builder();
		// first check if we have already annotations
		if (clazz.getAnnotation(Path.class) != null) {
			log4j.info("Found path annotation for " + clazz.getCanonicalName());
			classType = "ANNOTATION";
			wsName = clazz.getAnnotation(Path.class).value();
		} else if
		// if our class implements ITriglavPluginClass use the inherited methods
		// to get the additional information
		// for the methods in this class
		(ITriglavPlugin.class.isAssignableFrom(clazz)) {
			classType = "INTERFACE";
			ITriglavPlugin plc = (ITriglavPlugin) clazz.newInstance();
			wsName = plc.getWebServiceName();
			wsMethodNames = plc.getMethodWebServiceNames();
			wsMethodProds = plc.getMethodProduces();
			wsMethodCons = plc.getMethodConsumes();
			wsMethodHttpMethods = plc.getMethodHttpMethods();
			// add the inherited methods as methods for which we should not
			// generate webservices
			ignoreMethods = Arrays.asList(ITriglavPlugin.class.getDeclaredMethods());

		}
		if (classType.equals("ANNOTATION")) {
			main = Resource.builder(clazz);
			counter++;
		} else {
			// get name which will be user for building the path of the
			// webservice.
			// If the name is not found in the custom config
			// THIS IS THE NAME THAT WILL BE USED FOR GENERATING THE PATH !!!
			String representationName = !wsName.equals("") ? wsName : clazz.getName();
			if (!representationName.startsWith("/")) {
				representationName = "/" + representationName;
			}
			//System.out.println(representationName);
			// set the main path for all the generated webservices. All the
			// webservices will be appended to this main path.
			main.path(representationName);
			// Iterate through the class methods and add each method found as a
			// resource to the main path.
			Method[] listMethods = clazz.getDeclaredMethods();

			for (Method currMethod : listMethods) {
				// Check if the method is PUBLIC, NOT FINAL , NOT NATIVE and NOT
				// an
				// INTERFACE as well as check that it
				// returns a value of Response type
				if (Modifier.isPublic(currMethod.getModifiers()) && !Modifier.isFinal(currMethod.getModifiers())
						&& !Modifier.isNative(currMethod.getModifiers())
						&& !Modifier.isInterface(currMethod.getModifiers())
						&& currMethod.getReturnType() == Response.class)

				{

					Boolean ignore = false;
					// if the method is one of the ones we need to ignore ...
					// ignore
					// it
					for (Method ignoreMethodLocal : ignoreMethods) {
						if (ignoreMethodLocal.getName().equals(currMethod.getName())) {
							ignore = true;
							break;
						}
					}
					if (!ignore) {
						String webServiceName = null;
						String declaredMethodName = currMethod.getName();
						String methodProduce = null;
						String methodConsume = null;
						String methodHttpType = null;
						// Get the declared webServiceName from config
						if (wsMethodNames.get(declaredMethodName) != null)
							webServiceName = wsMethodNames.get(declaredMethodName);
						else
							webServiceName = declaredMethodName;
						System.out.println(webServiceName);
						// Get the declared webService Produces from config
						if (wsMethodProds.get(webServiceName) != null)
							methodProduce = wsMethodProds.get(webServiceName).toString();
						else
							methodProduce = MediaType.APPLICATION_JSON;
						System.out.println(methodProduce);
						// Get the declared webService Consumes from config
						if (wsMethodCons.get(webServiceName) != null)
							methodConsume = wsMethodCons.get(webServiceName).toString();
						System.out.println(methodConsume);
						// Get the declared webService MethodType from config
						if (wsMethodHttpMethods.get(webServiceName) != null)
							methodHttpType = wsMethodHttpMethods.get(webServiceName);
						else
							methodHttpType = HttpMethod.GET;
						System.out.println(methodHttpType);
						// generate the webservice for the method with the
						// configuration from above

						addChildResource(main, clazz, currMethod, webServiceName, methodProduce, methodConsume,
								methodHttpType);
						counter++;
					}
				}
			}
		}
		if (counter > 0)
		{
			log4j.info("Webservice built:"+ wsName);
			return main;
		}
		else
			return null;
		
	}

	/**
	 * A method that builds a webservice for a given method. In case the method
	 * is to be a POST webservice it tries to find the method parameters it
	 * needs to map the inputstream from the request to. For MethodConsume
	 * MediaType.APPLICATION_FORM_URLENCODED it tries to find a hashmap class in
	 * the method and in case of MediaType.APPLICATION_JSON it tries to find a
	 * JsonObject. If the methodConsume is something else it tries to send the
	 * InputStream directly so it can be handled by the method itself
	 *
	 * @param parent
	 *            the main Builder to which we append the new webservice
	 * @param clazz
	 *            The class on which we are currently building webservices
	 * @param inputMethod
	 *            The method for which we are currently building the webservice
	 * @param webServiceName
	 *            the name which we need to give to the new webservice
	 * @param methodProduce
	 *            the Produces option that we need to assign to the new
	 *            webservice
	 * @param methodConsume
	 *            the Consumes option that we need to assign to the new
	 *            webservice
	 * @param methodHttpType
	 *            the httpMethod type option that we need to assign to the new
	 *            webservice
	 * @return Resource.Builder the built webservice for the method
	 */
	private void addChildResource(Resource.Builder parent, final Class clazz,
			final java.lang.reflect.Method inputMethod, String webServiceName, String methodProduce,
			String methodConsume, String methodHttpType) throws Exception {

		String path = "/" + webServiceName;
		Class parClassForPost = null;
		Parameter temp = null;
		// If the httptype is post and we have a CONSUME property configured
		// search for a specific method parameter
		// in which to put the POST data from the request
		if (methodHttpType.equals("POST") && methodConsume != null && !methodConsume.equals("")) {
			if (methodConsume.equals(MediaType.APPLICATION_FORM_URLENCODED))
				parClassForPost = HashMap.class;
			else if (methodConsume.equals(MediaType.APPLICATION_JSON))
				parClassForPost = JsonObject.class;
			else
				parClassForPost = InputStream.class;
		}
		// Iterate through the parameters and generate a path for the future
		// webservice
		Parameter[] params = inputMethod.getParameters();
		for (Parameter currParam : params) {
			if (currParam.getType() != parClassForPost) {
				String parName = currParam.getName();
				path += "/{" + parName + "}";
			} else {
				if (temp != null) {
					throw new Exception("Could not generate webservice for metehod " + webServiceName);
				}
				temp = currParam;
			}
		}
		final Parameter paramForPost = temp;
		System.out.println(path);

		Resource.Builder childResource = parent.addChildResource(path);
		ResourceMethod.Builder methodResource = childResource.addMethod(methodHttpType)
				.handledBy(generateInflector(inputMethod, clazz, paramForPost)).produces(methodProduce).extended(true);
		if (methodConsume != null && methodConsume.length() > 0) {
			methodResource.consumes(methodConsume);
		}

	}

	/**
	 * A method that builds a webservice for a given method. In case the method
	 * is to be a POST webservice it tries to find the method parameters it
	 * needs to map the inputstream from the request to. For MethodConsume
	 * MediaType.APPLICATION_FORM_URLENCODED it tries to find a hashmap class in
	 * the method and in case of MediaType.APPLICATION_JSON it tries to find a
	 * JsonObject. If the methodConsume is something else it tries to send the
	 * InputStream directly so it can be handled by the method itself
	 *
	 * @param parent
	 *            the main Builder to which we append the new webservice
	 * @param clazz
	 *            The class on which we are currently building webservices
	 * @param inputMethod
	 *            The method for which we are currently building the webservice
	 * @param webServiceName
	 *            the name which we need to give to the new webservice
	 * @param methodProduce
	 *            the Produces option that we need to assign to the new
	 *            webservice
	 * @param methodConsume
	 *            the Consumes option that we need to assign to the new
	 *            webservice
	 * @param methodHttpType
	 *            the httpMethod type option that we need to assign to the new
	 *            webservice
	 * @return Resource.Builder the built webservice for the method
	 */
	private void addChildResourceFromAnnotation(Resource.Builder parent, final Class clazz,
			final java.lang.reflect.Method inputMethod) throws Exception {

		String path = "";
		String methodHttpType = "";
		List<String> methodProduce = new ArrayList<String>();
		List<String> methodConsume = new ArrayList<String>();

		for (Annotation ann : inputMethod.getAnnotations()) {
			Class antype = ann.annotationType();
			if (antype.equals(javax.ws.rs.Path.class))
				path = inputMethod.getAnnotation(javax.ws.rs.Path.class).value();
			if (antype.equals(javax.ws.rs.Produces.class))
				methodProduce = Arrays.asList(inputMethod.getAnnotation(javax.ws.rs.Produces.class).value());
			if (antype.equals(javax.ws.rs.Consumes.class))
				methodConsume = Arrays.asList(inputMethod.getAnnotation(javax.ws.rs.Consumes.class).value());
			if (javax.ws.rs.HttpMethod.class.isAssignableFrom(antype))
				methodHttpType = inputMethod.getAnnotation(javax.ws.rs.HttpMethod.class).value();

		}
		Resource.Builder childResource = parent.addChildResource(path);
		ResourceMethod.Builder methodResource = childResource.addMethod(methodHttpType)
				.handledBy(generateInflector(inputMethod, clazz, null));

	}

	/**
	 * A method that builds a the inflector ( the method that will be called
	 * when the webservice gets a hit ). The inflector must return a value of
	 * type Response which means that all methods that will be webservice'd need
	 * to return a value of type Response.
	 *
	 * @param clazz
	 *            The class on which we are currently building webservices
	 * @param inputMethod
	 *            The method for which we are currently building the webservice
	 * @param paramForPost
	 *            the parameter which will we used to send the inputStream data
	 *            from the request, either as a JsonObject,HashMap or a pure
	 *            InputStream.
	 * @return Inflector<ContainerRequestContext, Response> the built inflector
	 */
	public Inflector<ContainerRequestContext, Response> generateInflector(Method inputMethod, Class clazz,
			Parameter paramForPost) {
		return new Inflector<ContainerRequestContext, Response>() {
			@Override
			public Response apply(ContainerRequestContext containerRequestContext) {
				Response retval = null;
				MultivaluedMap<String, String> params = containerRequestContext.getUriInfo().getQueryParameters();
				Request req = containerRequestContext.getRequest();

				MultivaluedMap<String, String> pathp = containerRequestContext.getUriInfo().getPathParameters();
				Set<String> keysetFromPath = pathp.keySet();
				Integer argSize = keysetFromPath.size();
				Parameter[] localParams = inputMethod.getParameters();

				System.out.println(inputMethod.getName());
				System.out.println(clazz.getName());
				if (inputMethod.getParameterCount() == (paramForPost != null ? argSize + 1 : argSize)) {

					try {
						Object myargs[] = new Object[paramForPost != null ? argSize + 1 : argSize];
						containerRequestContext.getEntityStream();

						Integer counter = 0;
						for (Parameter currlocalParam : localParams) {
							if (currlocalParam != paramForPost) {
								Iterator<String> keyIt = keysetFromPath.iterator();
								String httpParamWeNeed = null;
								while (keyIt.hasNext()) {

									{
										String currHttpParam = keyIt.next();

										if (currlocalParam.getName().equals(currHttpParam)) {
											httpParamWeNeed = currHttpParam;
											break;
										}
									}
								}
								if (currlocalParam != null) {
									Class<?> parTypeClass = currlocalParam.getType();
									Object objToSend = pathp.get(httpParamWeNeed).get(0);
									Object parToSend = null;
									if (parTypeClass.equals(Long.class)) {
										parToSend = new Long(objToSend.toString());
									} else {
										parToSend = parTypeClass.cast(objToSend);
									}
									myargs[counter] = parToSend;
									counter++;
								}
							} else {
								String eStream = IOUtils.toString(containerRequestContext.getEntityStream(),
										Charsets.UTF_8);
								if (paramForPost.getType().equals(HashMap.class)) {
									HashMap<String, String> query_pairs = new HashMap<String, String>();

									String[] pairs = eStream.split("&");
									for (String pair : pairs) {
										int idx = pair.indexOf("=");
										query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
												URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
									}
									myargs[counter] = query_pairs;
									counter++;

								} else

								if (paramForPost.getType().equals(JsonObject.class)) {
									Gson gson = new Gson();
									JsonObject jso = gson.fromJson(eStream, JsonObject.class);
									myargs[counter] = jso;
									counter++;
								} else if (paramForPost.getType().equals(InputStream.class)) {

									myargs[counter] = containerRequestContext.getEntityStream();
									counter++;
								}
							}
						}

						retval = (Response) inputMethod.invoke(clazz.newInstance(), myargs);
					} catch (Exception e) {
						retval = Response.status(401).entity(e.getMessage()).build();
					}
				} else {
					retval = Response.status(401).entity("WebService arguments do not match").build();
				}

				return retval;
			}
		};
	}

	@Override
	public ResourceModel processSubResource(ResourceModel subResourceModel, Configuration configuration) {
		// we just return the original subResourceModel which means we do not
		// want to do any modification
		return subResourceModel;
	}
}
