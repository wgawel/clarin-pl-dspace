package pl.edu.pwr.dspace.app.xmlui.aspect.submission.submit;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.submission.AbstractSubmissionStep;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.CustomButton;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.SingleFile;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.workflow.WorkflowItem;
import org.xml.sax.SAXException;

import cz.cuni.mff.ufal.DSpaceApi;

public class CmdiStep extends AbstractSubmissionStep {

	 private static Logger log = Logger.getLogger(CmdiStep.class);
	 
	protected static final Message T_head = message("xmlui.Submission.submit.CmdiStep.head");
	protected static final Message T_head2 = message("xmlui.Submission.submit.UploadStep.head2");
	
	protected static final Message T_column1 = message("xmlui.Submission.submit.UploadStep.column2");
	protected static final Message T_column2 = message("xmlui.Submission.submit.UploadStep.column3");
	protected static final Message T_column3 = message("xmlui.Submission.submit.UploadStep.column4");
	protected static final Message T_column4 = message("xmlui.Submission.submit.CmdiStep.column1");
	protected static final Message T_column5 = message("xmlui.Submission.submit.CmdiStep.column2");
    protected static final Message T_submit_edit = message("xmlui.Submission.submit.UploadStep.submit_edit");

    protected static final Message T_description = message("xmlui.Submission.submit.UploadStep.description");
    protected static final Message T_description_help = message("xmlui.Submission.submit.UploadStep.description_help");
    protected static final Message T_submit_upload = message("xmlui.Submission.submit.UploadStep.submit_upload");
    protected static final Message T_inform_about_licences = message("xmlui.Submission.submit.UploadStep.inform_about_licences");

	public CmdiStep() {
	}

    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException,
    UIException, SQLException, IOException, AuthorizeException {
        super.addPageMeta(pageMeta);
        pageMeta.addMetadata("include-library","uploadFile");
    }

	public void addBody(Body body) throws SAXException, WingException,
    UIException, SQLException, IOException, AuthorizeException {

		Item item = submission.getItem();
		Collection collection = submission.getCollection();
		String actionURL = contextPath + "/handle/"+collection.getHandle() + "/submit/" + knot.getId() + ".continue";
		boolean workflow = submission instanceof WorkflowItem;
		Bundle[] bundles = item.getBundles("ORIGINAL");
		Bitstream[] bitstreams = new Bitstream[0];
		if (bundles.length > 0)
		{
			bitstreams = bundles[0].getBitstreams();
		}

	  	Division div = body.addInteractiveDivision("submit-cmdi", actionURL, Division.METHOD_POST, "primary submission");
    	div.setHead(T_submission_head);
    	addSubmissionProgressList(div);
    	
		Division summaryDiv = null;

        if (bitstreams.length > 0 || workflow)
		{
        	summaryDiv = div.addDivision("summary", "well well-light");
        	
	        Table summary = summaryDiv.addTable("submit-upload-summary",(bitstreams.length * 2) + 2,7);
	        summary.setHead(T_head2);
	        
	        Row header = summary.addRow(Row.ROLE_HEADER);

	        header.addCellContent(T_column1); // file name
	        header.addCellContent(T_column2); // size
	        header.addCellContent(T_column3); // description
	        header.addCellContent(T_column4); // upload cmdi file
	        header.addCellContent(T_column5); // create cmdi file

	        for (Bitstream bitstream : bitstreams)
	        {
	        	int id = bitstream.getID();
	        	String name = bitstream.getName();
	        	long bytes = bitstream.getSize();
	        	String desc = bitstream.getDescription();
	        	
	        	Row row = summary.addRow();
	            row.addCell().addXref(url,name);

	            row.addCellContent(DSpaceApi.convertBytesToHumanReadableForm(bytes));
	            row.addCellContent(desc);
	            
	            SingleFile file = row.addCell().addSingleFile("file", collection.getHandle(), Integer.toString(id));
	            CustomButton btn = row.addCell().addCustomButton(Integer.toString(id),collection.getHandle());
	        }
    
		} else {
			summaryDiv = div.addDivision("summary");
		}
		addControlButtons(summaryDiv.addList("buttons", List.TYPE_FORM));
	}

	
	@Override
	public List addReviewSection(List reviewList) throws SAXException,
			WingException, UIException, SQLException, IOException,
			AuthorizeException {
      return null;
	}
	
}
