package pl.edu.wroc.dspace.api;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.reading.AbstractReader;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import cz.cuni.mff.ufal.lindat.utilities.hibernate.CmdiProfile;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.HibernateUtil;
import pl.edu.pwr.cmdi.CmdiFileReader;
import pl.edu.pwr.cmdi.CmdiFormBuilder;
import pl.edu.pwr.cmdi.xml.CmdComponent;
import pl.edu.pwr.cmdi.xml.Header;


public class APICmdiImportProfile extends AbstractReader {


	private static Logger log = Logger.getLogger(APICmdiProfiles.class);
	private String profileId;

	@Override
	public void setup(SourceResolver resolver, Map objectModel, String src,
			Parameters parameters) throws ProcessingException, SAXException,IOException {
		super.setup(resolver,objectModel,src,parameters);

		try {
			profileId = parameters.getParameter("profileId");
		} catch (ParameterException e) {
			log.error("Bad parameter in path", e);
		}

	}

	private static final String ASSET_PATH =  "/dspace/assetstore/profiles/";
	

	@Override
	public void generate() throws IOException, SAXException, ProcessingException {
			HibernateUtil hibernateUtil = new HibernateUtil();
			Map<String, String> response = new HashMap<String,String>();
			if(profileId == null || "".equals(profileId)){
				response.put("Status", "ERROR");
				response.put("Info", "Incorrect profile id");
			} else {
				try {
					InputStream in = sendGet(profileId);
					saveToTempFile(in, profileId);
					CmdiFileReader reader = new CmdiFileReader();
					Document doc = reader.parseCmdiFile(new File("/tmp/"+profileId));

					CmdiFormBuilder builder = new CmdiFormBuilder();
					
					List<Node> list = reader.getListForElement(doc.getFirstChild().getChildNodes());
					CmdComponent cmd = reader.getCmdComponent(list.get(1));
					Header header = reader.getHeader((list.get(0)));
					
					PrintWriter out = new PrintWriter(ASSET_PATH + header.id);
					out.print(buildPorfileFormPage(builder.build(cmd)));
					out.close();
					
					//check if exist
					CmdiProfile profile = new CmdiProfile(header.id, header.name, ASSET_PATH + header.id);
					hibernateUtil.persist(CmdiProfile.class, profile);
					
					response.put("Status", "DONE");
					response.put("Info", "Profile imported");
				} catch (Exception e) {
					response.put("Status", "ERROR");
					response.put("Info", "Error while building file");
				} 
			}
			sendJSONResponse(response);

	}
	private void saveToTempFile(InputStream in,String filename){
		OutputStream outputStream = null;
		 
		try {
	 
			// write the inputStream to a FileOutputStream
			outputStream = new FileOutputStream(new File("/tmp/"+filename));
	 
			int read = 0;
			byte[] bytes = new byte[1024];
	 
			while ((read = in.read(bytes)) != -1) {
				outputStream.write(bytes, 0, read);
			}
 
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (outputStream != null) {
				try {
					// outputStream.flush();
					outputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
	 
			}
		}
	}
	private InputStream sendGet(String profileId) throws Exception {
		 
		String url = String.format("http://catalog.clarin.eu/ds/ComponentRegistry/rest/registry/profiles/%s/xml",profileId);
 
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("GET");
		return con.getInputStream();
	}
 
	private void sendJSONResponse(Map<String, String> map)
			throws UnsupportedEncodingException, IOException {
		
		Type collectionType = new TypeToken<Map<String, String>>() {
        } // end new
                .getType();
		 String gsonString = 
	                new Gson().toJson(map, collectionType);
		
		ByteArrayInputStream inputStream = new ByteArrayInputStream(gsonString.getBytes("UTF-8"));
		IOUtils.copy(inputStream, out);
		out.flush();
	}
	
	private String buildPorfileFormPage(String form){
		StringBuilder sb = new StringBuilder();
		sb.append("<html><header>");
		sb.append("<link href=\"/dspace/themes/ClarinPlTheme/lib/css/upload.css\" rel=\"stylesheet\"></link>");
		sb.append("<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.7/jquery.min.js\" type=\"text/javascript\"> </script>");
		sb.append("<script src=\"/dspace/themes/ClarinPlTheme/lib/js/upload.js\" type=\"text/javascript\"> </script>");
		sb.append("</header><body>");
		sb.append(form);
		sb.append("</body></html>");
		return sb.toString();
	}
}
