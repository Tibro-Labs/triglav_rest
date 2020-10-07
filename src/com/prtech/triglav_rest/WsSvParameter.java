package com.prtech.triglav_rest;

import java.util.ArrayList;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.joda.time.DateTime;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.prtech.svarog.CodeList;
import com.prtech.svarog.SvException;
import com.prtech.svarog.SvParameter;
import com.prtech.svarog.SvReader;
import com.prtech.svarog_common.DbDataArray;
import com.prtech.svarog_common.DbDataObject;



/***
 * Web service for managing parameters
 * it gives all public methods of class SvParameter access via web service
 * 
 * @author Dimitar Sazdov
 *
 */

@Path("/SvParameter")



/**
 * Web service version of SvParameter.getParameters
 * Method that returns parameters for the object according its config object
 * 
 * @param sessionId
 *            Session ID (SID) of the web communication between browser and web server
 * @param objectId
 *           Object ID for which we want to get parameters      
 * @param objectType
 *           Object type of the objectId parameter
 *           /OBJECT_TYPE_JOB_TYPE/      
 *                
 * @return Json Array objects containing value of parameter(s)
 */
public class WsSvParameter {
	@Path("/getParameters/{sessionId}/{objectId}/{objectType}")
	@GET
	@Produces("application/json")
	public Response getParameters(@PathParam("sessionId") String sessionId, @PathParam("objectId") Long objectId,
			@PathParam("objectType") Long objectType, @Context HttpServletRequest httpRequest) {
		String returnString = new String();
		SvReader svr = null;
		try {
			int x;
			DateTime refDate = DateTime.now();
			svr = new SvReader(sessionId);
			SvParameter svp = new SvParameter(sessionId);
			DbDataObject object1 = svr.getObjectById(objectId, objectType, refDate);
			HashMap<DbDataObject, ArrayList<DbDataObject>> allParams = new HashMap<>();
			ArrayList<DbDataObject> allValues = new ArrayList<>();
		//	allParams = svp.getParameters(object1);
			for(ArrayList<DbDataObject> arr:allParams.values())
			{
				allValues.addAll(arr); 
			}
			JsonObject jsObj = new JsonObject();
			if (allValues != null)
				if (allValues.size() >= 1) {
					JsonObject exParams = null;
					CodeList cl = new CodeList(svr);
					DbDataArray array = new DbDataArray();
					DbDataArray array2 = new DbDataArray();
					array2.addDataItem(allValues.get(0));
					for (x = 0; x < allValues.size(); x++)
						array.addDataItem(allValues.get(x));
					jsObj = array.getMembersJson().getTabularJson("", array2, null, array, exParams, cl);
				}
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			returnString = gson.toJson(jsObj);
		} catch (SvException e) {
			e.printStackTrace();
			return Response.status(401).entity(e.getFormattedMessage()).build();
		} finally {
			svr.release();
		}
		return Response.status(200).entity(returnString).build();
	}
/*
	@Path("/createParam/{sessionId}/{objectId1}/{objectType1}/{objectId2}/{objectType2}")
	@POST
	@Produces("application/json")
	public Response createParam(@PathParam("sessionId") String sessionId, @PathParam("objectId1") Long objectId1,
			@PathParam("objectType1") Long objectType1, @PathParam("objectId2") Long objectId2,
			@PathParam("objectType2") Long objectType2, @Context HttpServletRequest httpRequest) {
		String returnString = "";
		SvReader svr = null;
		try {
			DateTime refDate = DateTime.now();
			svr = new SvReader(sessionId);
			SvParameter svp = new SvParameter(sessionId);
			DbDataObject object1 = svr.getObjectById(objectId1, objectType1, refDate);
			DbDataObject object2 = svr.getObjectById(objectId2, objectType2, refDate);
			svp.generateParameters(object1, object2,null);
			returnString = "true";
		} catch (SvException e) {
			e.printStackTrace();
			return Response.status(401).entity(e.getFormattedMessage()).build();
		} finally {
			svr.release();
		}
		return Response.status(200).entity(returnString).build();
	}
*/
	
	

/**
 * Web service version of  SvParameter.createParamValue
 * created by error createParamValue should not be public
 * 
 * @param sessionId
 *            Session ID (SID) of the web communication between browser and web server
 * @param objectId
 *           Object ID for which we want to get parameters      
 * @param objectType
 *           Object type of the objectId parameter
 */
	/*
	@Path("/createParamValue/{sessionId}/{objectId}/{objectType}/{value}/{codeList}")
	@POST
	@Produces("application/json")
	public Response createParamValue(@PathParam("sessionId") String sessionId, @PathParam("objectId") Long objectId,
			@PathParam("objectType") Long objectType, @PathParam("value") String value,
			@PathParam("codeList") Long codeList, @Context HttpServletRequest httpRequest) {
		String returnString = "";
		SvReader svr = null;
		try {
			DateTime refDate = DateTime.now();
			svr = new SvReader(sessionId);
			SvParameter svp = new SvParameter(sessionId);
			//DbDataObject object1 = new DbDataObject();
			//object1.setObject_id(objectId);
			//object1.setObject_type(objectType);
			DbDataObject object1 = svr.getObjectById(objectId, objectType, refDate);
			DbDataObject object3 = svp.createParamValue(object1, value, codeList);
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			JsonObject jso = object3.toJson();
			returnString = gson.toJson(jso);
		} catch (SvException e) {
			e.printStackTrace();
			return Response.status(401).entity(e.getFormattedMessage()).build();
		} finally {
			svr.release();
		}
		return Response.status(200).entity(returnString).build();
	}
*/
	

/**
 * Web service version of  SvParameter.generateParameters
 * Method that creates parameters for the object according its config object
 * 
 * @param sessionId
 *            Session ID (SID) of the web communication between browser and web server
 * @param objectId1
 *           Object ID for which we want to generate parameters      
 * @param objectType1
 *           Object type of the objectId1 parameter
 *           /OBJECT_TYPE_JOB/      
 * @param objectId2
 *           Object ID for the object that we want to copy parameters from     
 * @param objectType2
 *           Object type of the objectId2 parameter
 *           /OBJECT_TYPE_JOB_TYPE/          
 * @return true if there are no errors and Parameters are created 
 */	
	@Path("/generateParameters/{sessionId}/{objectId1}/{objectType1}/{objectId2}/{objectType2}")
	@POST
	@Produces("application/json")
	public Response generateParameters(@PathParam("sessionId") String sessionId, @PathParam("objectId1") Long objectId1,
			@PathParam("objectType1") Long objectType1, @PathParam("objectId2") Long objectId2,
			@PathParam("objectType2") Long objectType2, @Context HttpServletRequest httpRequest) {
		String returnString = new String();
		SvReader svr = null;
		try {
			DateTime refDate = DateTime.now();
			svr = new SvReader(sessionId);
			SvParameter svp = new SvParameter(sessionId);
			DbDataObject object1 = svr.getObjectById(objectId1, objectType1, refDate);
			DbDataObject object2 = svr.getObjectById(objectId2, objectType2, refDate);
			svp.generateParameters(object1, object2, null);
			returnString = "true";
		} catch (SvException e) {
			e.printStackTrace();
			return Response.status(401).entity(e.getFormattedMessage()).build();
		} finally {
			svr.release();
		}
		return Response.status(200).entity(returnString).build();
	}

}
