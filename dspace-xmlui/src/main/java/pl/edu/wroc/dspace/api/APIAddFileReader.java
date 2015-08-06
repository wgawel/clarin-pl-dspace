/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package pl.edu.wroc.dspace.api;

import org.dspace.app.xmlui.cocoon.*;
import com.google.gson.Gson;
import cz.cuni.mff.ufal.dspace.content.Handle;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.reading.AbstractReader;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.dspace.vocabulary.ControlledVocabulary;
import org.xml.sax.SAXException;
import org.apache.cocoon.servlet.multipart.Part;
import org.apache.cocoon.servlet.multipart.PartOnDisk;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import javax.servlet.http.HttpSession;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.components.flow.FlowHelper;
import org.apache.cocoon.components.flow.WebContinuation;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.dspace.app.itemupdate.AddBitstreamsAction;
import org.dspace.app.itemupdate.ItemArchive;
import org.dspace.app.xmlui.aspect.administrative.FlowResult;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authenticate.AuthenticationManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import pl.edu.wroc.dspace.api.result.IJsonData;
import pl.edu.wroc.dspace.api.result.ResultError;
import pl.edu.wroc.dspace.api.result.ResultSuccess;
import pl.edu.wroc.dspace.api.result.ResultSuccessAddFile;
import pl.edu.wroc.dspace.api.result.elements.ApiBitstream;

/**
 *
 * @author Wojciech Gaweł (wojtekgaw at gmail com)
 */
public class APIAddFileReader extends AbstractReader {
    
    protected Map objectModel;

    protected Context context;

    protected String contextPath;

    protected String servletPath;

    protected String sitemapURI;

    protected String url;
    
    protected Parameters parameters;
    
    protected EPerson eperson;
    
    protected WebContinuation knot;

    private static final Logger log = Logger.getLogger(APIAddFileReader.class);
    
    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters parameters) throws ProcessingException, SAXException, IOException {
        log.debug("APIAddFileReader - setup");
        
        this.objectModel = objectModel;
        this.parameters = parameters;
        try {
            this.context = ContextUtil.obtainContext(objectModel);
        } catch (SQLException ex) {
            log.error(ex);
        }
        this.eperson = context.getCurrentUser();
        Request request = ObjectModelHelper.getRequest(objectModel);
        this.contextPath = request.getContextPath();
        if (contextPath == null)
        {
            contextPath = "/";
        }

        this.servletPath = request.getServletPath();
        this.sitemapURI = request.getSitemapURI(); 
        this.knot = FlowHelper.getWebContinuation(objectModel);
    }

    @Override
    public void generate() throws IOException, SAXException, ProcessingException {
        log.debug("APIAddFileReader - start");
        Request request = ObjectModelHelper.getRequest(objectModel);
        
        try {
            int repositoryId = 0;
            try {
                repositoryId = Integer.parseInt(request.getParameter("repo_id"));
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("'repo_id' is not a number.");
            }
            if (repositoryId == 0) {
                throw new IllegalArgumentException("'repo_id' is not valid repository number (repo_id = 0).");
            }
            
            String itemId = request.getParameter("item_id");
            if (itemId == null || itemId.isEmpty()) {
                throw new IllegalArgumentException("'item_id' is empty");
            }
            
            String fileDescription = request.getParameter("file_description");
            
            Part filePart = (Part) request.get("file");
            if (filePart == null) {
                throw new IllegalArgumentException("No file found.");
            }
            
            String fileName = filePart.getFileName();
            
            login(request);
            Bitstream newBitstream = addFileFromPostToItem(repositoryId, itemId, filePart, fileName, fileDescription);

            String jsonString = createJsonResultAddBitstreamSuccess(newBitstream);
            
            
            ByteArrayInputStream inputStream = new ByteArrayInputStream(jsonString.getBytes("UTF-8"));
            IOUtils.copy(inputStream, out);
            out.flush();
        } catch (Exception e) {
            log.error("Error while addFileFromPostToItem: " + e + (ExceptionUtils.getFullStackTrace(e)));
            String jsonString = createJsonResultError("Error while addFileFromPostToItem: " + e);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(jsonString.getBytes("UTF-8"));
            IOUtils.copy(inputStream, out);
            out.flush();
        }
    }
    
    public Bitstream addFileFromPostToItem(int repositoryId, String handlePartItemId, Part filePart, String fileName, String fileDescription) throws SQLException, Exception {
        
//        ItemArchive itarch = ItemArchive.create(context, file, null);
//        boolean isTest = false;
//        boolean suppressUndo = false;
//        
//        AddBitstreamsAction action = new AddBitstreamsAction();
//        action.execute(context, itarch, isTest, suppressUndo);
                
        // Based on:
        // sources/dspace-xmlui/dspace-xmlui-api/src/main/java/org/dspace/app/xmlui/aspect/administrative/FlowItemUtils.java:processAddBitstream
                
        // public static FlowResult processAddBitstream(Context context, int itemID, Request request) 

        // Upload a new file
        log.info("addFileFromPostToItem: try to find handle: \""+repositoryId+"/"+handlePartItemId+"\"");
        Handle handle = Handle.findByHandle(context, repositoryId+"/"+handlePartItemId);
        if (handle == null) {
            throw new IllegalArgumentException("Handle \""+repositoryId+"/"+handlePartItemId+"\" not found.");
        }
        log.info("addFileFromPostToItem: handle found, resource_id = \""+handle.getResourceID()+"\"");
        Item item = Item.find(context, handle.getResourceID());
        log.info("addFileFromPostToItem: found item with name = \""+item.getName()+"\"");

        InputStream is = null;
        String upload_name = null;
        if (filePart != null && filePart.getSize() > 0) {
            is = filePart.getInputStream();
            upload_name = filePart.getUploadName();
        } else {
//            try {
//                String filePath = request.getParameter("file_local");
//                if (filePath.startsWith("/")) {
//                    filePath = "file://" + filePath;
//                }
//                URL url = new URI(filePath).toURL();
//                is = url.openStream();
//                upload_name = filePath;
//            } catch (URISyntaxException e) {
//            }
        }

        if (is != null) {

            String bundleName = "ORIGINAL";

            Bitstream bitstream;
            Bundle[] bundles = item.getBundles(bundleName);
            if (bundles.length < 1) {
                // set bundle's name to ORIGINAL
                bitstream = item.createSingleBitstream(is, bundleName);

                // set the permission as defined in the owning collection
                Collection owningCollection = item.getOwningCollection();
                if (owningCollection != null) {
                    Bundle bnd = bitstream.getBundles()[0];
                    bnd.inheritCollectionDefaultPolicies(owningCollection);
                }
            } else {
                // we have a bundle already, just add bitstream
                bitstream = bundles[0].createBitstream(is);
            }

            String upload_name_stripped = upload_name;
            while (upload_name_stripped.indexOf('/') > -1) {
                upload_name_stripped = upload_name_stripped.substring(upload_name_stripped.indexOf('/') + 1);
            }

            while (upload_name_stripped.indexOf('\\') > -1) {
                upload_name_stripped = upload_name_stripped.substring(upload_name_stripped.indexOf('\\') + 1);
            }

            bitstream.setName(upload_name_stripped);
            bitstream.setSource(upload_name);
            bitstream.setDescription(fileDescription);

            // Identify the format
            BitstreamFormat format = FormatIdentifier.guessFormat(context, bitstream);
            bitstream.setFormat(format);

            // Update to DB
            bitstream.update();
            item.store_provenance_info("DSpace API: Item was added a bitstream", context.getCurrentUser());
            item.update();

            context.commit();
            
            return bitstream;
        } else {
            throw new RuntimeException("No File found.");
        }
    }
    
    private void login(Request request) throws SQLException, AuthorizeException {
        EPerson loginAsEPerson = EPerson.findByEmail(context, "wojtekgaw+dspace_sapi@gmail.com"); //TODO: add auth methid OR move to configuration
        
        // Success, allow the user to login as another user.
	context.setCurrentUser(loginAsEPerson);
	
        // Set any special groups - invoke the authentication mgr.
        int[] groupIDs = AuthenticationManager.getSpecialGroups(context,request);
        for (int groupID : groupIDs)
        {
            context.setSpecialGroup(groupID);
        }
	HttpSession session = request.getSession(false);
        // Set both the effective and authenticated user to the same.
        session.setAttribute("dspace.user.effective", loginAsEPerson.getID());
    }
    
    private String createJsonResultAddBitstreamSuccess(Bitstream bitstream) {
        ApiBitstream apiBitstream = new ApiBitstream(bitstream);
        return createJsonResultSuccess(apiBitstream);
    }
    
    private static String createJsonResultSuccess(ApiBitstream data) {
        ResultSuccessAddFile result = new ResultSuccessAddFile(data);
        return result.toJson();
    }

    private static String createJsonResultError(String errorText) {
        ResultError result = new ResultError(errorText);
        return result.toJson();
    }
}
