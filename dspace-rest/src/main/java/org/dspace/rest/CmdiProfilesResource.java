package org.dspace.rest;
import cz.cuni.mff.ufal.DSpaceApi;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.CmdiProfile;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.HibernateUtil;
import cz.cuni.mff.ufal.lindat.utilities.interfaces.IFunctionalities;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.rest.cmdi.CmdiFileReader;
import org.dspace.rest.cmdi.CmdiFormBuilder;
import org.dspace.rest.cmdi.xml.CmdComponent;
import org.dspace.rest.cmdi.xml.Header;
import org.dspace.rest.common.Community;
import org.dspace.rest.exceptions.ContextException;
import org.dspace.usage.UsageEvent;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by tnaskret on 22.10.15.
 */
@Path("/cmdi/profiles")
public class CmdiProfilesResource extends Resource {
    private static Logger log = Logger.getLogger(CmdiProfilesResource.class);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public javax.ws.rs.core.Response getProfiles(@QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
                                            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException {
        List<CmdiProfile> profiles = DSpaceApi.getCmdiProfiles();
        Map<Integer, String> map = new HashMap<Integer, String>();
        for (CmdiProfile p : profiles) {
            map.put(p.getID(), p.getName());
        }
        return Response.status(200).type("application/json").entity(map).build();
    }

    @GET
    @Path("/{profile_id}/form")
    public javax.ws.rs.core.Response getProfileById(@PathParam("profile_id") Integer profileId,
                                                     @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
                                                     @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException {

        org.dspace.core.Context context = null;
        FileInputStream fis = null;
        try {
            context = createContext(getUser(headers));
            CmdiProfile profile = DSpaceApi.getCmdiProfileById(profileId);
            File file = new File(profile.getForm());
            fis = new FileInputStream(file);
            context.complete();
        } catch (FileNotFoundException e) {
            processException("Could not read profile(id=" + profileId + "),  Message:" + e, context);
        } catch (SQLException e) {
            processException("Could not read profile(id=" + profileId + "), SQLException. Message:" + e, context);
        } catch (ContextException e) {
            processException(
                    "Could not retrieve profile file(id=" + profileId + "), ContextException! Message: " + e.getMessage(),
                    context);
        }
        return Response.ok(fis).build();
    }

    private static final String ASSET_PATH =  "/dspace/assetstore/profiles/";
    private static String CATALOG = "http://catalog.clarin.eu/ds/ComponentRegistry/rest/registry/profiles/%s/xml";

    @GET
    @Path("/import/{profile_id}")
    public javax.ws.rs.core.Response importProfileFromClarinCatalog(@PathParam("profile_id") String profileId, @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
                                                                    @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request){

        Map<String, String> map = new HashMap<String, String>();
            try {
                if(profileId == null || "".equals(profileId)) throw new IllegalArgumentException();
                InputStream in = sendGet(profileId);
                saveToTempFile(in, profileId);
                CmdiFileReader reader = new CmdiFileReader();
                Document doc = reader.parseCmdiFile(new File("/tmp/" + profileId));

                CmdiFormBuilder builder = new CmdiFormBuilder();

                List<Node> list = reader.getListForElement(doc.getFirstChild().getChildNodes());
                CmdComponent cmd = reader.getCmdComponent(list.get(1));
                Header header = reader.getHeader((list.get(0)));

                PrintWriter out = new PrintWriter(ASSET_PATH + header.id);
                out.print(buildPorfileFormPage(builder.build(cmd)));
                out.close();

                //check if exist
                CmdiProfile profile = new CmdiProfile(header.id, header.name, ASSET_PATH + header.id);
                DSpaceApi.saveCmdiProfile(profile);

                map.put("status", "SUCCESS");
                map.put("message", "Profile " + profileId + " imported");
                return Response.status(200).type("application/json").entity(map).build();

            } catch (Exception e) {
                map.put("status","ERROR");
                map.put("message", e.getMessage());
                return Response.status(Response.Status.BAD_REQUEST).type("application/json").entity(map).build();
            }
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

    private InputStream sendGet(String profileId) throws IOException {

        String url = String.format(CATALOG,profileId);
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        return con.getInputStream();
    }

    private String buildPorfileFormPage(String form){
        StringBuilder sb = new StringBuilder();
        sb.append(form);
        return sb.toString();
    }
}
