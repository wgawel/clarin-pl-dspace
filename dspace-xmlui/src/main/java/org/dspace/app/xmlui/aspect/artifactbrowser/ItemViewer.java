/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.artifactbrowser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import cz.cuni.mff.ufal.DSpaceApi;
import cz.cuni.mff.ufal.lindat.utilities.interfaces.IFunctionalities;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.cocoon.util.HashUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.excalibur.source.SourceValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Metadatum;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.app.util.GoogleMetadata;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.core.PluginManager;
import org.dspace.services.ConfigurationService;
import org.jdom.Element;
import org.jdom.Text;
import org.jdom.output.XMLOutputter;
import org.xml.sax.SAXException;
import org.dspace.core.ConfigurationManager;
import org.dspace.app.sfx.SFXFileReader;
import org.dspace.app.xmlui.wing.element.Metadata;
import org.dspace.utils.DSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Display a single item.
 *
 * @author Scott Phillips
 */
public class ItemViewer extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    /** Language strings */
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");

    private static final Message T_trail =
        message("xmlui.ArtifactBrowser.ItemViewer.trail");

    private static final Message T_show_simple =
        message("xmlui.ArtifactBrowser.ItemViewer.show_simple");

    private static final Message T_show_full =
        message("xmlui.ArtifactBrowser.ItemViewer.show_full");

    private static final Message T_head_parent_collections =
        message("xmlui.ArtifactBrowser.ItemViewer.head_parent_collections");

    private static final Message T_withdrawn = message("xmlui.ArtifactBrowser.ItemViewer.withdrawn");
    
	/** Cached validity object */
	private SourceValidity validity = null;

	/** XHTML crosswalk instance */
	private DisseminationCrosswalk xHTMLHeadCrosswalk = null;

	private final String sfxFile = ConfigurationManager.getProperty("dspace.dir")
            + File.separator + "config" + File.separator + "sfx.xml";

    private static final Logger log = LoggerFactory.getLogger(ItemViewer.class);

    private ConfigurationService cs = new DSpace().getConfigurationService();

    private final IFunctionalities functionalityManager = DSpaceApi.getFunctionalityManager();

    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    @Override
    public Serializable getKey() {
        try {
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

            if (dso == null)
            {
                return "0"; // no item, something is wrong.
            }

            return HashUtil.hash(dso.getHandle() + "full:" + showFullItem(objectModel));
        }
        catch (SQLException sqle)
        {
            // Ignore all errors and just return that the component is not cachable.
            return "0";
        }
    }

    /**
     * Generate the cache validity object.
     *
     * The validity object will include the item being viewed,
     * along with all bundles & bitstreams.
     */
    @Override
    public SourceValidity getValidity()
    {
        DSpaceObject dso = null;

        if (this.validity == null)
    	{
	        try {
	            dso = HandleUtil.obtainHandle(objectModel);

	            DSpaceValidity validity = new DSpaceValidity();
	            validity.add(dso);
	            this.validity =  validity.complete();
	        }
	        catch (Exception e)
	        {
	            // Ignore all errors and just invalidate the cache.
	        }

    	}
    	return this.validity;
    }

    /** Matches Handle System URIs. */
    private static final Pattern handlePattern = Pattern.compile(
            "hdl:|https?://hdl\\.handle\\.net/", Pattern.CASE_INSENSITIVE);

    /** Matches DOI URIs. */
    private static final Pattern doiPattern = Pattern.compile(
            "doi:|https?://(dx\\.)?doi\\.org/", Pattern.CASE_INSENSITIVE);

    /**
     * Add the item's title and trail links to the page's metadata.
     */
    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (!(dso instanceof Item))
        {
            return;
        }

        Item item = (Item) dso;

        // Set the page title
        String title = getItemTitle(item);

        if (title != null)
        {
            pageMeta.addMetadata("title").addContent(title);
        }
        else
        {
            pageMeta.addMetadata("title").addContent(item.getHandle());
        }

        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        HandleUtil.buildHandleTrail(item,pageMeta,contextPath);
        pageMeta.addTrail().addContent(T_trail);

        // Add SFX link
        String sfxserverUrl = ConfigurationManager.getProperty("sfx.server.url");
        if (sfxserverUrl != null && sfxserverUrl.length() > 0)
        {
            String sfxQuery = "";

            // parse XML file -> XML document will be build
            sfxQuery = SFXFileReader.loadSFXFile(sfxFile, item);

            // Remove initial &, if any
            if (sfxQuery.startsWith("&"))
            {
                sfxQuery = sfxQuery.substring(1);
            }
            sfxserverUrl = sfxserverUrl.trim() +"&" + sfxQuery.trim();
            pageMeta.addMetadata("sfx","server").addContent(sfxserverUrl);
        }
        
        // Add persistent identifiers
        /* Temporarily switch to using metadata directly.
         * FIXME Proper fix is to have IdentifierService handle all durable
         * identifiers, whether minted here or elsewhere.
        List<IdentifierProvider> idPs = new DSpace().getServiceManager()
                .getServicesByType(IdentifierProvider.class);
        for (IdentifierProvider idP : idPs)
        {
            log.debug("Looking up Item {} by IdentifierProvider {}",
                    dso.getID(), idP.getClass().getName());
            try {
                String id = idP.lookup(context, dso);
                log.debug("Found identifier {}", id);
                String idType;
                String providerName = idP.getClass().getSimpleName().toLowerCase();
                if (providerName.contains("handle"))
                    idType = "handle";
                else if (providerName.contains("doi"))
                    idType = "doi";
                else
                {
                    log.info("Unhandled provider {}", idP.getClass().getName());
                    continue;
                }
                log.debug("Adding identifier of type {}", idType);
                Metadata md = pageMeta.addMetadata("identifier", idType);
                md.addContent(id);
            } catch (IdentifierNotFoundException | IdentifierNotResolvableException ex) {
                continue;
            }
        }
        */
        String identifierField = new DSpace().getConfigurationService()
                .getPropertyAsType("altmetrics.field", "dc.identifier.uri");
        for (Metadatum uri : dso.getMetadataByMetadataString(identifierField))
        {
            String idType, idValue;
            Matcher handleMatcher = handlePattern.matcher(uri.value);
            Matcher doiMatcher = doiPattern.matcher(uri.value);
            if (handleMatcher.lookingAt())
            {
                idType = "handle";
                idValue = uri.value.substring(handleMatcher.end());
            }
            else if (doiMatcher.lookingAt())
            {
                idType = "doi";
                idValue = uri.value.substring(doiMatcher.end());
            }
            else
            {
                log.info("Unhandled identifier URI {}", uri.value);
                continue;
            }
            log.debug("Adding identifier of type {}", idType);
            Metadata md = pageMeta.addMetadata("identifier", idType);
            md.addContent(idValue);
        }

        String sfxserverImg = ConfigurationManager.getProperty("sfx.server.image_url");
        if (sfxserverImg != null && sfxserverImg.length() > 0)
        {
            pageMeta.addMetadata("sfx","image_url").addContent(sfxserverImg);
        }

        boolean googleEnabled = ConfigurationManager.getBooleanProperty(
            "google-metadata.enable", false);

        if (googleEnabled)
        {
            // Add Google metadata field names & values to DRI
            GoogleMetadata gmd = new GoogleMetadata(context, item);

            for (Entry<String, String> m : gmd.getMappings())
            {
                pageMeta.addMetadata(m.getKey()).addContent(m.getValue());
            }
        }

        // Metadata for <head> element
        if (xHTMLHeadCrosswalk == null)
        {
            xHTMLHeadCrosswalk = (DisseminationCrosswalk) PluginManager.getNamedPlugin(
              DisseminationCrosswalk.class, "XHTML_HEAD_ITEM");
        }

        // Produce <meta> elements for header from crosswalk
        try
        {
            List l = xHTMLHeadCrosswalk.disseminateList(item);
            StringWriter sw = new StringWriter();

            XMLOutputter xmlo = new XMLOutputter();
            xmlo.output(new Text("\n"), sw);
            for (int i = 0; i < l.size(); i++)
            {
                Element e = (Element) l.get(i);
                // FIXME: we unset the Namespace so it's not printed.
                // This is fairly yucky, but means the same crosswalk should
                // work for Manakin as well as the JSP-based UI.
                e.setNamespace(null);
                xmlo.output(e, sw);
                xmlo.output(new Text("\n"), sw);
            }
            pageMeta.addMetadata("xhtml_head_item").addContent(sw.toString());
        }
        catch (CrosswalkException ce)
        {
            // TODO: Is this the right exception class?
            throw new WingException(ce);
        }

        addGoogleDatasetInfo(pageMeta, item);

        addCommandLineInfo(pageMeta, item);
    }

    private void addCommandLineInfo(PageMeta pageMeta, Item item) throws WingException {
        final Bitstream[] bitstreams;
        try {
            bitstreams = item.getNonInternalBitstreams();
            if(bitstreams.length > 0){
                functionalityManager.openSession();
                if(functionalityManager.isUserAllowedToAccessTheResource(-1, bitstreams[0].getID())) {
                    pageMeta.addMetadata("include-library", "cmdline-info");
                }
                functionalityManager.closeSession();
            }
        } catch (SQLException e) {
            log.error("Exception while fetching bitstream of item " + item.getID(), e);
        }
    }

    private void addGoogleDatasetInfo(PageMeta pageMeta, Item item) throws WingException {
        if(shouldAddGoogleDatasetMetadataForItem(item)){
            pageMeta.addMetadata("google_dataset").addContent(new GoogleDataset(item).toString());
        }
    }

    private boolean shouldAddGoogleDatasetMetadataForItem(Item item) {
        boolean datasetEnabled = cs.getPropertyAsType("google-dataset.enable", false);
        if(!datasetEnabled){
            return false;
        }

        boolean withBitstreamOnly = cs.getPropertyAsType("google-dataset.onlyItemsWithBitstreams", true);
        if(withBitstreamOnly){
            try {
                boolean hasBitstream = item.getNonInternalBitstreams().length > 0;
                if(!hasBitstream){
                    return false;
                }
            } catch (SQLException e) {
                log.error("Error while looking for bitstreams.", e);
            }
        }

        String blacklistedTypes = cs.getProperty("google-dataset.blacklistedTypes");
        if(isNotBlank(blacklistedTypes)){
            String[] types = blacklistedTypes.split(",");
            String itemType = item.getMetadata("dc.type");
            if(itemType != null){
                for(String type : types){
                    if(itemType.equals(type.trim())){
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Display a single item
     */
    @Override
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (!(dso instanceof Item))
        {
            return;
        }

        Item item = (Item) dso;

        // Build the item viewer division.
        Division division = body.addDivision("item-view","primary");
        String title = getItemTitle(item);
        if (title != null)
        {
            division.setHead(title);
        }
        else
        {
            division.setHead(item.getHandle());
        }

        // Add Withdrawn Message if it is
        if(item.isWithdrawn()){
            Division div = division.addDivision("notice", "alert");
            Para p = div.addPara();
            p.addContent(T_withdrawn);
            //Set proper response. Return "404 Not Found"
            HttpServletResponse response = (HttpServletResponse)objectModel
                    .get(HttpEnvironment.HTTP_RESPONSE_OBJECT);   
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Para showfullPara = division.addPara(null, "item-view-toggle item-view-toggle-top");

        if (showFullItem(objectModel))
        {
            String link = contextPath + "/handle/" + item.getHandle();
            showfullPara.addXref(link).addContent(T_show_simple);
        }
        else
        {
            String link = contextPath + "/handle/" + item.getHandle()
                    + "?show=full";
            showfullPara.addXref(link).addContent(T_show_full);
        }

        ReferenceSet referenceSet;
        if (showFullItem(objectModel))
        {
            referenceSet = division.addReferenceSet("collection-viewer",
                    ReferenceSet.TYPE_DETAIL_VIEW);
        }
        else
        {
            referenceSet = division.addReferenceSet("collection-viewer",
                    ReferenceSet.TYPE_SUMMARY_VIEW);
        }

        // Reference the actual Item
        ReferenceSet appearsInclude = referenceSet.addReference(item).addReferenceSet(ReferenceSet.TYPE_DETAIL_LIST,null,"hierarchy");
        appearsInclude.setHead(T_head_parent_collections);

        // Reference all collections the item appears in.
        for (Collection collection : item.getCollections())
        {
            appearsInclude.addReference(collection);
        }

        showfullPara = division.addPara(null,"item-view-toggle item-view-toggle-bottom");

        if (showFullItem(objectModel))
        {
            String link = contextPath + "/handle/" + item.getHandle();
            showfullPara.addXref(link).addContent(T_show_simple);
        }
        else
        {
            String link = contextPath + "/handle/" + item.getHandle()
                    + "?show=full";
            showfullPara.addXref(link).addContent(T_show_full);
        }
    }

    /**
     * Determine if the full item should be referenced or just a summary.
     */
    public static boolean showFullItem(Map objectModel)
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String show = request.getParameter("show");

        if (show != null && show.length() > 0)
        {
            return true;
        }

        return false;
    }

    /**
     * Obtain the item's title.
     */
    public static String getItemTitle(Item item)
    {
        Metadatum[] titles = item.getDC("title", Item.ANY, Item.ANY);

        String title;
        if (titles != null && titles.length > 0)
        {
            title = titles[0].value;
        }
        else
        {
            title = null;
        }
        return title;
    }

    /**
     * Recycle
     */
    @Override
    public void recycle() {
    	this.validity = null;
    	super.recycle();
    }

    private static class GoogleDataset{
        private static final Set<String> mandatoryFields = new HashSet<>();
        private static final HashMap<String, String> google2dspace = new HashMap<>();
        // https://developers.google.com/search/docs/data-types/dataset#dataset specifies that description must be
        // between 50 and 5000 characters
        public static final int minDescriptionLength = 50;
        public static final int maxDescriptionLength = 5000;

        static {
            mandatoryFields.add("name");
            mandatoryFields.add("description");

            google2dspace.put("name", "dc.title");
            google2dspace.put("description", "dc.description");

            String pathToProps = new DSpace().getConfigurationService().getPropertyAsType("google-metadata.config", "");
            if(!pathToProps.trim().isEmpty()) {
                Properties properties = new Properties();
                try {
                    BufferedReader reader = Files.newBufferedReader(Paths.get(pathToProps), StandardCharsets.UTF_8);
                    properties.load(reader);
                } catch (IOException e) {
                    log.error("Exception while reading google-metadata.config", e);
                }
                for (String key : properties.stringPropertyNames()) {
                    if (key.startsWith("google.dataset_")) {
                        String googleKey = key.replace("google.dataset_", "").trim();
                        google2dspace.put(googleKey, properties.getProperty(key).trim());
                    }
                }
            }
        }


        private JsonObject jsonObject;
        GoogleDataset(Item item){
            jsonObject = new JsonObject();
            jsonObject.addProperty("@context", "https://schema.org");
            jsonObject.addProperty("@type", "Dataset");
            for(Entry<String, String> entry : google2dspace.entrySet()){
                String googleKey = entry.getKey();
                String dspaceField = entry.getValue();
                Metadatum[] metadata = item.getMetadataByMetadataString(dspaceField);
                if(metadata.length == 1){
                    jsonObject.addProperty(googleKey, metadata[0].value);
                }else if(metadata.length > 1){
                    JsonArray values = new JsonArray();
                    for(Metadatum md : metadata){
                        values.add(new JsonPrimitive(md.value));
                    }
                    jsonObject.add(googleKey, values);
                }else if (mandatoryFields.contains(googleKey)){
                    jsonObject.addProperty(googleKey, "(:unav)");
                }else{
                    continue;
                }
            }
            convertToValidValues(jsonObject);
        }

        private void convertToValidValues(JsonObject jsonObject) {
            String description = jsonObject.get("description").getAsString();
            if(description.length() < minDescriptionLength){
                int fill = minDescriptionLength - description.length();
                String filler = StringUtils.repeat('.', fill);
                jsonObject.addProperty("description", description + filler);
            }else if(description.length() > maxDescriptionLength){
                jsonObject.addProperty("description", description.substring(0, maxDescriptionLength));
            }

            if(jsonObject.has("creator")){
                JsonElement creator = jsonObject.get("creator");
                JsonArray creators = new JsonArray();
                if(creator.isJsonPrimitive()){
                    creators.add(creatorFromString(creator.getAsString()));
                }else if(creator.isJsonArray()){
                    for(JsonElement creatorEl : creator.getAsJsonArray()){
                        creators.add(creatorFromString(creatorEl.getAsString()));
                    }

                }
                jsonObject.add("creator", creators);
            }
        }

        private JsonElement creatorFromString(String creator) {
            JsonObject el = new JsonObject();
            el.addProperty("@type", "Person");
            el.addProperty("name", creator);
            String[] parts = creator.split(",");
            if(parts.length > 0){
                el.addProperty("familyName", parts[0].trim());
            }
            if(parts.length > 1){
                el.addProperty("givenName", parts[1].trim());
            }
            return el;
        }

        @Override
        public String toString() {
            return String.format("\n<script type=\"application/ld+json\">\n%s\n</script>", jsonObject.toString());
        }
    }
}
