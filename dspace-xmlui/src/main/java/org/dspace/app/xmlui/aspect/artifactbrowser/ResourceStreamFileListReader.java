package org.dspace.app.xmlui.aspect.artifactbrowser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.cocoon.reading.AbstractReader;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.xml.sax.SAXException;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class ResourceStreamFileListReader extends AbstractReader{

	private static final Logger log = Logger.getLogger(ResourceStreamFileListReader.class);
	
	private Item item = null;
	private HttpClient client = new DefaultHttpClient();
	private Context context;
	private String handle;

	@Override
	public void generate() throws IOException, SAXException,
			ProcessingException {
		
		final HttpServletRequest httpRequest = (HttpServletRequest) objectModel
				.get(HttpEnvironment.HTTP_REQUEST_OBJECT);

		String handle = parameters.getParameter("handle", null);
		String bitstreamUrl = String.format("%s%s/bitstream/handle/%s/", ProcessDocumentsAction.host, httpRequest.getContextPath(), handle);
		List<ResourceFileElement> elements = new ArrayList<ResourceFileElement>();
		try {
			this.handle = parameters.getParameter("handle", null);
			int itemID = parameters.getParameterAsInteger("itemID", -1);
			loadItem();
			List<Bitstream> streamList = getFiles(handle, itemID);
			Set<String> names = new HashSet<String>();
			
			for (Bitstream stream : streamList) {
				URI uri = new URI(bitstreamUrl + URLEncoder.encode(stream.getName(), "UTF-8").replace("+", "%20")+"?sequence="+stream.getSequenceID());
				String cclFilename="";
				if(names.contains(stream.getName())){
					cclFilename = stream.getName().substring(0, stream.getName().lastIndexOf("."))+"."+stream.getSequenceID()+".ccl";
				} else {
					cclFilename = stream.getName().substring(0, stream.getName().lastIndexOf("."))+".ccl";
					names.add(stream.getName());
				}
				elements.add(new ResourceFileElement(
						uri.toString(), 
						stream.getName(), 
						Long.toString(stream.getSize()), 
						stream.getFormat().getMIMEType(), 
						stream.getDescription(),cclFilename));
			}
			
		} catch (SQLException e) {
			log.info("Error accessing db:",e);
		} catch (URISyntaxException e) {
			log.info("Cant parse url:",e);
		}
		sendJSONResponse(elements);
	}

	private void sendJSONResponse(List<ResourceFileElement> elements)
			throws UnsupportedEncodingException, IOException {
		
		Type collectionType = new TypeToken<List<ResourceFileElement>>() {
        } // end new
                .getType();
		 String gsonString = 
	                new Gson().toJson(elements, collectionType);
		
		ByteArrayInputStream inputStream = new ByteArrayInputStream(gsonString.getBytes("UTF-8"));
		IOUtils.copy(inputStream, out);
		out.flush();
	}

	private void loadItem() throws SQLException {

		Context context = ContextUtil.obtainContext(objectModel);
		int itemID = parameters.getParameterAsInteger("itemID", -1);
		DSpaceObject dso = null;

		if (itemID > -1) {
			// Referenced by internal itemID
			item = Item.find(context, itemID);
		} else if (handle != null) {
			// Reference by an item's handle.
			dso = HandleManager.resolveToObject(context, handle);

			if (dso instanceof Item) {
				item = (Item) dso;
			}
		}
	}
	
	public List<Bitstream> getFiles(String handle, int itemID)
			throws SQLException {

		List<Bitstream> files = new ArrayList<Bitstream>();

		Bundle[] originals = item.getBundles("ORIGINAL");
		for (Bundle original : originals) {
			Bitstream[] bss = original.getBitstreams();
			for (Bitstream bitstream : bss) {
				String filename = bitstream.getName();

				if (bitstream.getName().matches(ProcessDocumentsAction.ALLOWED_FILES)) {
					files.add(bitstream);
				}
			}
		}

		return files;
	}
	
	private class ResourceFileElement{

		private String url;
		private String name;
		private String size;
		private String format;
		private String description;
		private String ccl;
		
		public ResourceFileElement() {}
		
		public ResourceFileElement(String url, String name, String size,
				String format, String description, String ccl) {
			this.url = url;
			this.name = name;
			this.size = size;
			this.format = format;
			this.description = description;
			this.ccl = ccl;
		}

		public String getUrl() {
			return url;
		}
		public void setUrl(String url) {
			this.url = url;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getSize() {
			return size;
		}
		public void setSize(String size) {
			this.size = size;
		}
		public String getFormat() {
			return format;
		}
		public void setFormat(String format) {
			this.format = format;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}

		public String getCcl() {
			return ccl;
		}

		public void setCcl(String ccl) {
			this.ccl = ccl;
		}
	}
}
