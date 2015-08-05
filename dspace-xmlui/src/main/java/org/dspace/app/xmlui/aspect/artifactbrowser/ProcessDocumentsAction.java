package org.dspace.app.xmlui.aspect.artifactbrowser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.util.UriComponentsBuilder;

public class ProcessDocumentsAction extends AbstractAction {

	private static final Logger log = Logger.getLogger(ProcessDocumentsAction.class);
	private static final String nlengineURL = ConfigurationManager.getProperty("dspace.nlpengine.url") + "startTask/";
	public static final String host = ConfigurationManager.getProperty("dspace.baseUrl");
	public static final String ALLOWED_FILES = String.format("(?i).*\\.(%s)$", ConfigurationManager.getProperty("dspace.file.extension.to.ccl"));
	private static final String meta_file = ConfigurationManager.getProperty("dspace.metafile.xml");
	private static final String zip_name = ConfigurationManager.getProperty("dspace.ccl.zip");
	private static final String dspace_hostname = ConfigurationManager.getProperty("dspace.hostname");
	private static final String nfs = ConfigurationManager.getProperty("dspace.nfs.path");

	private Item item = null;
	private HttpClient client = new DefaultHttpClient();
	private Context context;

	@Override
	public Map act(Redirector arg0, SourceResolver arg1, Map objectModel,
			String arg3, Parameters parameters) throws Exception {

		final HttpServletRequest httpRequest = (HttpServletRequest) objectModel
				.get(HttpEnvironment.HTTP_REQUEST_OBJECT);
		final HttpServletResponse httpResponse = (HttpServletResponse) objectModel
				.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);

		context = ContextUtil.obtainContext(objectModel);
		String handle = parameters.getParameter("handle", null);

		String bitstreamUrl = String.format("%s%s/bitstream/handle/%s/", host,
				httpRequest.getContextPath(), handle);

		int itemID = parameters.getParameterAsInteger("itemID", -1);
		List<Bitstream> itemFiles = getFiles(handle, itemID);
		String ipAddress = httpRequest.getRemoteAddr();
		log.info("Request ip:" + ipAddress);
		if (item.getProcessStatus().equals("READY")
				|| item.getProcessStatus().equals("ERROR")) {
			
			if(itemFiles.size()>0){
				saveMetaFile(handle);
				String xml = getXml(itemFiles, bitstreamUrl, handle);
				log.info(xml);
				String nlpengineToken = callService(xml);
				if (nlpengineToken != null && !nlpengineToken.isEmpty()) {
					item.setNLPEngineToken(nlpengineToken);
					item.setProcessStatus("PROCESSING");
				}
			}

		}
		client.getConnectionManager().shutdown();
		httpResponse.sendRedirect(httpRequest.getContextPath() + "/handle/"	+ handle);
		return null;
	}

	public List<Bitstream> getFiles(String handle, int itemID)
			throws SQLException {
		List<Bitstream> files = new ArrayList<Bitstream>();

		loadItemObject(handle, itemID);

		Bundle[] originals = item.getBundles("ORIGINAL");
		for (Bundle original : originals) {
			Bitstream[] bss = original.getBitstreams();
			for (Bitstream bitstream : bss) {
				String filename = bitstream.getName();

				if (bitstream.getName().matches(ALLOWED_FILES)) {
					files.add(bitstream);
				}
			}
		}

		return files;
	}

	private void loadItemObject(String handle, int itemID)
			throws SQLException {
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

	public String callService(String xml) {

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(nlengineURL);
		try {

			HttpPost post = new HttpPost(builder.build().toUri());
			StringEntity entity =  new StringEntity(xml, "UTF-8");
			entity.setChunked(true);
			entity.setContentType("application/xml");

			post.setEntity(entity);
			post.setHeader("Content-Type", "application/xml; charset=UTF-8");

			HttpResponse response = client.execute(post);

			BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
			return rd.readLine();
		} catch (UnsupportedEncodingException e) {
			log.error("Bad encoding ",e);
		} catch (ClientProtocolException e) {
			log.error(e);
		} catch (IOException e) {
			log.error(e);
		}
		return null;
	}

	private static final HttpHeaders header() {
		HttpHeaders headers = new HttpHeaders();
		MediaType mediaType = new MediaType("application", "xml", Charset.forName("UTF-8"));
		headers.setContentType(mediaType);
		return headers;
	}
	
	private void saveMetaFile(String handle){

		String metaURL = 
				String.format("https://%s/oai/requeststripped?verb=GetRecord&metadataPrefix=cmdi&identifier=oai:%s:%s",
						dspace_hostname,dspace_hostname,handle);
		
		HttpGet request = new HttpGet(metaURL);
	 
		// add request header
		request.addHeader("Accept", "application/xml");
		
		String nfs_path = String.format("%s%s/",nfs,handle);
		
		try {
			HttpResponse response = client.execute(request);
			
			File path = new File(nfs_path);
			path.getAbsoluteFile().mkdirs();
			
			File file = new File(nfs_path+meta_file);
			if (!file.exists()) {
				file.createNewFile();
			}
 
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file.getAbsoluteFile()), "UTF-8"));
			BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
			String line ="";
			while ((line = rd.readLine()) != null) {
				bw.write(line + "\n");
			}
			bw.close();
 
		} catch (IOException e) {
			log.error("Saving metadata file error:",e);
		}
	}

	private String getXml(List<Bitstream> files, String bitstreamUrl, String handle) {

		String[] h = handle.split("/");
		String repoId = h[0];
		String itemId = h[1];

		StringBuilder body = new StringBuilder();
		body.append(addSourceElement("1", files, bitstreamUrl, context.getCurrentUser().getEmail() ));
		body.append(addActivityElement("2", "any2txt", "1"));
		body.append(addActivityElement("3", "wcrft2large", "2"));
		body.append(addActivityElement("4", "liner2_large", "3","'{\"model\":\"all\"}'"));
		body.append(addActivityElement("5", "wsd2", "4"));
		body.append(addAgregateElement("6", zip_name, "zip", "5"));
		body.append(addOutputElement("7", "6", addOutputItemDspace("nfs", repoId, itemId, "Plik CCL")));
		String result = createXml(body.toString());
		return result;
	}

	protected String createXml(String body) {
		return new String("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><lpmn>" + body + "</lpmn>");
	}

	protected String addOutputElement(String id, String source,
			String outputItem) {
		return String.format("<output id=\"%s\" source=\"%s\">%s</output>", id,	source, outputItem);
	}

	protected String addOutputItemDspace(String type, String repoId,
			String itemId, String desc) {
		return String.format("<dspace type=\"%s\" item_id=\"%s\" repo_id=\"%s\">%s</dspace>",type, itemId, repoId, desc);
	}

	protected String addAgregateElement(String id, String name, String type,
			String source) {
		return (String.format(
				"<agregate name=\"%s\" type=\"%s\" id=\"%s\" source=\"%s\" />",
				name, type, id, source));
	}

	protected String addActivityElement(String id, String name, String source) {
		return String.format("<activity id=\"%s\" name=\"%s\" source=\"%s\" />",id, name, source);
	}
	
	protected String addActivityElement(String id, String name, String source,String options) {
		return String.format("<activity id=\"%s\" name=\"%s\" source=\"%s\" options=%s />" ,id, name, source,options);
	}

	protected String addSourceElement(String id, List<Bitstream> bitstreams, String bitstreamUrl, String dspaceLogin) {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("<source id=\"%s\" dspace_login=\"%s\">", id, dspaceLogin));
		Set<String> names = new HashSet<String>();
		for (Bitstream stream : bitstreams) {

			try {
				URI uri = new URI(bitstreamUrl + URLEncoder.encode(stream.getName(), "UTF-8").replace("+", "%20")+"?sequence="+stream.getSequenceID());
				String resultFile="";
				if(names.contains(stream.getName())){
					resultFile = stream.getName().substring(0, stream.getName().lastIndexOf("."))+"."+stream.getSequenceID()+".ccl";
				} else {
					resultFile = stream.getName().substring(0, stream.getName().lastIndexOf("."))+".ccl";
					names.add(stream.getName());
				}
				sb.append("<url name=\"" + resultFile + "\">" + uri.toURL() + "</url>");
			} catch (URISyntaxException e) {
				log.error("Bad file url", e);
			} catch (MalformedURLException e) {
				log.error("Bad file url", e);
			} catch (UnsupportedEncodingException e) {
				log.error("Encoding  url", e);
			}

		}
		sb.append("</source>");
		return sb.toString();
	}

}
