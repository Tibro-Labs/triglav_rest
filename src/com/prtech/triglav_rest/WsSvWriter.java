package com.prtech.triglav_rest;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.prtech.svarog.SvException;
import com.prtech.svarog.SvWriter;
import com.prtech.svarog_common.DbDataObject;

/***
 * Web service for writing data it gives all public methods of class SvWriter
 * access via web service
 * 
 * @authorDimitar Sazdov
 *
 */

@Path("/svWriter")

public class WsSvWriter {

	/**
	 * Web service version of SvWriter.saveObject
	 * 
	 * A method that saves a JSON object to the Database. If the JSON object
	 * contains an Array of JSON Objects, each of them will be saved in the
	 * database using the same transaction. If saving one of the objects fails,
	 * the whole object will be rolled back.
	 * 
	 * @param sessionId
	 *            Session ID (SID) of the web communication between browser and
	 *            web server
	 * @param jsonString
	 *            standard serialized object of any type
	 * 
	 * @return true if object was saved with no errors
	 */
	@Path("/saveObject/{sessionId}/{jsonString}")
	@POST
	@Produces("application/json")
	public Response saveObject(@PathParam("sessionId") String sessionId, @PathParam("jsonString") String jsonString,
			@Context HttpServletRequest httpRequest)

	{
		String returnString = new String();
		SvWriter svw = null;
		JsonObject jsonObj = null;
		try {

			Gson gson = new Gson();
			jsonObj = gson.fromJson(jsonString, JsonObject.class);
			svw = new SvWriter(sessionId);
			svw.saveObject(jsonObj);
			returnString = "true";
		} catch (SvException e) {
			e.printStackTrace();
			return Response.status(401).entity(e.getFormattedMessage()).build();
		} finally {
			svw.release();
		}
		return Response.status(200).entity(returnString).build();
	}

	

	/**
	 * Web service version of SvWriter.saveObjectsArray
	 * 
	 * fail try to get array of objects and save them
	 * 
	 * @param sessionId
	 *            Session ID (SID) of the web communication between browser and
	 *            web server
	 * @param jsonString
	 *            standard serialized objects of any type
	 * 
	 * @return true if object was saved with no errors
	 */	
	@Path("/saveObjectsArray/{sessionId}/{jsonString}")
	@POST
	@Produces("application/json")
	public Response saveObjectsArray(@PathParam("sessionId") String sessionId,
			@PathParam("jsonString") String jsonString, @Context HttpServletRequest httpRequest)

	{
		String returnString = "does not work";
		/*
		 * SvWriter svw = null; JsonObject jsonObj = null; DbDataArray array
		 * =new DbDataArray(); try { Gson gson = new Gson(); //JsonObject json1
		 * = (JsonObject)new JsonParser().parse(jsonString); jsonObj =
		 * gson.fromJson(jsonString, JsonObject.class); // array =
		 * gson.fromJson(jsonString, DbDataArray.class); svw = new
		 * SvWriter(sessionId); //svw.saveObject(array); returnString="saved"; }
		 * catch (SvException e) { e.printStackTrace(); return
		 * Response.status(401).entity(e.getFormattedMessage()).build(); }
		 */
		return Response.status(200).entity(returnString).build();
	}

	

	/**
	 * Web service version of SvWriter.deleteObject
	 * we can delete the object by calling this and using the same Json that we used in SaveObject()
	 * 
	 * @param sessionId
	 *            Session ID (SID) of the web communication between browser and
	 *            web server
	 * @param jsonString
	 *            standard serialized object of any type
	 * 
	 * @return true if object was deleted with no errors
	 */	
	@Path("/deleteObject/{sessionId}/{jsonString}")
	@POST
	@Produces("application/json")
	public Response deleteObject(@PathParam("sessionId") String sessionId, @PathParam("jsonString") String jsonString,
			@Context HttpServletRequest httpRequest)

	{
		String returnString = new String();
		SvWriter svw = null;
		JsonObject jsonObj = null;
		DbDataObject vobject = new DbDataObject();
		try {
			Gson gson = new Gson();
			jsonObj = gson.fromJson(jsonString, JsonObject.class);
			svw = new SvWriter(sessionId);
			vobject.fromJson(jsonObj);
			svw.deleteObject(vobject);
			returnString = "true";
		} catch (SvException e) {
			e.printStackTrace();
			return Response.status(401).entity(e.getFormattedMessage()).build();
		} finally {
			svw.release();
		}
		return Response.status(200).entity(returnString).build();
	}

	

	/**
	 * Web service version of SvWriter.deleteObject
	 * delete the object with given ID, type and PKid (version)
	 * if there is greater PKid (same object changed by other user) object should not be deleted
	 * 
	 * @param sessionId
	 *            Session ID (SID) of the web communication between browser and
	 *            web server
	 * @param objectId
	 *            ID of the object that we want to delete
	 * @param objectType
	 *            type of the Object that we try to delete
	 * @param objectPkId
	 *            PkId (version) of the object that we have
	 * 
	 * @return true if object was deleted with no errors
	 */		
	@Path("/deleteObject/{sessionId}/{objectId}/{objectType}/{objectPkId}")
	@POST
	@Produces("application/json")
	public Response deleteObject(@PathParam("sessionId") String sessionId, @PathParam("objectId") Long objectId,
			@PathParam("objectType") Long objectType, @PathParam("objectPkId") Long objectPkId,
			@Context HttpServletRequest httpRequest) {
		String returnString = "";
		SvWriter svw = null;
		DbDataObject vobject = new DbDataObject();
		try {
			vobject.setObject_id(objectId);
			vobject.setObject_type(objectType);
			vobject.setPkid(objectPkId);
			svw = new SvWriter(sessionId);
			svw.deleteObject(vobject);
			returnString = "true";
		} catch (SvException e) {
			e.printStackTrace();
			return Response.status(401).entity(e.getFormattedMessage()).build();
		} finally {
			svw.release();
		}
		return Response.status(200).entity(returnString).build();
	}

	

	/**
	 * Web service version of SvWriter.deleteObjectsByParent
	 * find all child objects of type childrenObjectType that have parent ID of objectId and delete them
	 * 
	 * !!! not tested !!!
	 * 
	 * @param sessionId
	 *            Session ID (SID) of the web communication between browser and
	 *            web server
	 * @param objectId
	 *            ID of the object that we want to delete his children
	 * @param objectType
	 *            type of the Object that we try to delete his chilren
	 * @param childrenObjectType
	 *            when is the type of children objects that we try to delete
	 * 
	 * @return true if object was deleted with no errors
	 */			
	@Path("/deleteObjectsByParent/{sessionId}/{objectId}/{objectType}/{childrenObjectType}")
	@POST
	@Produces("application/json")
	public Response deleteObjectsByParent(@PathParam("sessionId") String sessionId,
			@PathParam("objectId") Long objectId, @PathParam("objectType") Long objectType,
			@PathParam("childrenObjectType") Long childrenObjectType, @Context HttpServletRequest httpRequest) {
		String returnString = new String();
		SvWriter svw = null;
		DbDataObject vobject = new DbDataObject();
		try {
			/*
			 * SvReader svr = new SvReader(sessionId); DateTime refDate =
			 * DateTime.now(); vobject = svr.getObjectById(objectId, objectType,
			 * refDate);
			 */
			vobject.setObject_id(objectId);
			vobject.setObject_type(objectType);
			svw = new SvWriter(sessionId);
			// svw.deleteObjectsByParent(vobject,childrenObjectType);
			returnString = "children deleted (not tested, not deleted) ";
		} catch (SvException e) {
			e.printStackTrace();
			return Response.status(401).entity(e.getFormattedMessage()).build();
		} finally {
			svw.release();
		}
		return Response.status(200).entity(returnString).build();
	}

}
