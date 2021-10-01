package org.dspace.rest;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.handle.HandleManager;
import org.dspace.rest.common.clarinpl.*;
import org.dspace.rest.exceptions.ContextException;
import org.dspace.usage.UsageEvent;
import org.json.JSONArray;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.StandardCopyOption;
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
    public static final String nfs = ConfigurationManager.getProperty("dspace.nfs.path");
    public static final String mewexURL = ConfigurationManager.getProperty("dspace.wielowyr.export.url");
    public static final String inforexURL = ConfigurationManager.getProperty("dspace.inforex.export.url");
    public static final String oaiUrl = ConfigurationManager.getProperty("oai.context")+"cite?metadataPrefix=cmdi&handle=";

    @javax.ws.rs.core.Context public ServletContext servletContext;

    @GET
    @Path("/{prefix}/{suffix}/start")
    public String startProcess(
            @PathParam("prefix") Integer prefix, @PathParam("suffix") Integer suffix, @QueryParam("expand") String expand,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("userEmail") String user_email,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request){

        org.dspace.core.Context context = null;

        try {
            context = createContext(headers);
            org.dspace.content.DSpaceObject dso = HandleManager.resolveToObject(context, prefix + "/" + suffix);

            if(dso.getType() == Constants.ITEM) {
                org.dspace.content.Item dspaceItem = (Item) dso;

                List<Bitstream> bitstreams = getBitStreamsByItemId(dspaceItem, context);
                String bitstreamUrl = String.format("%s%s/bitstreams/", host, request.getContextPath());

                String handle = prefix +"/"+suffix;

                String json = getEngineJSON(bitstreams, bitstreamUrl, prefix.toString(), suffix.toString(), user_email);
                log.info(json);
                if ("READY".equals(dspaceItem.getProcessStatus())
                        || "ERROR".equals(dspaceItem.getProcessStatus())) {

                    if (bitstreams.size() > 0) {
                        saveMetaFile(handle);
                        String nlpengineToken = callEngineService(json);
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
        } catch (IOException e) {
            e.printStackTrace();
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
            context = createContext(headers);
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
            context = createContext(headers);
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
    @Path("/{prefix}/{suffix}/add/kontext")
    public String addToKontext( @PathParam("prefix") Integer prefix, @PathParam("suffix") Integer suffix, @QueryParam("expand") String expand,
                                @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
                                @QueryParam("userEmail") String user_email,
                                @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request){

        log.info("Reading item(handle=" + prefix + "/" + suffix + ").");
        org.dspace.core.Context context = null;

        try {
            context = createContext(headers);
            org.dspace.content.DSpaceObject dso = HandleManager.resolveToObject(context, prefix + "/" + suffix);

            if(dso.getType() == Constants.ITEM) {
                org.dspace.content.Item dspaceItem = (Item) dso;

                String handle = prefix +"/"+suffix;

                String json = callKontextService(handle,dspaceItem.getName(),user_email, "", "");
                log.info(json);
                String job = callEngineService(json);
                dspaceItem.setInKontextTermArchive(true);
                log.info(job);
                context.complete();
                return "Export to Kontext started job: " + job;
            }
        } catch (ContextException e) {
            processException("Could not read item(handle=" + prefix + "/" + suffix + "), ContextException. Message: " + e.getMessage(), context);
        } catch (SQLException e) {
            processException("Could not read item(handle=" + prefix + "/" + suffix + "), ContextException. Message: " + e.getMessage(), context);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Fail to start process";
    }

    @GET
    @Path("/{prefix}/{suffix}/add/archive")
    public String addToArchive(
            @PathParam("prefix") Integer prefix, @PathParam("suffix") Integer suffix, @QueryParam("expand") String expand,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
    {
        log.info("Reading item(handle=" + prefix + "/" + suffix + ").");
        org.dspace.core.Context context = null;
        try
        {
            context = createContext(headers);
            org.dspace.content.DSpaceObject dso = HandleManager.resolveToObject(context, prefix + "/" + suffix);

            if(dso.getType() == Constants.ITEM) {
                org.dspace.content.Item item = (Item) dso;
                item.setLongTermArchive(true);
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

        return "Added to Archive Item handle=" + prefix + "/" + suffix;
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
            context = createContext(headers);
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
            context = createContext(headers);
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

    @GET
    @Path("/{prefix}/{suffix}/ccl")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCCL(
            @PathParam("prefix") Integer prefix, @PathParam("suffix") Integer suffix, @QueryParam("expand") String expand,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("userEmail") String user_email, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) {

        log.info("Reading item(handle=" + prefix + "/" + suffix + "/ccl).");

        org.dspace.core.Context context = null;
        InputStream inputStream = null;

        try {
            context = createContext(headers);
            org.dspace.content.DSpaceObject dso = HandleManager.resolveToObject(context, prefix + "/" + suffix);

            if(dso.getType() == Constants.ITEM) {
                org.dspace.content.Item item = (Item) dso;

                String handle = item.getHandle();
                String nfs_path = String.format("%s%s/", nfs, handle);

                File file = new File(nfs_path + zip_name);
                if (file.exists()) {
                    inputStream = new FileInputStream(file);
                } else {
                    context.abort();
                    return Response.status(Response.Status.NOT_FOUND).build();
                }
                context.complete();
                log.trace("Item(handle=" + prefix + "/" + suffix + ") was successfully read.");
            }
        } catch (Exception e) {
            log.error("Could not read item. Item may not exist (handle=" + prefix + "/" + suffix +"/ccl" +"), ContextException. Message: ");
            return Response.status(Response.Status.NOT_FOUND).type("application/json").build();
        } finally {
            try {
                context.complete();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return Response.ok(inputStream).type("application/zip").build();
    }


    @POST
    @Path("/{prefix}/{suffix}/ccl")
    @Produces(MediaType.APPLICATION_JSON)
    public Response loadCCL(
            @PathParam("prefix") Integer prefix, @PathParam("suffix") Integer suffix,
            InputStream is,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("userEmail") String user_email, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) {

        log.info("Reading item(handle=" + prefix + "/" + suffix + "/ccl).");
        log.info("IP: " + user_ip);
        org.dspace.core.Context context = null;

        try {
            context = createContext(headers);
            org.dspace.content.DSpaceObject dso = HandleManager.resolveToObject(context, prefix + "/" + suffix);

            if(dso.getType() == Constants.ITEM) {
                org.dspace.content.Item item = (Item) dso;
                String handle = item.getHandle();

                String nfs_path = String.format("%s%s/", nfs, handle);


                File path = new File(nfs_path);
                path.getAbsoluteFile().mkdirs();

                File targetFile = new File(nfs_path + zip_name);
                if (!targetFile.exists()) {
                    targetFile.createNewFile();
                }

                java.nio.file.Files.copy(
                        is,
                        targetFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
                context.complete();
                log.trace("Item(handle=" + prefix + "/" + suffix + ") was successfully read.");
            }
        } catch (Exception e) {
            log.error("Could not load file item not exist (handle=" + prefix + "/" + suffix +"/ccl" +"), ContextException. Message: ");
            return Response.status(Response.Status.NOT_FOUND).type("application/json").build();
        } finally {
            try {
                context.complete();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return Response.ok().build();
    }


    private ProcessStatusResponse checkStatus( org.dspace.content.Item item) {
        ProcessStatusResponse status = new ProcessStatusResponse();
        status.setHandle(item.getHandle());
        String token = item.getNLPEngineToken();
        String currentItemStatus = item.getProcessStatus();
        Map<String, Object> engineStatus = new HashMap<>();

        if (token != null && !"".equals(token)) {
            try {
                if(!currentItemStatus.equals("DONE")){
                    engineStatus = getNLPEngineStatus(token);
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
                    case NOTEXISTING:
                        status.setError(String.format("Error message recived from engine: %s", (String) engineStatus.get("value")));
                        break;
                    case QUEUE:
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
        xml.getActivities().add(new ActivityElement("2", "any2txt", "1", null));
        xml.getActivities().add(new ActivityElement("3", "wcrft2large", "2", null));
        xml.getActivities().add(new ActivityElement("4", "liner2_large", "3", "{\"model\":\"all\"}"));
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

    private static final String lpmnString="any2txt|wcrft2({\"morfeusz2\":false})|liner2|wsd|dir|makezip";

    public String getEngineJSON(List<Bitstream> files, String bitstreamUrl, String prefix, String suffix, String username) {

        JSONObject request = new JSONObject();
        request.put("user", username);
        request.put("application", "dspace");

        String lpmn = "urls(";

        Set<String> names = new HashSet<>();
        JSONArray urls = new JSONArray();
        for (Bitstream stream : files) {

            try {
                URI uri = new URI(bitstreamUrl + stream.getID() + "/retrieve?service=true");
                String resultFile = "";
                if (names.contains(stream.getName())) {
                    resultFile = stream.getName().substring(0, stream.getName().lastIndexOf(".")) + "." + stream.getSequenceID() + ".ccl";
                } else {
                    resultFile = stream.getName().substring(0, stream.getName().lastIndexOf(".")) + ".ccl";
                    names.add(stream.getName());
                }
                JSONObject el = new JSONObject();
                el.put("name", resultFile);
                el.put("url", uri.toURL().toString());
                urls.put(el);

            } catch (URISyntaxException e) {
                log.error("Bad file url", e);
            } catch (MalformedURLException e) {
                log.error("Bad file url", e);
            }
        }
        lpmn += urls.toString();
        lpmn += ")|"+lpmnString+"|todspace(/" + prefix + "/" + suffix + "/)";
        request.put("lpmn", lpmn);

        return request.toString();
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

    protected String callKontextService(String handle, String itemName, String userName, String language, String info){

        JSONObject request = new JSONObject();
        request.put("user", userName);
        request.put("application", "dspace");

        StringBuilder  lpmn = new StringBuilder();
        lpmn.append("dspacezip(/")
               .append(handle)
               .append("/)|dir|ccl2vert|comcorp(")
               .append("{\"add_to_kontext\": true,")
               .append("\"user_email\":\"").append(userName).append("\",")
               .append("\"corpus_data\": {")
               .append("\"id\": \"").append(handle.replace("/","_")).append("\",")
               .append("\"name\": \"").append(itemName).append("\",")
               .append("\"info\": \"").append(info).append("\",")
               .append("\"encoding\": \"UTF-8\",")
               .append("\"lang\": \"").append(language).append("\" }})");
        request.put("lpmn", lpmn.toString());
        return request.toString();
    }

        private Map callMewexService(String handle, String itemName, String userEmail){
        RestTemplate template = new RestTemplate();

        String nfs_file = String.format("%s%s/%s", nfs, handle, zip_name);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(mewexURL);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("name", itemName);
        map.add("email", userEmail);
        map.add("url", nfs_file);
        log.debug("Call MEWEX->" + map);
        String res = template.postForObject(builder.build().toUri(), map, String.class);

        JSONObject json = null;
        try {
            json = (JSONObject)new JSONParser().parse(res);
        } catch (ParseException e) {
            log.error("Parsing MewexService:", e);
        }
        log.debug("Response MEWEX->" + json);
        return json;

    }

    public String callEngineServiceXML(String xml) {

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

    public String callEngineService(String json) throws IOException {

        HttpClient client = new DefaultHttpClient();
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(nlengineTaskStartURL);
        try {

            HttpPost post = new HttpPost(builder.build().toUri());
            StringEntity entity = new StringEntity(json, "UTF-8");
            entity.setChunked(true);
            entity.setContentType("application/json");

            post.setEntity(entity);
            post.setHeader("Content-Type", "application/json; charset=UTF-8");

            HttpResponse response = client.execute(post);

            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            return rd.readLine();
        } catch (UnsupportedEncodingException e) {
            log.error("Bad encoding ", e);
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
        READY, QUEUE, PROCESSING, ERROR, DONE, NOTEXISTING
    }
}
