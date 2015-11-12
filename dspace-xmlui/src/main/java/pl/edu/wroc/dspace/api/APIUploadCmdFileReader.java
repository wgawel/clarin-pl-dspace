package pl.edu.wroc.dspace.api;

import java.io.*;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import cz.cuni.mff.ufal.dspace.handle.Handle;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.reading.AbstractReader;
import org.apache.cocoon.servlet.multipart.Part;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.solr.common.util.FastInputStream;
import org.dspace.app.util.Util;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import pl.edu.wroc.dspace.api.result.ResultSuccessAddFile;
import pl.edu.wroc.dspace.api.result.elements.ApiBitstream;

public class APIUploadCmdFileReader extends AbstractReader {

	protected Map objectModel;

	protected Context context;

	private String contextPath;

	private static final String REST_URL = ConfigurationManager.getProperty("dspace.rest");
	private static final String REST_ITEMS = REST_URL + "/items/";

	private static Logger log = Logger.getLogger(APIUploadCmdFileReader.class);

	@Override
	public void setup(SourceResolver resolver, Map objectModel, String src,
			Parameters parameters) throws ProcessingException, SAXException,
			IOException {
		this.objectModel = objectModel;
		this.parameters = parameters;
		try {
			this.context = ContextUtil.obtainContext(objectModel);
		} catch (SQLException ex) {
			log.error(ex);

		}
	}

	@Override
	public void generate() throws IOException, SAXException,
			ProcessingException {

		Request request = ObjectModelHelper.getRequest(objectModel);
		ByteArrayInputStream inputStream = null ;
		this.contextPath = ConfigurationManager.getProperty("dspace.url");
		try {
			Part filePart = (Part) request.get("file");
			String itemId =  request.get("handle").toString();
			String refBitstreamId = (String) request.get("bitstreamId");

			if (filePart != null) {
				createFromFile(filePart, itemId, refBitstreamId);
			}

			if(request.get("xml") != null){
				String xml = request.get("xml").toString();
				createFromXML(xml, itemId, refBitstreamId);
			}
		} catch (Exception e) {
			log.error("Error in generate block",e);
		}
	}

	private void createFromXML(String xml, String itemId, String refBitstreamId) throws Exception {
		ByteArrayInputStream inputStream;
		Bitstream refBitstream = Bitstream.find(context, new Integer(refBitstreamId));

		File temp = File.createTempFile(refBitstream.getName(), ".cmdi");
		BufferedWriter out = new BufferedWriter(new FileWriter(temp));
		String oaiLink = REST_ITEMS + itemId + "/cmdi";
		String id = Integer.toString(refBitstream.getID());
		String link = REST_URL + "/bitstreams/" + id + "/retrieve";
		out.write(createXML(context.getCurrentUser().getFullName(), id , link, xml,oaiLink));
		out.close();
		InputStream is = new FileInputStream(temp);

		String upload_name = refBitstream.getName()+".cmdi";
		Bitstream newBitstream = addFileFromPostToItem(itemId, is, upload_name, refBitstream);
		updateReferenceItemCmdiId(refBitstream, newBitstream.getID());
	}

	private void createFromFile(Part filePart, String itemId, String refBitstreamId) throws Exception {
		ByteArrayInputStream inputStream;
		Bitstream refBitstream = Bitstream.find(context, new Integer(refBitstreamId));
		Bitstream newBitstream= null;
		if (filePart != null && filePart.getSize() > 0) {
			InputStream is = filePart.getInputStream();
			String upload_name = filePart.getUploadName();
			newBitstream = addFileFromPostToItem(itemId, is, upload_name, refBitstream);
		}
		updateReferenceItemCmdiId(refBitstream, newBitstream.getID());
		String jsonString = createJsonResultAddBitstreamSuccess(newBitstream);

		inputStream = new ByteArrayInputStream(jsonString.getBytes("UTF-8"));
		IOUtils.copy(inputStream, out);
		out.flush();
	}

	private String createJsonResultAddBitstreamSuccess(Bitstream bitstream) {
		ApiBitstream apiBitstream = new ApiBitstream(bitstream);
		return createJsonResultSuccess(apiBitstream);
	}

	private static String createJsonResultSuccess(ApiBitstream data) {
		ResultSuccessAddFile result = new ResultSuccessAddFile(data);
		return result.toJson();
	}

	public void updateReferenceItemCmdiId(Bitstream refBitstream, int cmdiId) {

		try {
			if (refBitstream.getCmdiBitstreamId() != 0) {
				Bitstream oldCmdiRef = Bitstream.find(context, refBitstream.getCmdiBitstreamId());
				oldCmdiRef.delete();
			}
			refBitstream.setCmdiBitstreamId(cmdiId);
			refBitstream.update(false);
		} catch (NumberFormatException e) {
			log.error(e);
		} catch (SQLException e) {
			log.error(e);
		} catch (AuthorizeException e) {
			log.error(e);
		}
	}

	private Element createResourceProxyElement(Document doc, String bstreamId) {

		Element root = doc.createElement("ResourceProxy");
		root.setAttribute("id", "b_" + bstreamId);
		String handle = REST_URL + "/bitstreams/" + bstreamId + "/retrieve";
		Element resourceType = doc.createElement("ResourceType");
		resourceType.setAttribute("mimetype", "text/xml");
		resourceType.setTextContent("Resource");
		Element resourceRef = doc.createElement("ResourceRef");
		resourceRef.setTextContent(handle);
		root.appendChild(resourceType);
		root.appendChild(resourceRef);

		return root;
	}

	private Element createIsPartOf(Document doc, String ownerLink){
		Element root = doc.createElement("IsPartOfList");
		Element child = doc.createElement("IsPartOf");
		child.setTextContent(ownerLink);
		root.appendChild(child);
		return root;
	}

	public void removeChilds(Node node) {
	    while (node.hasChildNodes())
	        node.removeChild(node.getFirstChild());
	}
	
	public InputStream parseCmdiFile(InputStream inputFile, String itemId, String bstramId) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		String oaiLink = REST_ITEMS + itemId +"/cmdi";
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(inputFile);

			NodeList nodes = doc.getElementsByTagName("ResourceProxyList");
			removeChilds(nodes.item(0));
			nodes.item(0).appendChild(createResourceProxyElement(doc, bstramId));

			nodes = doc.getElementsByTagName("Resources");
			nodes.item(0).appendChild(createIsPartOf(doc, oaiLink));

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

	public Bitstream addFileFromPostToItem(String handle, InputStream is,String upload_name, Bitstream refBitstream)
			throws Exception {

		Item item = (Item) refBitstream.getParentObject();

		if (is != null) {

			is = parseCmdiFile(is, handle, Integer.toString(refBitstream.getID()));

			Bitstream bitstream = Bitstream.create(context, is);

			String upload_name_stripped = upload_name;
			while (upload_name_stripped.indexOf('/') > -1) {
				upload_name_stripped = upload_name_stripped
						.substring(upload_name_stripped.indexOf('/') + 1);
			}

			while (upload_name_stripped.indexOf('\\') > -1) {
				upload_name_stripped = upload_name_stripped
						.substring(upload_name_stripped.indexOf('\\') + 1);
			}

			bitstream.setName(upload_name_stripped);
			bitstream.setSource(upload_name);

			// Identify the format
			BitstreamFormat format = FormatIdentifier.guessFormat(context,bitstream);
			bitstream.setFormat(format);

			// Update to DB
			bitstream.update(false);
			item.store_provenance_info("DSpace API: Item was added a bitstream",context.getCurrentUser());
			item.update();

			context.commit();
			
			return bitstream;
		} else {
			throw new RuntimeException("No File found.");
		}
	}

	private String createXML(String user, String id, String link, String component,String isPartOf){
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sb.append("<CMD xmlns=\"http://www.clarin.eu/cmd/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
		sb.append("CMDVersion=\"1.1\" xsi:schemaLocation=\"http://www.clarin.eu/cmd/ http://catalog.clarin.eu/ds/ComponentRegistry/rest/registry/profiles/clarin.eu:cr1:p_1396012485161/xsd\">\n");
		sb.append("<Header>\n");
		sb.append("\t<MdCreator>"+user+"</MdCreator>\n");
		sb.append("\t<MdCreationDate>" +new Date() + "</MdCreationDate>\n");
		sb.append("\t<MdProfile>clarin.eu:cr1:p_1396012485161</MdProfile>\n");
		sb.append("</Header>\n");
		sb.append("<Resources>\n");
			sb.append("\t<ResourceProxyList>\n");
				sb.append("\t<ResourceProxy id=\"b_"+id+"\">\n");
					sb.append("\t\t<ResourceType mimetype=\"text/xml\"/>\n");
					sb.append("\t\t<ResourceRef>"+link+"</ResourceRef>\n");
					sb.append("\t\t</ResourceProxy>");
			sb.append("\t</ResourceProxyList>\n");
			sb.append("\t<JournalFileProxyList/>\n");
			sb.append("\t<ResourceRelationList/>\n");
			sb.append("\t<IsPartOfList>\n\t<IsParOf>"+isPartOf+"</IsParOf>\n</IsPartOfList>\n");
		sb.append("</Resources>\n");
		sb.append("<Components>\n");
				sb.append(component);
		sb.append("</Components>\n");
		sb.append("</CMD>\n");
		return  sb.toString();
	}
}
