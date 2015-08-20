package pl.edu.wroc.dspace.api;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
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
import org.xml.sax.SAXException;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import cz.cuni.mff.ufal.DSpaceApi;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.CmdiProfile;
import cz.cuni.mff.ufal.lindat.utilities.interfaces.IFunctionalities;

public class APICmdiProfiles extends AbstractReader {


	private static Logger log = Logger.getLogger(APICmdiProfiles.class);
	private Integer profileId;

	@Override
	public void setup(SourceResolver resolver, Map objectModel, String src,
			Parameters parameters) throws ProcessingException, SAXException,IOException {
		super.setup(resolver,objectModel,src,parameters);

		try {
			String param = parameters.getParameter("profileId");
			if(param.equals("undefined")){
				profileId = 1;
			} else {
				profileId = new Integer(param);
			}
		} catch (ParameterException e) {
			log.error("Bad parameter in path", e);
		}

	}

	@Override
	public void generate() throws IOException, SAXException,
			ProcessingException {
			IFunctionalities manager = DSpaceApi.getFunctionalityManager();

			if(profileId == null || "".equals(profileId)){
				generateProfilesResponse(manager);				
			} else {
				generateProfileByIdResponse();
			}

	}

	private void generateProfileByIdResponse() throws FileNotFoundException,
			IOException {
		CmdiProfile profile = DSpaceApi.getCmdiProfileById(profileId);
		File file = new File(profile.getForm());
		FileInputStream fis = new FileInputStream(file);
		IOUtils.copy(fis, out);
		out.flush();
	}

	private void generateProfilesResponse(IFunctionalities manager)
			throws UnsupportedEncodingException, IOException {
		List<CmdiProfile> profiles = manager.getAll(CmdiProfile.class);
		Map<Integer, String> map = new HashMap<Integer,String>();
		for(CmdiProfile p : profiles){
			map.put(p.getID(), p.getName());
		}
		sendJSONResponse(map);
	}

	private void sendJSONResponse(Map<Integer, String> map)
			throws UnsupportedEncodingException, IOException {
		
		Type collectionType = new TypeToken<Map<Integer, String>>() {
        } // end new
                .getType();
		 String gsonString = new Gson().toJson(map, collectionType);
		
		ByteArrayInputStream inputStream = new ByteArrayInputStream(gsonString.getBytes("UTF-8"));
		IOUtils.copy(inputStream, out);
		out.flush();
	}

}
