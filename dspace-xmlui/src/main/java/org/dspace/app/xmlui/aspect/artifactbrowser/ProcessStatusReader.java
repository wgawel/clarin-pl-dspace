package org.dspace.app.xmlui.aspect.artifactbrowser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.cocoon.ProcessingException;
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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.xml.sax.SAXException;

public class ProcessStatusReader extends AbstractReader {

	private static final Logger log = Logger
			.getLogger(ProcessStatusReader.class);

	private static final String nlengineTaskStatusURL = ConfigurationManager
			.getProperty("dspace.nlpengine.url") + "getJSONStatus/";

	private Item item = null;
	private String handle;

	@Override
	public void generate() throws IOException, SAXException,
			ProcessingException {

		try {
			this.handle = parameters.getParameter("handle", null);
			loadItem();
		} catch (SQLException e) {
			log.error("Unable to find item", e);
		}

		Status responseStatus = new Status();
		checkStatusChange(responseStatus);
		sendJSONResponse(buildResponseMap(responseStatus));
	}

	@SuppressWarnings("unchecked")
	private void checkStatusChange(Status status) {
		String token = item.getNLPEngineToken();
		String currentItemStatus = item.getProcessStatus();
		Map<String, Object> engineStatus = new HashMap<String, Object>();

		if (token != null && !"".equals(token)) {
			try {
				if(!currentItemStatus.equals("DONE")){
					engineStatus = getNlpengineStatus(token);
					if (!currentItemStatus.equals(engineStatus.get("status"))) {
						item.setProcessStatus((String) engineStatus.get("status"));
					}
				}
				switch (ProcessStatus.valueOf(item.getProcessStatus())) {
				case PROCESSING:
					status.progress = Double.toString((Double) engineStatus.get("value"));
					break;
				case ERROR:
					status.progress = "0";
					status.error = String.format("Error message recived from engine: %s", (String) engineStatus.get("value"));
					break;
				case DONE:
					status.progress = "1";
					break;
				case READY:
				default:
					break;
				}
			} catch (Exception e) {
				status.error = String.format("Error: %s",e.getMessage());
			}
		}
		status.nlpEngineToken = item.getNLPEngineToken();
		status.status = item.getProcessStatus();
	}

	private Map<String, String> buildResponseMap(Status status) {
		Map<String, String> map = new HashMap<String, String>();
		map.put("handle", handle);
		map.put("nlpengine_token", status.nlpEngineToken);
		map.put("status", status.status);
		map.put("progress", status.progress);
		map.put("error", status.error);
		return map;
	}

	private void sendJSONResponse(Map<String, String> map)
			throws UnsupportedEncodingException, IOException {
		JSONObject obj = new JSONObject(map);
		ByteArrayInputStream inputStream = new ByteArrayInputStream(obj
				.toJSONString().getBytes("UTF-8"));
		IOUtils.copy(inputStream, out);
		out.flush();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Map getNlpengineStatus(String token) {
		RestTemplate template = new RestTemplate();
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(nlengineTaskStatusURL + token);
		Map<String, Object> json = template.getForObject(builder.build().toUri(), Map.class);
		return json;
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

	private class Status {
		String nlpEngineToken = "";
		String status = "";
		String error = "";
		String progress = "0";
	}

	public enum ProcessStatus {
		READY, PROCESSING, ERROR, DONE
	}
}
