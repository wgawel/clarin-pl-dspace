package pl.edu.pwr.dspace.submit.step;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.submit.AbstractProcessingStep;

public class CmdiStep extends AbstractProcessingStep{

	private static Logger log = Logger.getLogger(CmdiStep.class);
    /** Button to upload a file * */
    public static final String SUBMIT_UPLOAD_BUTTON = "submit_upload";

    /** Button to skip uploading a file * */
    public static final String SUBMIT_SKIP_BUTTON = "submit_skip";

    /** Button to submit more files * */
    public static final String SUBMIT_MORE_BUTTON = "submit_more";

    /** Button to cancel editing of file info * */
    public static final String CANCEL_EDIT_BUTTON = "submit_edit_cancel";

    // integrity error occurred
    public static final int STATUS_INTEGRITY_ERROR = 1;

    // error in uploading file
    public static final int STATUS_UPLOAD_ERROR = 2;

    // error - no files uploaded!
    public static final int STATUS_NO_FILES_ERROR = 5;

    // error - no files uploaded!
    public static final int STATUS_NO_CMDI_FILE_ERROR = 401;

    // format of uploaded file is unknown
    public static final int STATUS_UNKNOWN_FORMAT = 10;

    // virus checker unavailable ?
    public static final int STATUS_VIRUS_CHECKER_UNAVAILABLE = 14;

    // file failed virus check
    public static final int STATUS_CONTAINS_VIRUS = 16;

    // edit file information
    public static final int STATUS_EDIT_BITSTREAM = 20;

    // return from editing file information
    public static final int STATUS_EDIT_COMPLETE = 25;
    
    // file not found while processing local file
    public static final int STATUS_NOT_FOUND = 101;

    // file failed virus check
    public static final int STATUS_SERVER_FILE_CONTAINS_VIRUS = 116;
	public CmdiStep() throws ServletException{
	}
	
	@Override
	public int doProcessing(Context context, HttpServletRequest request,
			HttpServletResponse response, SubmissionInfo subInfo)
			throws ServletException, IOException, SQLException,
			AuthorizeException {
		return STATUS_COMPLETE;
	}

	@Override
	public int getNumberOfPages(HttpServletRequest request,
			SubmissionInfo subInfo) throws ServletException {
		return 1;
	}
}
