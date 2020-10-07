package com.prtech.triglav_rest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.impl.common.IOUtil;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.joda.time.DateTime;

import com.prtech.svarog.SvCore;
import com.prtech.svarog.SvException;
import com.prtech.svarog.SvFileStore;
import com.prtech.svarog.SvReader;
import com.prtech.svarog.svCONST;
import com.prtech.svarog_common.DbDataArray;
import com.prtech.svarog_common.DbDataObject;
import com.prtech.svarog_common.DbSearchCriterion;
import com.prtech.svarog_common.DbSearchCriterion.DbCompareOperand;

@Path("/svFileStore")
public class WsSvFileStore {
	static final Logger log4j = LogManager.getLogger(WsSvFileStore.class.getName());

	// {sessionId}/{linkedObjectName}/{linkedObjectId}/{fileType}/

	@GET
	@Path("/getFileData/{sessionId}/{fileObjectId}/")
	public Response getFileData(@PathParam("fileObjectId") Long fileObjectId,
			@PathParam("sessionId") String sessionId) {
		
		SvReader svr = null;
		DbDataObject dboFile = null;
		try {
			svr = new SvReader(sessionId);
			dboFile = svr.getObjectById(fileObjectId, svCONST.OBJECT_TYPE_FILE, null);
		} catch (SvException ex) {
			// TODO Auto-generated catch block
			return Response.status(200).entity(ex.getLabelCode()).build();
		}
		

		StreamingOutput fileStream = new StreamingOutput() {
			@Override
			public void write(java.io.OutputStream output) throws IOException, WebApplicationException {
				InputStream is = null;
				SvFileStore svfs = null;
				SvReader rdr = null;
				try {
					svfs = new SvFileStore(sessionId);
					rdr = new SvReader(svfs);
					DbDataObject dboFile = rdr.getObjectById(fileObjectId, svCONST.OBJECT_TYPE_FILE, null);
					is = svfs.getFileAsStream(dboFile);
					IOUtils.copy(is, output);
					output.flush();
				} catch (Exception e) {
					throw new WebApplicationException("File Not Found !!");
				} finally {
					if (is != null)
						is.close();
					if(rdr!=null)
						rdr.release();
					if(svfs!=null)
						svfs.release();
				}
			}
		};
		return Response.ok(fileStream, MediaType.APPLICATION_OCTET_STREAM)
				.header("content-disposition", "filename = "+(String)dboFile.getVal("FILE_NAME")).build();
	}

	@GET
	@Path("/getFiles/{sessionId}/{linkedObjectName}/{linkedObjectId}/{fileType}/")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response getFiles(@PathParam("linkedObjectName") String linkedObjectName,
			@PathParam("linkedObjectId") Long linkedObjectId, @PathParam("fileType") String fileType,
			@PathParam("sessionId") String sessionId) {
		SvFileStore svfs = null;
		SvReader svr = null;
		try {
			svfs = new SvFileStore(sessionId);
		} catch (SvException ex) {
			// TODO Auto-generated catch block
			return Response.status(200).entity(ex.getLabelCode()).build();
		}
		try {
			Long linkedObjectTypeId = SvCore.getTypeIdByName(linkedObjectName);
			DbSearchCriterion dbs = new DbSearchCriterion("FILE_TYPE", DbCompareOperand.LIKE,"%" + fileType + "%");
			DbDataArray ret = svfs.getFilesBySearch(linkedObjectId, linkedObjectTypeId, null, dbs);
			return Response.status(200).entity(ret.toSimpleJson().toString()).build();

		} catch (Exception e) {
			String retMsg = "system.error.err";
			String logMsg = "Exception saving file.";
			if (e instanceof SvException) {
				retMsg = ((SvException) e).getLabelCode();
				logMsg = ((SvException) e).getFormattedMessage();
			}
			log4j.error(logMsg, e);
			return Response.status(200).entity(retMsg).build();
		} finally {
			if (svfs != null)
				svfs.release();
			if (svr != null)
				svr.release();

		}
		// save it
		// return Response.status(200).entity("ok").build();
	}

	@POST
	@Path("/saveFile/{sessionId}/{linkedObjectName}/{linkedObjectId}/{fileType}/")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response saveFile(@PathParam("linkedObjectName") String linkedObjectName,
			@PathParam("linkedObjectId") Long linkedObjectId, @PathParam("fileType") String fileType,
			@PathParam("sessionId") String sessionId, @FormDataParam("notes") String fileNotes,
			@FormDataParam("file_name") String fileName, @FormDataParam("data") InputStream uploadedInputStream,
			@FormDataParam("data") FormDataContentDisposition fileDetail,
			@FormDataParam("data") final FormDataBodyPart dataBody) {
		SvFileStore svfs = null;
		SvReader svr = null;
		try {
			svfs = new SvFileStore(sessionId);
		} catch (SvException ex) {
			// TODO Auto-generated catch block
			return Response.status(200).entity(ex.getLabelCode()).build();
		}
		// save it
		try {
			svr = new SvReader(svfs);
			byte[] fileData = IOUtils.toByteArray(uploadedInputStream);
			DbDataObject fileDescriptor = new DbDataObject();
			fileDescriptor.setObject_type(svCONST.OBJECT_TYPE_FILE);
			fileDescriptor.setObject_type(svCONST.OBJECT_TYPE_FILE);
			fileDescriptor.setVal("file_notes", fileNotes);
			fileDescriptor.setVal("file_size", fileData.length);
			fileDescriptor.setVal("file_date", new DateTime());
			fileDescriptor.setVal("file_name",
					fileName != null && fileName.length() > 0 ? fileName : fileDetail.getFileName());
			fileDescriptor.setVal("file_type", fileType);
			fileDescriptor.setVal("content_type", dataBody.getMediaType().toString());

			Long linkedObjectTypeId = SvCore.getTypeIdByName(linkedObjectName);
			DbDataObject linkedObject = svr.getObjectById(linkedObjectId, linkedObjectTypeId, null);
			svfs.saveFile(fileDescriptor, linkedObject, fileData, true);

		} catch (Exception e) {
			String retMsg = "system.error.err";
			String logMsg = "Exception saving file.";
			if (e instanceof SvException) {
				retMsg = ((SvException) e).getLabelCode();
				logMsg = ((SvException) e).getFormattedMessage();
			}
			log4j.error(logMsg, e);
			return Response.status(200).entity(retMsg).build();
		} finally {
			if (svfs != null)
				svfs.release();
			if (svr != null)
				svr.release();

		}
		return Response.status(200).entity("ok").build();
	}

	// save uploaded file to new location
	private void writeToFile(byte[] fileBytes, String uploadedFileLocation) {

		FileOutputStream fop = null;
		File file;

		try {

			file = new File(uploadedFileLocation);
			if (file.getParentFile() != null) {
				file.getParentFile().mkdirs();
			}
			file.createNewFile();
			fop = new FileOutputStream(file);

			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}

			// get the content in bytes
			fop.write(fileBytes);

			fop.flush();

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (fop != null) {
					fop.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}