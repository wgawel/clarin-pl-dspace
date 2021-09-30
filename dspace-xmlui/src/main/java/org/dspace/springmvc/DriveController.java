package org.dspace.springmvc;

import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.submission.submit.UploadStep;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.springsocial.AsyncBitstreamAdder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.social.google.api.Google;
import org.springframework.social.google.api.drive.DriveFile;
import org.springframework.social.google.api.drive.DriveFilesPage;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * Interactions with gdrive
 */
@Controller
@RequestMapping("/drive")
@Profile("drive-beta")
public class DriveController {

	Logger log = Logger.getLogger(DriveController.class);

	/**
	 * Drive connection for the current user
	 */
	private final Google google;

	private final AsyncBitstreamAdder asyncBitstreamAdder;

	@Autowired
	public DriveController(Google google, AsyncBitstreamAdder asyncBitstreamAdder) {
		this.google = google;
		this.asyncBitstreamAdder = asyncBitstreamAdder;
	}

	/**
	 * Get /drive/files fetches the root drive directory
	 * @param pageToken
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="/files", method=GET)
	public DriveFilesPage getRootFileList(@RequestParam(required=false) String pageToken) {
        try {
		    return google.driveOperations().getRootFiles(pageToken);
        }catch(Exception e){
            return new DriveFilesPage();
        }
	}

	/**
	 * GET /drive/files/{id} fetches content of directory with that id or errors it the id belongs to a file
	 * @param request
	 * @param response
	 * @param id
	 * @param pageToken
	 * @return
	 * @throws IOException
	 * @throws SQLException
	 * @throws AuthorizeException
	 * @throws URISyntaxException
	 */
	@ResponseBody
	@RequestMapping(value="/files/{id}", method=GET)
	public ResponseEntity<?> getFileList(HttpServletRequest request, HttpServletResponse response, @PathVariable String id,
									 @RequestParam(required = false) String pageToken)
			throws IOException, SQLException, AuthorizeException, URISyntaxException {
		DriveFile driveFile = google.driveOperations().getFile(id);
		if (driveFile.isFolder()) {
			return new ResponseEntity<>(google.driveOperations().getFiles(id, pageToken), HttpStatus.OK);
		}else {
			return new ResponseEntity<>("Use POST to add files to an item", HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * POST /drive/files/{id} initiates server side file download & bitstream addition.
	 * Uses session attribute to figure out what item are we working on.
	 * Uses another session attribute to store the future result/status
	 * Responds with ACCEPTED status and Location header containing the /status URL
	 * @param request
	 * @param response
	 * @param id
	 * @return
	 * @throws IOException
	 * @throws SQLException
	 * @throws AuthorizeException
	 * @throws URISyntaxException
	 */
	@ResponseBody
	@RequestMapping(value="/files/{id}", method=POST)
	public ResponseEntity<?> getFile(HttpServletRequest request, HttpServletResponse response, @PathVariable String id)
									throws IOException, SQLException, AuthorizeException, URISyntaxException {
		int item_id = (Integer)request.getSession().getAttribute(UploadStep.ITEM_ID);
		Context context = ContextUtil.obtainContext(request);
		Future<String> future = asyncBitstreamAdder.createBitstream(Google.class, context.getCurrentUser().getID(), item_id, id);
		request.getSession().setAttribute(id, future);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(new URI(request.getRequestURI() + "/status"));
		return new ResponseEntity<>("", headers, HttpStatus.ACCEPTED);
	}

	/**
	 * GET /drive/files/{id}/status - basic status reporting on download of file with id
	 * Uses session attribute to obtain future/status of asyncBitstreamAdder
	 * @param request
	 * @param response
	 * @param id
	 * @return
	 * @throws IOException
	 * @throws SQLException
	 * @throws AuthorizeException
	 */
	@ResponseBody
	@RequestMapping(value="/files/{id}/status", method=GET)
	public ResponseEntity<?> getFileStatus(HttpServletRequest request, HttpServletResponse response, @PathVariable String id)
								  		throws IOException, SQLException, AuthorizeException {
	    Future<String> future = (Future<String>) request.getSession().getAttribute(id);
	    if(future == null){
			return new ResponseEntity<>("Not found, different session or already finished.", HttpStatus.NOT_FOUND);
		}

		if(future.isDone()){
			//The file was added or an error occured
			request.getSession().removeAttribute(id);
			String status;
			try{
				status = future.get();
			}catch (InterruptedException | ExecutionException e){
				log.error(e);
				status = "Error:\n" + e.getMessage();
			}

			if("Done".equals(status)) {
				return new ResponseEntity<>(status, HttpStatus.OK);
			}else{
				return new ResponseEntity<>(status, HttpStatus.BAD_REQUEST);
			}
		}else{
		    //Still downloading
			return new ResponseEntity<>("Processing", HttpStatus.OK);
		}
	}

	/**
	 * Any thrown exception should show stack trace & 5xx error
	 * @return
	 */
	@ExceptionHandler(Exception.class)
	public String handleExceptions(){
		return "forward:/error";
	}
}
