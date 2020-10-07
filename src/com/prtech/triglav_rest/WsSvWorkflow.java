package com.prtech.triglav_rest;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.joda.time.DateTime;
import com.prtech.svarog.SvException;
import com.prtech.svarog.SvReader;
import com.prtech.svarog.SvWorkflow;
import com.prtech.svarog_common.DbDataObject;



/***
 * Web service for managing work-flow 
 * it gives all public methods of class SvWorkflow access via web service
 * 
 * @author Dimitar Sazdov
 *
 */


@Path("/SvWorkflow")


public class WsSvWorkflow {

	
	

	/**
	 * Web service version of SvWriter.moveObject
	 * it will move object with ID of objectId to the new status specified with newStatus
	 * 
	 * @param sessionId
	 *            Session ID (SID) of the web communication between browser and
	 *            web server
	 * @param objectId
	 *            ID of the object that we want to move to new status
	 * @param objectType
	 *            type of the Object that we try to move
	 * @param newStatus
	 *            Name of the new status
	 * 
	 * @return true if object was moved with no errors
	 */		
	@SuppressWarnings("null")
	@Path("/moveObject/{sessionId}/{objectId}/{objectType}/{newStatus}")
	@POST
	@Produces("application/json")
	public Response moveObject(@PathParam("sessionId") String sessionId, @PathParam("objectId") Long objectId,
			@PathParam("objectType") Long objectType, @PathParam("newStatus") String newStatus,
			@Context HttpServletRequest httpRequest)

	{
		String returnString = new String();
		SvWorkflow svwr = null;
		DbDataObject vobject = new DbDataObject();
		try {
			SvReader svr = new SvReader(sessionId);
			DateTime refDate = DateTime.now();
			vobject = svr.getObjectById(objectId, objectType, refDate);
			svwr.moveObject(vobject, newStatus);
			returnString = "objest moved to status "+newStatus ;
		} catch (SvException e) {
			e.printStackTrace();
			return Response.status(401).entity(e.getFormattedMessage()).build();
		}
		return Response.status(200).entity(returnString).build();
	}
	
	
	
	
}
