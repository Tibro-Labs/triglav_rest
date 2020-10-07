package com.prtech.triglav_rest;

/*import static org.junit.Assert.fail; */

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
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
import com.prtech.svarog.SvCore;
import com.prtech.svarog.SvException;
import com.prtech.svarog.SvReader;
import com.prtech.svarog_common.DbDataArray;
import com.prtech.svarog_common.DbDataObject;
import com.prtech.svarog_common.DbSearchExpression;


/***
 * Web service for reading data
 * it gives all public methods of class SvReader access via web service
 * 
 * @author Dimitar Sazdov
 *
 */


@Path("/svReader")
public class WsSvReader {

/**
 * Web service version of  SvReader.getObjects
 * that can read objects according to standardized dbSearch criteria
 * 
 * @param sessionId
 *           Session ID (SID) of the web communication between browser and web server
 * @param dbSearch
 *           standard serialized query of the search criteria , since some characters can generate error in path of URL, character % should be replaced with __5__   
 * @param typeDescriptor
 *           Type of the Object that we are searching for 
 * @param refDateString
 *           standard joda date-time as string, get only objects that were valid at this date
 * @param rowLimit
 *           How many items we want per page                      
 * @param offset
 *           Page number 0+  
 *                
 * @return Json Array of objects containing values of the search criteria
 */
	@Path("/getObjects/{sessionId}/{dbSearch}/{typeDescriptor}/{refDateString}/{rowLimit}/{offset}")
	@GET
	@Produces("application/json")
	public Response getUsers(@PathParam("sessionId") String sessionId, @PathParam("dbSearch") String dbSearch,
			@PathParam("typeDescriptor") Long typeDescriptor, @PathParam("refDateString") String refDateString,
			@PathParam("rowLimit") Integer rowLimit, @PathParam("offset") Integer offset,
			@Context HttpServletRequest httpRequest) {
		String unscrambledDbSerach = dbSearch;
		unscrambledDbSerach = unscrambledDbSerach.replaceAll("__5__", "%");
		SvReader svr = null;
		String vretvalString = new String();
		DbDataArray array = null;
		Integer vrowLimit = rowLimit;
		if (vrowLimit <=0 ) vrowLimit=20;
		if (vrowLimit >50 ) vrowLimit=50;
		Integer voffset = offset;
		if (voffset <=0 ) voffset=0;
		try {
			svr = new SvReader(sessionId);
			Gson gsonSearch = new Gson();
			JsonObject searchedString = gsonSearch.fromJson(unscrambledDbSerach, JsonObject.class);
			DbSearchExpression dbsearchex = new DbSearchExpression();
			dbsearchex.fromJson(searchedString);
			DateTime refDate = new DateTime(refDateString);
			JsonObject jsObj = null;
			JsonObject exParams = null;
			array = svr.getObjects(dbsearchex, typeDescriptor, refDate, vrowLimit, voffset);
			DbDataArray fields = SvReader.getFields(typeDescriptor);
			CodeList cl = new CodeList(svr);
			jsObj = fields.getMembersJson().getTabularJson("", array, null, fields, exParams, cl);
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			vretvalString = gson.toJson(jsObj);
			//System.out.println(vretvalString);
		} catch (SvException e) {
			e.printStackTrace();
			return Response.status(401).entity(e.getFormattedMessage()).build();
		} finally {
			svr.release();
		}
		return Response.status(200).entity(vretvalString).build();
	}

	/*
	 * Gson gsonSearch = new Gson(); JsonObject searchedString =
	 * gsonSearch.fromJson(dbSearchString,JsonObject.class); DbSearchExpression
	 * dbsearch = null; dbsearch.fromJson(searchedString);
	 */

	

/**
 * Web service version of  SvReader.getObjectById
 * that can read objects from ID and type of object 
 * 
 * @param sessionId
 *           Session ID (SID) of the web communication between browser and web server
 * @param objectId
 *           ID of the object that we like to get  
 * @param objectName
 *           Type of the Object that we are searching for 
 * @param refDateString
 *           standard joda date-time as string, get only objects that were valid at this date
 *           
 * @return  object and values for that object in Json format
 */	
	@Path("/getObjectById/{sessionId}/{objectId}/{objectName}/{refDateString}")
	@GET
	@Produces("application/json")
	public Response getObjectById(@PathParam("sessionId") String sessionId, @PathParam("objectId") Long objectId,
			@PathParam("objectName") String objectName, @PathParam("refDateString") String refDateString,
			@Context HttpServletRequest httpRequest) {
		String vretvalString = new String();
		SvReader svr = null;
		try {
			svr = new SvReader(sessionId);
			DateTime refDate = new DateTime(refDateString);
			DbDataObject dbt = SvCore.getDbtByName(objectName);
			DbDataObject vobject = svr.getObjectById(objectId, dbt.getObject_id(), refDate);
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			JsonObject jso = vobject.toJson();
			vretvalString = gson.toJson(jso); // kako da konvertirash object vo json
		} catch (SvException e) {
			e.printStackTrace();
			return Response.status(401).entity(e.getFormattedMessage()).build();
		} finally {
			svr.release();
		}
		return Response.status(200).entity(vretvalString).build();
	}
	
	

/**
 * Web service version of  SvReader.getObjectsByLinkedId
 * that can read objects that are linked to single object via specified link type
 * 
 * @param sessionId
 *           Session ID (SID) of the web communication between browser and web server
 * @param objectId1
 *           ID of the Object for which we like to get all linked objects 
 * @param objectName1
 *           type of the Object for which we like to get all linked objects (table name)
 * @param linkType
 *           Object ID of link type between two types of objects ( object type OBJECT_TYPE_LINK_TYPE )
 * @param objectName2
 *           type of the Object that is linked to Object specified with objectId1
 * @param isReverse         
 *           false for standard link types, true for those that are created reverse by error                  
 * @param refDateString
 *           standard joda date-time as string, get only objects that were valid at this date
 * @param rowLimit
 *           How many items we want per page
 * @param offset
 *           Page number 0+  
 *                
 * @return Json Array of objects linked to object with given ID
 */
	@Path("/getObjectByLinkedId/{sessionId}/{objectId1}/{objectName1}/{linkType}/{objectName2}/{isReverse}/{refDateString}/{rowLimit}/{offset}")
	@GET
	@Produces("application/json")
	public Response getObjectsByLinkedId(@PathParam("sessionId") String sessionId,
			@PathParam("objectId1") Long objectId1, @PathParam("objectName1") String objectName1,
			@PathParam("linkType") Long linkType, @PathParam("objectName2") String objectName2,
			@PathParam("isReverse") Boolean isReverse, @PathParam("refDateString") String refDateString,
			@PathParam("rowLimit") Integer rowLimit, @PathParam("offset") Integer offset,
			@Context HttpServletRequest httpRequest) {
		return getObjectsByLinkedId(sessionId, objectId1, objectName1, linkType, objectName2, isReverse, refDateString,
				rowLimit, offset, null, httpRequest);
	}



/**
 * Web service version of  SvReader.getObjectsByLinkedId
 * that can read objects that are linked to single object via specified link type
 * 
 * @param sessionId
 *           Session ID (SID) of the web communication between browser and web server
 * @param objectId1
 *           ID of the Object for which we like to get all linked objects 
 * @param linkId
 *           Object ID of link type between two types of objects ( object type OBJECT_TYPE_LINK_TYPE )
 * @param refDateString
 *           standard joda date-time as string, get only objects that were valid at this date
 * @param rowLimit
 *           How many items we want per page
 * @param offset
 *           Page number 0+  
 *                
 * @return Json Array of objects linked to object with given ID
 */	
	@Path("/getObjectByLinkedId/{sessionId}/{objectId1}/{linkId}/{refDateString}/{rowLimit}/{offset}")
	@GET
	@Produces("application/json")
	public Response getObjectsByLinkedId(@PathParam("sessionId") String sessionId,
			@PathParam("objectId1") Long objectId1,	@PathParam("linkId") Long linkId,
			@PathParam("refDateString") String refDateString, @PathParam("rowLimit") Integer rowLimit,
			@PathParam("offset") Integer offset, @Context HttpServletRequest httpRequest) {
		String vretvalString = new String();
		SvReader svr = null;
		try {
			DateTime refDate = new DateTime(refDateString);
			svr = new SvReader(sessionId);
			Long linkTypeId = SvCore.getTypeIdByName("LINK_TYPE");
			DbDataObject dbLink = svr.getObjectById(linkId, linkTypeId, refDate);
			long object2Id = (long) dbLink.getVal("link_obj_type_2");
			DbDataArray array2 =new DbDataArray();
			JsonObject jsObj = null;
			JsonObject exParams = null;
			CodeList cl = new CodeList(svr);
			DbDataArray array  = svr.getObjectsByLinkedId(objectId1, dbLink, refDate, rowLimit, offset);
			if (array.getItems().size()>0) {
				DbDataObject object1 = svr.getObjectById(array.getItems().get(0).getObject_id(),object2Id , refDate);
				 array2.addDataItem(object1);
				 jsObj = array.getMembersJson().getTabularJson("", array2, null, array, exParams, cl);
			}
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			vretvalString = gson.toJson(jsObj);
		} catch (SvException e) {
			e.printStackTrace();
			return Response.status(401).entity(e.getFormattedMessage()).build();
		} finally {
			svr.release();
		}
		return Response.status(200).entity(vretvalString).build();
	}



/**
 * Web service version of  SvReader.getObjectsByLinkedId
 * that can read objects that are linked to single object via specified link type
 * 
 * @param sessionId
 *           Session ID (SID) of the web communication between browser and web server
 * @param objectId1
 *           ID of the Object for which we like to get all linked objects 
 * @param objectName1
 *           type of the Object for which we like to get all linked objects (table name)
 * @param linkType
 *           Object ID of link type between two types of objects ( object type OBJECT_TYPE_LINK_TYPE )
 * @param objectName2
 *           type of the Object that is linked to Object specified with objectId1
 * @param isReverse         
 *           false for standard link types, true for those that are created reverse by error                  
 * @param refDateString
 *           standard joda date-time as string, get only objects that were valid at this date
 * @param rowLimit
 *           How many items we want per page
 * @param offset
 *           Page number 0+  
 * @param sortField          
 *           Name of the field of type "objectName2" that we want to sort by (asc only)     
 * @return Json Array of objects linked to object with given ID
 */
	@Path("/getObjectByLinkedId/{sessionId}/{objectId1}/{objectName1}/{linkType}/{objectName2}/{isReverse}/{refDateString}/{rowLimit}/{offset}/{sortField}")
	@GET
	@Produces("application/json")
	public Response getObjectsByLinkedId(@PathParam("sessionId") String sessionId,
			@PathParam("objectId1") Long objectId1, @PathParam("objectName1") String objectName1,
			@PathParam("linkType") Long linkType,@PathParam("objectName2") String objectName2,
			@PathParam("isReverse") Boolean isReverse,
			@PathParam("refDateString") String refDateString, @PathParam("rowLimit") Integer rowLimit,
			@PathParam("offset") Integer offset, @PathParam("sortField") String sortField, 
			@Context HttpServletRequest httpRequest) {
		String vretvalString = new String();
		DateTime refDate = new DateTime(refDateString);
		SvReader svr = null;
		Long linkTypeId = SvCore.getTypeIdByName("LINK_TYPE");
		DbDataObject vobject= new DbDataObject();
		try {
			svr = new SvReader(sessionId);
			vobject = svr.getObjectById(linkType, linkTypeId, refDate);
			DbDataArray array = new DbDataArray();
			Long vobjectId1 = SvCore.getTypeIdByName(objectName1);
			Long vobjectId2 = SvCore.getTypeIdByName(objectName2);
			DbDataArray fields1 = svr.getObjectsByLinkedId(objectId1, vobjectId1, vobject, vobjectId2, isReverse,
					refDate, rowLimit, offset, sortField);
			JsonObject jsObj = new JsonObject();
			JsonObject exParams = null;
			CodeList cl = new CodeList(svr);
			if (fields1.getItems().size() > 0) {
				DbDataObject object1 = svr.getObjectById(fields1.getItems().get(0).getObject_id(), vobjectId2, refDate);
				array.addDataItem(object1);
				jsObj = array.getMembersJson().getTabularJson("", fields1, null, array, exParams, cl);
			}
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			vretvalString = gson.toJson(jsObj);
		} catch (SvException e) {
			e.printStackTrace();
			return Response.status(401).entity(e.getFormattedMessage()).build();
		} finally {
			svr.release();
		}
		return Response.status(200).entity(vretvalString).build();
	}
	
	
	

/**
 * Web service version of  SvReader.getObjectsByParentId
 * that can return objects of objectType that are children to object with ID parentId
 * 
 * @param sessionId
 *           Session ID (SID) of the web communication between browser and web server
 * @param parentId
 *           ID of the Object for which we like to get all children objects
 * @param objectType
 *           Id of the type of the child objects
 * @param refDateString
 *           standard joda date-time as string, get only objects that were valid at this date
 * @param rowLimit
 *           How many items we want per page
 * @param offset
 *           Page number 0+  
 *                
 * @return Json Array of objects of type objectType, children of object with ID parentId
 */	
	@Path("/getObjectsByParentId/{sessionId}/{parentId}/{objectType}/{refDateString}/{rowLimit}/{offset}")
	@GET
	@Produces("application/json")
	public Response getObjectsByParentId(@PathParam("sessionId") String sessionId,
			@PathParam("parentId") Long parentId, @PathParam("objectType") Long objectType,
			@PathParam("refDateString") String refDateString, @PathParam("rowLimit") Integer rowLimit,
			@PathParam("offset") Integer offset, @Context HttpServletRequest httpRequest) {
		return getObjectsByParentId(sessionId, parentId, objectType, refDateString, rowLimit, offset, null,
				httpRequest);
	}
	

/**
 * Web service version of  SvReader.getObjectsByParentId
 * that can return objects of objectType that are children to object with ID parentId
 * 
 * @param sessionId
 *           Session ID (SID) of the web communication between browser and web server
 * @param parentId
 *           ID of the Object for which we like to get all children objects
 * @param objectType
 *           Id of the type of the child objects
 * @param refDateString
 *           standard joda date-time as string, get only objects that were valid at this date
 * @param rowLimit
 *           How many items we want per page
 * @param offset
 *           Page number 0+  
 * @param sortField          
 *           Name of the field of type "objectType" that we want to sort by (asc only)  
 *           
 * @return Json Array of objects of type objectType, children of object with ID parentId
 */	
	@Path("/getObjectsByParentId/{sessionId}/{parentId}/{objectType}/{refDateString}/{rowLimit}/{offset}/{sortByField}")
	@GET
	@Produces("application/json")
	public Response getObjectsByParentId(@PathParam("sessionId") String sessionId, @PathParam("parentId") Long parentId,
			@PathParam("objectType") Long objectType, @PathParam("refDateString") String refDateString,
			 @PathParam("rowLimit") Integer rowLimit, @PathParam("offset") Integer offset,
			 @PathParam("sortByField") String sortByField, @Context HttpServletRequest httpRequest) {
		String vretvalString = new String();
		SvReader svr = null;
		try {
			svr = new SvReader(sessionId);
			DateTime refDate = new DateTime(refDateString);
			DbDataArray array = svr.getObjectsByParentId(parentId, objectType, refDate, rowLimit, offset,sortByField);
			DbDataObject vobject = SvCore.getDbt(objectType);
			JsonObject jsObj = null;
			JsonObject exParams = null;
			CodeList cl = new CodeList(svr);
			jsObj = array.getMembersJson().getTabularJson("", vobject, null, array, exParams, cl);
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			vretvalString = gson.toJson(jsObj);
		} catch (SvException e) {
			e.printStackTrace();
			return Response.status(401).entity(e.getFormattedMessage()).build();
		} finally {
			svr.release();
		}
		return Response.status(200).entity(vretvalString).build();
	}
	
	
	public void testusers() {

	}

}
