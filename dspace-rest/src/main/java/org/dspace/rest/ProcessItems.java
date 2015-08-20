package org.dspace.rest;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.handle.HandleManager;
import org.dspace.rest.common.clarinpl.*;
import org.dspace.rest.exceptions.ContextException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by tnaskret on 13.08.15.
 */
@Path("/process/items/handle")
public class ProcessItems extends ItemsResource {

    private static Logger log = Logger.getLogger(ProcessItems.class);

    public static final String nlengineTaskStatusURL = ConfigurationManager.getProperty("dspace.nlpengine.url") + "getJSONStatus/";
    public static final String nlengineTaskStartURL = ConfigurationManager.getProperty("dspace.nlpengine.url") + "startTask/";
    public static final String host = ConfigurationManager.getProperty("dspace.baseUrl");
    public static final String ALLOWED_FILES = String.format("(?i).*\\.(%s)$", ConfigurationManager.getProperty("dspace.file.extension.to.ccl"));
    public static final String meta_file = ConfigurationManager.getProperty("dspace.metafile.xml");
    public static final String zip_name = ConfigurationManager.getProperty("dspace.ccl.zip");
    public static final String dspace_hostname = ConfigurationManager.getProperty("dspace.hostname");
    public static final String nfs = ConfigurationManager.getProperty("dspace.nfs.path");
    public static final String mewexURL = ConfigurationManager.getProperty("dspace.wielowyr.export.url");
    public static final String inforexURL = ConfigurationManager.getProperty("dspace.inforex.export.url");
    public static final String oaiUrl = ConfigurationManager.getProperty("oai.context")+"cite?metadataPrefix=cmdi&handle=";

    @javax.ws.rs.core.Context public static ServletContext servletContext;

    @GET
    @Path("/{prefix}/{suffix}/start")
    public String startProcess(
            @PathParam("prefix") Integer prefix, @PathParam("suffix") Integer suffix, @QueryParam("expand") String expand,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("userEmail") String user_email,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request){

        org.dspace.core.Context context = null;

        try {
            context = createContext(getUser(headers));
            org.dspace.content.DSpaceObject dso = HandleManager.resolveToObject(context, prefix + "/" + suffix);

            if(dso.getType() == Constants.ITEM) {
                org.dspace.content.Item dspaceItem = (Item) dso;

                List<Bitstream> bitstreams = getBitStreamsByItemId(dspaceItem, context);
                String bitstreamUrl = String.format("%s%s/bitstreams/", host, request.getContextPath());

                String handle = prefix +"/"+suffix;

                String xml = getEngineXml(bitstreams, bitstreamUrl, prefix.toString(), suffix.toString(), user_email);
                log.info(xml);
                if ("READY".equals(dspaceItem.getProcessStatus())
                        || "ERROR".equals(dspaceItem.getProcessStatus())) {

                    if (bitstreams.size() > 0) {
                        saveMetaFile(handle);
                        String nlpengineToken = callEngineService(xml);
                        log.info(nlpengineToken);
                        if (nlpengineToken != null && !nlpengineToken.isEmpty()) {
                            dspaceItem.setNLPEngineToken(nlpengineToken);
                            dspaceItem.setProcessStatus("PROCESSING");
                        }
                    }
                    context.complete();
                }

                return "Process started";
            }
        } catch (ContextException e) {
            processException("Could not read item(handle=" + prefix + "/" + suffix + "), ContextException. Message: " + e.getMessage(), context);
        } catch (SQLException e) {
            processException("Could not read item(handle=" + prefix + "/" + suffix + "), ContextException. Message: " + e.getMessage(), context);
        }
        return "Fail to start process";
    }

    @GET
    @Path("/{prefix}/{suffix}/status")
    @Produces({MediaType.APPLICATION_JSON})
    public ProcessStatusResponse processStatus(
            @PathParam("prefix") Integer prefix, @PathParam("suffix") Integer suffix, @QueryParam("expand") String expand,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request){

        org.dspace.core.Context context = null;
        org.dspace.content.Item dspaceItem = null;
        try
        {
            context = createContext(getUser(headers));
            org.dspace.content.DSpaceObject dso = HandleManager.resolveToObject(context, prefix + "/" + suffix);
            if(dso.getType() == Constants.ITEM) {
                dspaceItem = (Item) dso;
                ProcessStatusResponse status = checkStatus(dspaceItem);
                context.complete();
                log.trace("Item(handle=" + prefix + "/" + suffix + ") was successfully read.");
                return status;
            }
        }
        catch (SQLException e)
        {
            processException("Could not read item(handle=" + prefix + "/" + suffix + "), SQLException. Message: " + e, context);
        }
        catch (ContextException e)
        {
            processException("Could not read item(handle=" + prefix + "/" + suffix + "), ContextException. Message: " + e.getMessage(), context);
        }
        finally
        {
            processFinally(context);
        }

        return new ProcessStatusResponse();
    }

    @GET
    @Path("/{prefix}/{suffix}/restart")
    public String processRestart(
          @PathParam("prefix") Integer prefix, @PathParam("suffix") Integer suffix, @QueryParam("expand") String expand,
          @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
          @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
    {
        log.info("Reading item(handle=" + prefix + "/" + suffix + ").");
        org.dspace.core.Context context = null;
        try
        {
            context = createContext(getUser(headers));
            org.dspace.content.DSpaceObject dso = HandleManager.resolveToObject(context, prefix + "/" + suffix);

            if(dso.getType() == Constants.ITEM) {
                org.dspace.content.Item item = (Item) dso;
                item.setNLPEngineToken(null);
                item.setProcessStatus("READY");
                context.complete();
                log.trace("Item(handle=" + prefix + "/" + suffix + ") was successfully read.");
            }
        }
        catch (SQLException e)
        {
            processException("Could not read item(handle=" + prefix + "/" + suffix + "), SQLException. Message: " + e, context);
        }
        catch (ContextException e)
        {
            processException("Could not read item(handle=" + prefix + "/" + suffix + "), ContextException. Message: " + e.getMessage(), context);
        }
        finally
        {
            processFinally(context);
        }

        return "Process for Item handle=" + prefix + "/" + suffix +" reseted";
    }

    @GET
    @Path("/{prefix}/{suffix}/export/mewex")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String,String> exportToMewex(
            @PathParam("prefix") Integer prefix, @PathParam("suffix") Integer suffix, @QueryParam("expand") String expand,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent, @QueryParam("userEmail") String user_email,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
    {
        log.info("Reading item(handle=" + prefix + "/" + suffix+ ").");
        org.dspace.core.Context context = null;
        Map<String,String> m = new HashMap<String, String>();
        m.put("redirect", null);
        m.put("error", null);

        try
        {
            context = createContext(getUser(headers));
            org.dspace.content.DSpaceObject dso = HandleManager.resolveToObject(context, prefix + "/" + suffix);
            if(dso.getType() == Constants.ITEM) {
                org.dspace.content.Item item = (Item) dso;
                String handle = item.getHandle();
                if (item.getProcessStatus().equals("DONE")) {
                    try {
                        Map<String, String> response = callMewexService(handle, item.getName(), user_email);
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

                context.complete();

                log.trace("Item(handle=" + prefix + "/" + suffix + ") was successfully read.");
            }
        }
        catch (SQLException e)
        {
            m.put("error","Internal error: " + e.getMessage());
            processException("Could not read item(handle=" + prefix + "/" + suffix+ "), SQLException. Message: " + e, context);
        }
        catch (ContextException e)
        {
            processException("Could not read item(handle=" + prefix + "/" + suffix +"), ContextException. Message: " + e.getMessage(), context);
        }
        finally
        {
            processFinally(context);
        }

        return m;
    }


    @GET
    @Path("/{prefix}/{suffix}/export/inforex")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String,String> exportToInforex(
            @PathParam("prefix") Integer prefix, @PathParam("suffix") Integer suffix, @QueryParam("expand") String expand,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("userEmail") String user_email, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) {

        log.info("Reading item(handle=" + prefix + "/" + suffix + ").");
        org.dspace.core.Context context = null;
        Map<String, String> m = new HashMap<String, String>();
        m.put("redirect", null);
        m.put("error", null);

        try {
            context = createContext(getUser(headers));
            org.dspace.content.DSpaceObject dso = HandleManager.resolveToObject(context, prefix + "/" + suffix);

            if(dso.getType() == Constants.ITEM) {
                org.dspace.content.Item item = (Item) dso;
                String handle = item.getHandle();
                if (item.getProcessStatus().equals("DONE")) {
                    try {
                        Map<String, String> response = callInforexService(handle, item.getName(), user_email);
                        if (response.containsKey("redirect")) {
                            m.put("redirect", response.get("redirect"));
                        }
                        if (response.containsKey("error")) {
                            log.error("Error Inforex : " + response.get("error"));
                            m.put("error", response.get("error"));
                        }
                    } catch (Exception e) {
                        log.error(e);
                        m.put("error", "Internal error: " + e.getMessage());
                    }
                }

                context.complete();
                log.trace("Item(handle=" + prefix + "/" + suffix + ") was successfully read.");
            }
        } catch (SQLException e) {
            m.put("error", "Internal error: " + e.getMessage());
            processException("Could not read item(handle=" + prefix + "/" + suffix +"), SQLException. Message: " + e, context);
        } catch (ContextException e) {
            processException("Could not read item(handle=" + prefix + "/" + suffix +"), ContextException. Message: " + e.getMessage(), context);
        } finally {
            processFinally(context);
        }

        return m;
    }

    private ProcessStatusResponse checkStatus( org.dspace.content.Item item) {
        ProcessStatusResponse status = new ProcessStatusResponse();
        status.setItemId(Integer.toString(item.getID()));
        String token = item.getNLPEngineToken();
        String currentItemStatus = item.getProcessStatus();
        Map<String, Object> engineStatus = new HashMap<String, Object>();

        if (token != null && !"".equals(token)) {
            try {
                if(!currentItemStatus.equals("DONE")){
                    engineStatus = getNLPEngineStatus(token);
                    log.info(engineStatus.get("value"));
                    if (!currentItemStatus.equals(engineStatus.get("status"))) {
                        item.setProcessStatus((String) engineStatus.get("status"));
                    }
                }
                switch (ProcessStatus.valueOf(item.getProcessStatus())) {
                    case PROCESSING:
                        status.setProgress(Double.toString((Double) engineStatus.get("value")));
                        break;
                    case ERROR:
                        status.setProgress("0");
                        status.setError(String.format("Error message recived from engine: %s", (String) engineStatus.get("value")));
                        break;
                    case DONE:
                        status.setProgress("1");
                        break;
                    case READY:
                    default:
                        break;
                }
            } catch (Exception e) {
                status.setError(String.format("Error: %s", e.getMessage()));
            }
        }
        status.setNlpEngineToken(item.getNLPEngineToken());
        status.setStatus(item.getProcessStatus());
        return status;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Map getNLPEngineStatus(String token) {
        RestTemplate template = new RestTemplate();
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(nlengineTaskStatusURL + token);
        Map<String, Object> json = template.getForObject(builder.build().toUri(), Map.class);
        return json;
    }

    private void saveMetaFile(String handle){
        HttpClient client = new DefaultHttpClient();

        String metaURL = oaiUrl+handle;
        log.info(metaURL);
        HttpGet request = new HttpGet(metaURL);

        // add request header
        request.addHeader("Accept", "application/xml");

        String nfs_path = String.format("%s%s/", nfs, handle);

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
            log.error("Saving metadata file error:", e);
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    public String getEngineXml(List<Bitstream> files, String bitstreamUrl, String prefix, String suffix,  String username){

        ClarinPlEngine xml = new ClarinPlEngine();


        SourceElement src = new SourceElement("1", username);
        Set<String> names = new HashSet<String>();
        for (Bitstream stream : files) {

            try {
                URI uri = new URI(bitstreamUrl + stream.getID()+"/retrieve");
                String resultFile="";
                if(names.contains(stream.getName())){
                    resultFile = stream.getName().substring(0, stream.getName().lastIndexOf("."))+"."+stream.getSequenceID()+".ccl";
                } else {
                    resultFile = stream.getName().substring(0, stream.getName().lastIndexOf("."))+".ccl";
                    names.add(stream.getName());
                }
                src.addUrl(new UrlElement(resultFile,uri.toURL().toString()));
            } catch (URISyntaxException e) {
                log.error("Bad file url", e);
            } catch (MalformedURLException e) {
                log.error("Bad file url", e);
            }
        }
        xml.setSource(src);
        xml.getActivities().add(new ActivityElement("2", "any2text", "1", null));
        xml.getActivities().add(new ActivityElement("3", "wcrft2large", "2", null));
        xml.getActivities().add(new ActivityElement("4", "liner2_large", "3", "'{\"model\":\"all\"}'"));
        xml.getActivities().add(new ActivityElement("5", "wsd2", "4", null));
        xml.setAgregate(new AgregateElement("6", zip_name, "zip", "5"));
        xml.setOutput(new OutputElement("7", "6", new DspaceElement("nfs", suffix, prefix, "Plik CCL")));

        java.io.StringWriter sw = new StringWriter();
        sw.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
        try {
            JAXBContext context = JAXBContext.newInstance(ClarinPlEngine.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(xml, sw);
        } catch (JAXBException e) {
            processException("Could not marshall, JAXBException. Message: " + e, null);
        }

        return sw.toString();
    }

    public List<Bitstream> getBitStreamsByItemId( org.dspace.content.Item dspaceItem, org.dspace.core.Context context){
        List<Bitstream> files = new ArrayList<Bitstream>();

        Bundle[] originals = new Bundle[0];
        try {
            originals = dspaceItem.getBundles("ORIGINAL");

            for (Bundle original : originals) {
                Bitstream[] bss = original.getBitstreams();
                for (Bitstream bitstream : bss) {
                    String filename = bitstream.getName();

                    if (bitstream.getName().matches(ALLOWED_FILES)) {
                        files.add(bitstream);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return files;
    }

    private Map<String,String> callInforexService(String handle, String itemName, String userEmail) throws SQLException, ParseException {
        RestTemplate template = new RestTemplate();

        String nfs_file = String.format("%s%s/%s", nfs, handle, zip_name);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(inforexURL);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("ajax", "dspace_import");
        map.add("name", itemName);
        map.add("email", userEmail);
        map.add("path", nfs_file);
        String res = template.postForObject(builder.build().toUri(), map, String.class);
        JSONObject json = (JSONObject)new JSONParser().parse(res);
        return json;

    }

    private Map callMewexService(String handle, String itemName, String userEmail){
        RestTemplate template = new RestTemplate();

        String nfs_file = String.format("%s%s/%s", nfs, handle, zip_name);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(mewexURL);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("name", itemName);
        map.add("email", userEmail);
        map.add("url", nfs_file);

        String res = template.postForObject(builder.build().toUri(), map, String.class);

        JSONObject json = null;
        try {
            json = (JSONObject)new JSONParser().parse(res);
        } catch (ParseException e) {
            log.error("Parsing MewexService:", e);
        }
        return json;

    }

    public String callEngineService(String xml) {

        HttpClient client = new DefaultHttpClient();
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(nlengineTaskStartURL);
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
        } finally {
            client.getConnectionManager().shutdown();
        }
        return null;
    }

    public enum ProcessStatus {
        READY, PROCESSING, ERROR, DONE
    }
}
