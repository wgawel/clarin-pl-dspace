package org.dspace.app.xmlui.aspect.artifactbrowser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.cocoon.reading.AbstractReader;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.Context;
import org.xml.sax.SAXException;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class ItemListReader extends AbstractReader{

	private static final Logger log = Logger.getLogger(ItemListReader.class);
	private String search;

	@Override
	public void generate() throws IOException, SAXException,
			ProcessingException {
		final HttpServletRequest httpRequest = (HttpServletRequest) objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT);
		search = parameters.getParameter("search", null);
		try {
			sendJSONResponse(loadItems(httpRequest));
		} catch (SQLException e) {
			log.error("Sql error can't load items ", e);
		}
	}

	private List<JsonItem> loadItems(HttpServletRequest request) throws SQLException {
		
		List<JsonItem> list = new ArrayList<JsonItem>();
		Context context = ContextUtil.obtainContext(objectModel);
		ItemIterator item = Item.findAll(context);
		while( item.hasNext() ) {
			Item i = item.next();
			String handle = i.getHandle();
			String bitstreamUrl = String.format("%s%s/handle/%s/", ProcessDocumentsAction.host, request.getContextPath(), handle);
			if(search !=null && !search.equals("")){
				if(containsSearchFrase(i, search)){
					list.add(new JsonItem(
							i.getName(), 
							bitstreamUrl, 
							i.getMetadata("dc.contributor.author"),
							i.getMetadata("dc.type")));
				}
			} else {
				list.add(new JsonItem(
						i.getName(), 
						bitstreamUrl, 
						i.getMetadata("dc.contributor.author"),
						i.getMetadata("dc.type")));
			}
		}
	
		return list;
	}

	private boolean containsSearchFrase(Item item, String search){
		if(item.getName().toLowerCase().contains(search.toLowerCase())) return true;
		if((item.getMetadata("dc.contributor.author")).toLowerCase().contains(search.toLowerCase())) return true;
		if(item.getMetadata("dc.type").toLowerCase().contains(search.toLowerCase())) return true;
		return false;
	}

	private void sendJSONResponse(List<JsonItem> elements)
			throws UnsupportedEncodingException, IOException {
		
		Type collectionType = new TypeToken<List<JsonItem>>() {
        } // end new
                .getType();
		 String gsonString = 
	                new Gson().toJson(elements, collectionType);
		
		ByteArrayInputStream inputStream = new ByteArrayInputStream(gsonString.getBytes("UTF-8"));
		IOUtils.copy(inputStream, out);
		out.flush();
	}

	public class JsonItem{
		String name;
		String url;
		String author;
		String type;
		
		public JsonItem() {
		}

		public JsonItem(String name, String url, String author,String type) {
			super();
			this.name = name;
			this.url = url;
			this.author = author;
			this.type = type;
		}

		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getUrl() {
			return url;
		}
		public void setUrl(String url) {
			this.url = url;
		}
		public String getAuthor() {
			return author;
		}
		public void setAuthor(String author) {
			this.author = author;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}
	}
}
