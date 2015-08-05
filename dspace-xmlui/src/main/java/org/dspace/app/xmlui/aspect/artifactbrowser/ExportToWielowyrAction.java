package org.dspace.app.xmlui.aspect.artifactbrowser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.cocoon.reading.AbstractReader;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.xml.sax.SAXException;


public class ExportToWielowyrAction extends AbstractReader{

	private static final String wielowyrURL = ConfigurationManager.getProperty("dspace.wielowyr.export.url");
	private static final String nfs = ConfigurationManager.getProperty("dspace.nfs.path");
	private static final String zip_name = ConfigurationManager.getProperty("dspace.ccl.zip");
	private static final Logger log = Logger.getLogger(ExportToWielowyrAction.class);

	private Item item = null;
	private String handle;
	private Context context;

	@Override
	public void generate() throws IOException, SAXException,
			ProcessingException {

		final HttpServletRequest httpRequest = (HttpServletRequest) objectModel
				.get(HttpEnvironment.HTTP_REQUEST_OBJECT);
		final HttpServletResponse httpResponse = (HttpServletResponse) objectModel
				.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
		Map<String,String> m = new HashMap<String, String>();
		m.put("redirect", null);
		m.put("error", null);
		
		try {
			context = ContextUtil.obtainContext(objectModel);
			loadItem(objectModel, parameters);
		} catch (SQLException err) {
			log.error("SQL error",err);
			m.put("error","Internal error: " + err.getMessage());
		}

		if (item.getProcessStatus().equals("DONE")) {
			try {
				Map<String, String> response = callService();
				if (response.containsKey("redirect")) {
					m.put("redirect", response.get("redirect"));
				}
				if (response.containsKey("error")) {
					log.error("Error MeWeX : " + response.get("error"));
					m.put("error", response.get("error"));
				}
			} catch (Exception e) {
				log.error(e);
				m.put("error", "Internal error: " + e.getMessage());
			}
		}		
		sendJSONResponse(m);
	}

	private void sendJSONResponse(Map<String, String> map)
			throws UnsupportedEncodingException, IOException {
		JSONObject obj = new JSONObject(map);
		ByteArrayInputStream inputStream = new ByteArrayInputStream(obj
				.toJSONString().getBytes("UTF-8"));
		IOUtils.copy(inputStream, out);
		out.flush();
	}

	private Map callService() throws SQLException, ParseException, org.apache.http.ParseException, IOException {
		RestTemplate template = new RestTemplate();
		
		String nfs_file = String.format("%s%s/%s", nfs, handle, zip_name);
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(wielowyrURL);
		
		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		map.add("name", item.getName());
		map.add("email", context.getCurrentUser().getEmail());
		map.add("url", nfs_file);
		
		String res = template.postForObject(builder.build().toUri(), map, String.class);

		JSONObject json = (JSONObject)new JSONParser().parse(res);
		return json;
		
	}
	
	private void loadItem(Map objectModel, Parameters parameters) throws SQLException {

			this.handle = parameters.getParameter("handle", null);
			
			DSpaceObject dso = null;
			int itemID = parameters.getParameterAsInteger("itemID", -1);

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

}
