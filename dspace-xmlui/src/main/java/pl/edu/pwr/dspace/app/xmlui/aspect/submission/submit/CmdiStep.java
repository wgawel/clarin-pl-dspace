package pl.edu.pwr.dspace.app.xmlui.aspect.submission.submit;

import java.io.*;
import java.sql.SQLException;
import java.util.Date;

import org.apache.log4j.Logger;
import org.dspace.app.util.Util;
import org.dspace.app.xmlui.aspect.submission.AbstractSubmissionStep;
import org.dspace.app.xmlui.aspect.submission.submit.ReviewStep;
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
import org.dspace.content.*;
import org.dspace.core.ConfigurationManager;
import org.dspace.storage.bitstore.BitstreamStorageManager;
import org.dspace.workflow.WorkflowItem;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import cz.cuni.mff.ufal.DSpaceApi;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class CmdiStep extends AbstractSubmissionStep {

	 private static Logger log = Logger.getLogger(CmdiStep.class);
	 
	protected static final Message T_head = message("xmlui.Submission.submit.CmdiStep.head");
	protected static final Message T_head2 = message("xmlui.Submission.submit.CMDIUploadStep.head2");
	
	protected static final Message T_column1 = message("xmlui.Submission.submit.UploadStep.column2");
	protected static final Message T_column2 = message("xmlui.Submission.submit.UploadStep.column3");
	protected static final Message T_column3 = message("xmlui.Submission.submit.UploadStep.column4");
	protected static final Message T_column4 = message("xmlui.Submission.submit.CmdiStep.column1");
	protected static final Message T_column5 = message("xmlui.Submission.submit.CmdiStep.column2");
	protected static final Message T_column6 = message("xmlui.Submission.submit.CmdiStep.column3");
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
		updateOwnCmdiMetadata(item);
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
	        header.addCellContent(T_column4); // current cmdi file name
			header.addCellContent(T_column5); // upload cmdi file name
	        header.addCellContent(T_column6); // create cmdi file

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

				String cmdiFile ="";
				if(bitstream.getCmdiBitstreamId()>0){
					Bitstream bs = Bitstream.find(context, bitstream.getCmdiBitstreamId());
					cmdiFile = bs.getName();
				}
				row.addCellContent(cmdiFile);

	            SingleFile file = row.addCell().addSingleFile("file", collection.getHandle(), Integer.toString(id));
	            CustomButton btn = row.addCell().addCustomButton(Integer.toString(id),collection.getHandle());
	        }
    
		} else {
			summaryDiv = div.addDivision("summary");
		}
		addControlButtons(summaryDiv.addList("buttons", List.TYPE_FORM));
	}

	private void updateOwnCmdiMetadata(Item item) {
		if(item.hasOwnMetadata()){
			String bundleName = "METADATA";
			try {
				Bundle[] bundles = item.getBundles(bundleName);
				Bitstream orginal = bundles[0].getBitstreams()[0];
				InputStream is = orginal.retrieve();

				is = parseCmdiFile(is, item.getHandle(), item);
				System.out.println(is);

				Bitstream replacement = bundles[0].createBitstream(is);
				replacement.setName(orginal.getName());
				replacement.setSource(orginal.getSource());
				replacement.setFormat(orginal.getFormat());

				orginal.delete();
				replacement.update(false);
				item.update();
				context.commit();

			} catch (SQLException e) {
				e.printStackTrace();
			} catch (AuthorizeException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public InputStream parseCmdiFile(InputStream inputFile, String handle, Item item) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(inputFile);

			NodeList user = doc.getElementsByTagName("MdCreator");
			NodeList creationDate = doc.getElementsByTagName("MdCreationDate");
			user.item(0).setTextContent(context.getCurrentUser().getFullName());
			Date now = new Date();
			creationDate.item(0).setTextContent(now.toString());
			NodeList nodes = doc.getElementsByTagName("ResourceProxyList");
			removeChilds(nodes.item(0));
			try {
				Bundle[] bundles = item.getBundles("ORIGINAL");
				for(Bitstream b : bundles[0].getBitstreams()) {
					nodes.item(0).appendChild(createResourceProxyElement(doc,makeBitstreamLink(submission.getCollection().getHandle(),b), Integer.toString(b.getID())));
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}

			DOMSource source = new DOMSource(doc);
			Result outputTarget = new StreamResult(outputStream);
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty("indent", "yes");
			transformer.transform(source, outputTarget);
			InputStream is = new ByteArrayInputStream(outputStream.toByteArray());
			return is;
		} catch (ParserConfigurationException pce) {
			log.info(pce);
		} catch (TransformerException tfe) {
			log.info(tfe);
		} catch (IOException ioe) {
			log.info(ioe);
		} catch (SAXException sae) {
			log.info(sae);
		}
		return null;
	}
	private String makeBitstreamLink(String handle, Bitstream bitstream) {
		String name = bitstream.getName();
		StringBuilder result = new StringBuilder(ConfigurationManager.getProperty("dspace.url"));
		result.append("/bitstream/handle/").append(handle);
		// append name although it isn't strictly necessary
		try {
			if (name != null) {
				result.append("/").append(
						Util.encodeBitstreamName(name, "UTF-8"));
			}
		} catch (UnsupportedEncodingException uee) {
			// just ignore it, we don't have to have a pretty
			// name on the end of the url because the sequence id will
			// locate it. However it means that links in this file might
			// not work....
		}
		result.append("?sequence=").append(
				String.valueOf(bitstream.getSequenceID()));
		return result.toString();
	}
	private Element createResourceProxyElement(Document doc, String handle, String bstreamId) {

		Element root = doc.createElement("ResourceProxy");
		root.setAttribute("id", "_" + bstreamId);

		Element resourceType = doc.createElement("ResourceType");
		resourceType.setAttribute("mimetype", "text/xml");
		resourceType.setTextContent("Resource");
		Element resourceRef = doc.createElement("ResourceRef");
		resourceRef.setTextContent(handle);
		root.appendChild(resourceType);
		root.appendChild(resourceRef);

		return root;
	}

	public void removeChilds(Node node) {
		while (node.hasChildNodes())
			node.removeChild(node.getFirstChild());
	}

	@Override
	public List addReviewSection(List reviewList) throws SAXException,
			WingException, UIException, SQLException, IOException,
			AuthorizeException {
		// Create a new list section for this step (and set its heading)
		List cmdiSection = reviewList.addList("submit-review-" + this.stepAndPage, List.TYPE_FORM);
		cmdiSection.setHead(T_head);

		// Review all uploaded files
		Item item = submission.getItem();
		Bundle[] bundles = item.getBundles("ORIGINAL");

		Bitstream[] bitstreams = new Bitstream[0];
		if (bundles.length > 0)
		{
			bitstreams = bundles[0].getBitstreams();
		}

		for (Bitstream bitstream : bitstreams)
		{

			String name = bitstream.getName();
			String url = "";
			String cmdiFile="";

			if(bitstream.getCmdiBitstreamId() > 0) {
				Bitstream cmdi = Bitstream.find(context, bitstream.getCmdiBitstreamId());
				cmdiFile += cmdi.getName();
			}

			org.dspace.app.xmlui.wing.element.Item file = cmdiSection.addItem();
			file.addXref(url,name);
			file.addContent(" -> "+ cmdiFile);

		}

		if(bitstreams.length==0){
			cmdiSection.addItem("You didn't upload any cmdi files.");
		}

		// return this new "upload" section
		return cmdiSection;
	}
	
}
