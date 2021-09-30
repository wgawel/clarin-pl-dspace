/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Arrays;
import java.util.Enumeration;

import org.apache.cocoon.environment.Request;
import org.apache.cocoon.servlet.multipart.Part;
import org.apache.log4j.Logger;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.dspace.app.util.Util;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.Metadatum;
import org.dspace.content.DSpaceObject;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.authority.Choices;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.curate.Curator;
import org.dspace.embargo.EmbargoManager;
import org.dspace.handle.HandleManager;

import javax.servlet.http.HttpServletRequest;
import cz.cuni.mff.ufal.administrative.EditItemMetadataForm;

/**
 * Utility methods to processes actions on Groups. These methods are used
 * exclusively from the administrative flow scripts.
 * 
 * based on class by Jay Paz and Scott Phillips
 * modified for LINDAT/CLARIN
 */
public class FlowItemUtils 
{

	/** Language Strings */
	private static final Message T_metadata_updated = new Message("default","The Item's metadata was successfully updated.");
	private static final Message T_metadata_added = new Message("default","New metadata was added.");
	private static final Message T_item_withdrawn = new Message("default","The item has been withdrawn.");
    private static final Message T_item_public = new Message("default","The item is now public.");
    private static final Message T_item_private = new Message("default","The item is now private.");
	private static final Message T_item_reinstated = new Message("default","The item has been reinstated.");
	private static final Message T_item_moved = new Message("default","The item has been moved.");
	private static final Message T_item_move_destination_not_found = new Message("default","The selected destination collection could not be found.");
	private static final Message T_bitstream_added = new Message("default","The new bitstream was successfully uploaded.");
	private static final Message T_bitstream_failed = new Message("default","Error while uploading file.");
	private static final Message T_bitstream_updated = new Message("default","The bitstream has been updated.");
	private static final Message T_bitstream_delete = new Message("default","The selected bitstreams have been deleted.");
	private static final Message T_bitstream_order = new Message("default","The bitstream order has been successfully altered.");
	private static final Message T_bitstream_not_found = new Message("default","The bitstream was not found.");
    private static final Message T_emgargo_set = new Message("default","The embargo was set.");

    private static Logger log = cz.cuni.mff.ufal.Logger.getLogger(FlowItemUtils.class);
	
	/**
	 * Resolve the given identifier to an item. The identifier may be either an
	 * internal ID or a handle. If an item is found then the result the internal
	 * ID of the item will be placed in the result "itemID" parameter.
	 * 
	 * If the identifier was unable to be resolved to an item then the "identifier"
	 * field is placed in error.
	 * 
	 * @param context The current DSpace context.
	 * @param identifier An Internal ID or a handle
	 * @return A flow result
	 */
	public static FlowResult resolveItemIdentifier(Context context, String identifier) throws SQLException
	{
		FlowResult result = new FlowResult();
		result.setContinue(false);
		
		//		Check whether it's a handle or internal id (by check ing if it has a slash inthe string)
		if (identifier.contains("/")) 
		{
			DSpaceObject dso = HandleManager.resolveToObject(context, identifier);
	
			if (dso != null && dso.getType() == Constants.ITEM) 
			{ 
				result.setParameter("itemID", dso.getID());
				result.setParameter("type", Constants.ITEM);
				result.setContinue(true);
				return result;
			}
		}
		else
		{
		
			Item item = null;
			try {
				item = Item.find(context, Integer.valueOf(identifier));
			} catch (NumberFormatException e) {
				// ignoring the exception
			}

			if (item != null) 
			{
				result.setParameter("itemID", item.getID());
				result.setParameter("type", Constants.ITEM);
				result.setContinue(true);
				return result;
			}
		}

		result.addError("identifier");
		return result;
	}
	
	/**
	 * Process the request parameters to update the item's metadata and remove any selected bitstreams.
	 * 
	 * Each metadata entry will have three fields "name_X", "value_X", and "language_X" where X is an
	 * integer that relates all three of the fields together. The name parameter stores the metadata name 
	 * that is used by the entry (i.e schema_element_qualifier). The value and language paramaters are user
	 * inputed fields. If the optional parameter "remove_X" is given then the metadata value is removed.
	 * 
	 * To support AJAX operations on this page an aditional parameter is considered, the "scope". The scope
	 * is the set of metadata entries that are being updated during this request. It the metadata name, 
	 * schema_element_qualifier, only fields that have this name are considered! If all fields are to be
	 * considered then scope should be set to "*". 
	 * 
	 * When creating an AJAX query include all the name_X, value_X, language_X, and remove_X for the fields
	 * in the set, and then set the scope parameter to be the metadata field.
	 * 
	 * @param context The current DSpace context
	 * @param itemID  internal item id
	 * @param request the Cocoon request
	 * @return A flow result
	 */
	public static FlowResult processEditItem(Context context, int itemID, Request request) throws SQLException, AuthorizeException, UIException, IOException
	{
		FlowResult result = new FlowResult();
		result.setContinue(false);

		Item item = Item.find(context, itemID);
		
		
		// STEP 1:
		// Clear all metadata within the scope
		// Only metadata values within this scope will be considered. This
		// is so ajax request can operate on only a subset of the values.
		String scope = request.getParameter("scope");
		if ("*".equals(scope))
		{
			item.clearMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
		}
		else
		{
			String[] parts = parseName(scope);
			item.clearMetadata(parts[0],parts[1],parts[2],Item.ANY);
		}
		
		// STEP 2:
		// First determine all the metadata fields that are within
		// the scope parameter
		ArrayList<Integer> indexes = new ArrayList<Integer>();
		Enumeration parameters = request.getParameterNames();
		while(parameters.hasMoreElements())
		{

			// Only consider the name_ fields
			String parameterName = (String) parameters.nextElement();
			if (parameterName.startsWith("name_"))
			{
				// Check if the name is within the scope
				String parameterValue = request.getParameter(parameterName);
				if ("*".equals(scope) || scope.equals(parameterValue))
				{
					// Extract the index from the name.
					String indexString = parameterName.substring("name_".length());
					Integer index = Integer.valueOf(indexString);
					indexes.add(index);
				}
			}
		}
		
		
		// STEP 3:
		// Iterate over all the indexes within the scope and add them back in.
		for (Integer index=1; index <= indexes.size(); ++index)
		{
			String name = request.getParameter("name_"+index);
			String value = request.getParameter("value_"+index);
            String authority = request.getParameter("value_"+index+"_authority");
            String confidence = request.getParameter("value_"+index+"_confidence");
			String lang = request.getParameter("language_"+index);
			String remove = request.getParameter("remove_"+index);
			
			// the user selected the remove checkbox.
			if (remove != null)
                        {
				continue;
                        }
			
			// get the field's name broken up
			String[] parts = parseName(name);
			
                        // probe for a confidence value
                        int iconf = Choices.CF_UNSET;
                        if (confidence != null && confidence.length() > 0)
                        {
                            iconf = Choices.getConfidenceValue(confidence);
                        }
                        // upgrade to a minimum of NOVALUE if there IS an authority key
                        if (authority != null && authority.length() > 0 && iconf == Choices.CF_UNSET)
                        {
                            iconf = Choices.CF_NOVALUE;
                        }
                        item.addMetadata(parts[0], parts[1], parts[2], lang,
                                             value, authority, iconf);
		}
		
		item.store_provenance_info("Item was updated in processEditItem:", context.getCurrentUser());
		item.update();
		context.commit();
		
		result.setContinue(true);
		
		result.setOutcome(true);
		result.setMessage(T_metadata_updated);
		
		return result;
	}
	
	public static FlowResult processEditItemMetadata(Context context, int itemID, Request request) throws SQLException, AuthorizeException, UIException, IOException
	{
		FlowResult result = new FlowResult();
		result.setContinue(false);

		Item item = Item.find(context, itemID);
		Metadatum[] metadata = EditItemMetadataForm.filterValues(item.getMetadata(Item.ANY,Item.ANY,Item.ANY,Item.ANY));
        Arrays.sort(metadata, new EditItemMetadataForm.MetadatumComparator());
		
		
		String scope = request.getParameter("scope");
		// STEP 2:
		// First determine all the metadata fields that are within
		// the scope parameter
		ArrayList<Integer> indexes = new ArrayList<Integer>();
		Enumeration parameters = request.getParameterNames();
		while(parameters.hasMoreElements())
		{

			// Only consider the name_ fields
			String parameterName = (String) parameters.nextElement();
			if (parameterName.startsWith("name_"))
			{
				// Check if the name is within the scope
				String parameterValue = request.getParameter(parameterName);
				if ("*".equals(scope) || scope.equals(parameterValue))
				{
					// Extract the index from the name.
					String indexString = parameterName.substring("name_".length());
					Integer index = Integer.valueOf(indexString);
					indexes.add(index);
				}
			}
		}
		
		
		StringBuilder provenanceLog = new StringBuilder();
		
		//clear only the fields that will be updated
		for(Integer index=1; index <= indexes.size(); ++index){
			String name = request.getParameter("name_"+index);
			String[] parts = parseName(name);
			item.clearMetadata(parts[0], parts[1], parts[2], Item.ANY);
		}
		
		// STEP 3:
		// Iterate over all the indexes within the scope and add them back in.
		for (Integer index=1; index <= indexes.size(); ++index)
		{
			String name = request.getParameter("name_"+index);
			String value = request.getParameter("value_"+index);
                        String authority = request.getParameter("value_"+index+"_authority");
                        String confidence = request.getParameter("value_"+index+"_confidence");
			String lang = request.getParameter("language_"+index);
			String remove = request.getParameter("remove_"+index);
			
			// the user selected the remove checkbox.
			if (remove != null)
                        {
				provenanceLog.append(String.format("Removed field '%s' with value '%s' and lang '%s'.\n", name, value, lang));
				continue;
                        }
			
			// get the field's name broken up
			String[] parts = parseName(name);
			
                        // probe for a confidence value
                        int iconf = Choices.CF_UNSET;
                        if (confidence != null && confidence.length() > 0)
                        {
                            iconf = Choices.getConfidenceValue(confidence);
                        }
                        // upgrade to a minimum of NOVALUE if there IS an authority key
                        if (authority != null && authority.length() > 0 && iconf == Choices.CF_UNSET)
                        {
                            iconf = Choices.CF_NOVALUE;
                        }
                        item.addMetadata(parts[0], parts[1], parts[2], lang,
                                             value, authority, iconf);
                        Metadatum dcv = metadata[index-1];
                        	//if language or value was changed
                        	if(dcv.schema.equals(parts[0]) && 
                        	    dcv.element.equals(parts[1]) &&
                        	    org.apache.commons.lang.ObjectUtils.equals(parts[2], dcv.qualifier)){
                        	    if(!(org.apache.commons.lang.ObjectUtils.equals(dcv.language,lang) && org.apache.commons.lang.ObjectUtils.equals(dcv.value,value))){
                                    provenanceLog.append(String.format("Field '%s.%s.%s' was changed: '%s' -> '%s'; '%s' -> '%s'\n", parts[0], parts[1],  parts[2] == null ? "" : parts[2], dcv.language, lang, dcv.value, value));
                        	    }
                        	}
		}
		
		item.store_provenance_info("Item was updated in processEditItemMetadata:\n" + provenanceLog.toString(), context.getCurrentUser());
		item.update();
		context.commit();
		
		result.setContinue(true);
		
		result.setOutcome(true);
		result.setMessage(T_metadata_updated);
		
		return result;
	}
	
	/**
	 * Process the request paramaters to add a new metadata entry for the item.
	 * 
	 * @param context The current DSpace context
	 * @param itemID  internal item id
	 * @param request the Cocoon request
	 * @return A flow result
	 */
	public static FlowResult processAddMetadata(Context context, int itemID, Request request) throws SQLException, AuthorizeException, UIException, IOException
	{
		FlowResult result = new FlowResult();
		result.setContinue(false);

		Item item = Item.find(context, itemID);
		
		
		String fieldID = request.getParameter("field");
		String value = request.getParameter("value");
		String language = request.getParameter("language");
		
		MetadataField field = MetadataField.find(context,Integer.valueOf(fieldID));
		MetadataSchema schema = MetadataSchema.find(context,field.getSchemaID());
		
		item.addMetadata(schema.getName(), field.getElement(), field.getQualifier(), language, value);
		
		item.store_provenance_info(String.format("Item metadata(%s,%s,%s) were added in processAddMetadata",schema.getName() + "." + field.getElement() +  (field.getQualifier() != null ? "." +field.getQualifier() :"") ,language, value), context.getCurrentUser());
		item.update();
		context.commit();
		
		result.setContinue(true);
		
		result.setOutcome(true);
		result.setMessage(T_metadata_added);
		
		return result;
	}


	/**
	 * Withdraw the specified item, this method assumes that the action has been confirmed.
	 * 
	 * @param context The DSpace context
	 * @param itemID The id of the to-be-withdrawn item.
	 * @return A result object
	 */
	public static FlowResult processWithdrawItem(Context context, int itemID, Request request) throws SQLException, AuthorizeException, IOException
	{
		FlowResult result = new FlowResult();
		result.setContinue(false);
		
		Item item = Item.find(context, itemID);
		String handle = request.getParameter("extra_handle");
		String reason = request.getParameter("extra_reason");
		if(!StringUtils.isEmpty(handle)){
			item.setReplacedBy(handle);
		}
		if(!StringUtils.isEmpty(reason)){
			item.store_provenance_info(String.format("The item was withdrawn with the following reason: \"%s\"", reason), context.getCurrentUser());
			Metadatum md = new Metadatum();
			md.schema = "local";
			md.element = "withdrawn";
			md.qualifier = "reason";
			md.value = reason;
			item.addMetadatum(md);
		}
		item.withdraw();
		context.commit();

		result.setContinue(true);
        result.setOutcome(true);
        result.setMessage(T_item_withdrawn);
        
		return result;
	}
	
	
	/**
	 * Reinstate the specified item, this method assumes that the action has been confirmed.
	 * 
	 * @param context The DSpace context
	 * @param itemID The id of the to-be-reinstated item.
	 * @return A result object
	 */
	public static FlowResult processReinstateItem(Context context, int itemID) throws SQLException, AuthorizeException, IOException
	{
		FlowResult result = new FlowResult();
		result.setContinue(false);
		
		Item item = Item.find(context, itemID);
		item.reinstate();
		context.commit();

		result.setContinue(true);
        result.setOutcome(true);
        result.setMessage(T_item_reinstated);
        
		return result;
	}


    /**
     * Make the specified item Private, this method assumes that the action has been confirmed.
     *
     * @param context The DSpace context
     * @param itemID The id of the to-be-withdrawn item.
     * @return A result object
     */
    public static FlowResult processPrivateItem(Context context, int itemID) throws SQLException, AuthorizeException, IOException
    {
        FlowResult result = new FlowResult();
        result.setContinue(false);

        Item item = Item.find(context, itemID);
        item.setDiscoverable(false);
        item.update();
        context.commit();

        result.setContinue(true);
        result.setOutcome(true);
        result.setMessage(T_item_private);

        return result;
    }

    /**
     * Make the specified item Private, this method assumes that the action has been confirmed.
     *
     * @param context The DSpace context
     * @param itemID The id of the to-be-withdrawn item.
     * @return A result object
     */
    public static FlowResult processPublicItem(Context context, int itemID) throws SQLException, AuthorizeException, IOException
    {
        FlowResult result = new FlowResult();
        result.setContinue(false);

        Item item = Item.find(context, itemID);
        item.setDiscoverable(true);
        item.update();
        context.commit();

        result.setContinue(true);
        result.setOutcome(true);
        result.setMessage(T_item_public);

        return result;
    }


    /**
     * Move the specified item to another collection.
     *
     * @param context The DSpace context
     * @param itemID The id of the to-be-moved item.
     * @param collectionID The id of the destination collection.
     * @param inherit Whether to inherit the policies of the destination collection
     * @return A result object
     */
    public static FlowResult processMoveItem(Context context, int itemID, int collectionID, boolean inherit) throws SQLException, AuthorizeException, IOException
    {
        FlowResult result = new FlowResult();
        result.setContinue(false);

        Item item = Item.find(context, itemID);

        if(AuthorizeManager.isAdmin(context, item))
        {
          //Add an action giving this user *explicit* admin permissions on the item itself.
          //This ensures that the user will be able to call item.update() even if he/she
          // moves it to a Collection that he/she doesn't administer.
          if (item.canEdit())
          {
              AuthorizeManager.authorizeAction(context, item, Constants.WRITE);
          }

          Collection destination = Collection.find(context, collectionID);
          if (destination == null)
          {
              result.setOutcome(false);
              result.setContinue(false);
              result.setMessage(T_item_move_destination_not_found);
              return result;
          }

          Collection owningCollection = item.getOwningCollection();
          if (destination.equals(owningCollection))
          {
              // nothing to do
              result.setOutcome(false);
              result.setContinue(false);
              return result;
          }

          // note: an item.move() method exists, but does not handle several cases:
          // - no preexisting owning collection (first arg is null)
          // - item already in collection, but not an owning collection
          //   (works, but puts item in collection twice)

          // Don't re-add the item to a collection it's already in.
          boolean alreadyInCollection = false;
          for (Collection collection : item.getCollections())
          {
              if (collection.equals(destination))
              {
                  alreadyInCollection = true;
                  break;
              }
          }

          // Remove item from its owning collection and add to the destination
          if (!alreadyInCollection)
          {
              destination.addItem(item);
          }

          if (owningCollection != null)
          {
              owningCollection.removeItem(item);
          }

          item.setOwningCollection(destination);

          // Inherit policies of destination collection if required
          if (inherit)
          {
              item.inheritCollectionDefaultPolicies(destination);
          }

          item.update();
          context.commit();

          result.setOutcome(true);
          result.setContinue(true);
          result.setMessage(T_item_moved);
        }

        return result;
    }


	/**
	 * Permanently delete the specified item, this method assumes that
	 * the action has been confirmed.
	 * 
	 * @param context The DSpace context
	 * @param itemID The id of the to-be-deleted item.
	 * @return A result object
	 */
	public static FlowResult processDeleteItem(Context context, int itemID) throws SQLException, AuthorizeException, IOException
	{
		FlowResult result = new FlowResult();
		result.setContinue(false);
		
		Item item = Item.find(context, itemID);
			
        Collection[] collections = item.getCollections();

        // Remove item from all the collections it's in
        for (Collection collection : collections)
        {
            collection.removeItem(item);
        }	
        
        // Note: when removing an item from the last collection it will
        // be removed from the system. So there is no need to also call
        // an item.delete() method.        
        
        // orphan item
        if ( collections.length == 0 ) {
        	item.delete();
        	log.error(String.format(
        			"A request to delete an item without collection assinged [%s] [%s] - forcing delete!",
        			item.getName(), item.getHandle()));
        }
        
        context.commit();
		
        result.setContinue(true);
        
		return result;
	}
	
	
	/**
	 * Add a new bitstream to the item. The bundle, bitstream (aka file), and description 
	 * will be used to create a new bitstream. If the format needs to be adjusted then they 
	 * will need to access the edit bitstream form after it has been uploaded.
	 * 
	 * @param context The DSpace content
	 * @param itemID The item to add a new bitstream too
	 * @param request The request.
	 * @return A flow result
	 */
	public static FlowResult processAddBitstream(Context context, int itemID, Request request) throws SQLException, AuthorizeException, IOException 
	{
         log.info("processAddBitstream:\n\titemID = '"+itemID+"'\n\tfile_local = '"+request.getParameter("file_local")+"'\n\tbundle = '"+request.getParameter("bundle")+"'");
		FlowResult result = new FlowResult();
		result.setContinue(false);
		
		// Upload a new file
		Item item = Item.find(context, itemID);
		
		
		Object object = request.get("file");
		Part filePart = null;
		if (object instanceof Part)
                {
			filePart = (Part) object;
                }

		InputStream is = null;
		String upload_name = null;
		if ( filePart != null && filePart.getSize() > 0 ) {
			is = filePart.getInputStream();
			// Strip all but the last filename. It would be nice
			// to know which OS the file came from.
			upload_name = filePart.getUploadName();
		}else {
			try {
				String filePath = request.getParameter("file_local");
	        	if(filePath.startsWith("/")) {
	        		filePath = "file://" + filePath;
	         	}
				if ( null != filePath && 0 < filePath.length() ) {
					URL url = new URI(filePath).toURL();
					is = url.openStream();
				}
	        	upload_name = filePath;
			} catch (URISyntaxException | FileNotFoundException e) {
				log.warn("Error while adding file. Check the path/url.", e);
			}
		}
		
		if ( is != null ) {

			String bundleName = request.getParameter("bundle");
			
			Bitstream bitstream;
			Bundle[] bundles = item.getBundles(bundleName);
			if (bundles.length < 1)
			{
				// set bundle's name to ORIGINAL
				bitstream = item.createSingleBitstream(is, bundleName);
			}
			else
			{
				// we have a bundle already, just add bitstream
				bitstream = bundles[0].createBitstream(is);
			}

			String upload_name_stripped = upload_name;
			while (upload_name_stripped.indexOf('/') > -1)
			{
				upload_name_stripped = upload_name_stripped.substring(upload_name_stripped.indexOf('/') + 1);
			}

			while (upload_name_stripped.indexOf('\\') > -1)
			{
				upload_name_stripped = upload_name_stripped.substring(upload_name_stripped.indexOf('\\') + 1);
			}

			bitstream.setName(upload_name_stripped);
			bitstream.setSource(upload_name);
			bitstream.setDescription(request.getParameter("description"));

			// Identify the format
			BitstreamFormat format = FormatIdentifier.guessFormat(context, bitstream);
			bitstream.setFormat(format);

			// Update to DB
			bitstream.update();
	        item.store_provenance_info("Item was added a bitstream", context.getCurrentUser());
			item.update();

			processAccessFields(context, request, item.getOwningCollection(), bitstream);

			context.commit();
			
			result.setContinue(true);
	        result.setOutcome(true);
	        result.setMessage(T_bitstream_added); 
		}
		else
		{
			result.setContinue(false);
	        result.setOutcome(false);
	        result.setMessage(T_bitstream_failed); 
		}
		return result;
	}

    private static void processAccessFields(Context context, HttpServletRequest request, Collection collection, Bitstream b) throws SQLException, AuthorizeException {

		if ( null == collection && AuthorizeManager.isAdmin(context) ) {
			// if an object does not have handle and owning collection is null => in workflow
			// if a user is admin, allow the action, see #123
			return;
		}


			// if it is a simple form we should create the policy for Anonymous
        // if Anonymous does not have right on this collection, create policies for any other groups with
        // DEFAULT_ITEM_READ specified.
        Date startDate = null;
        try {
            startDate = DateUtils.parseDate(request.getParameter("embargo_until_date"), new String[]{"yyyy-MM-dd", "yyyy-MM", "yyyy"});
        } catch (Exception e) {
            //Ignore, start date is already null
        }
        String reason = request.getParameter("reason");
        AuthorizeManager.generateAutomaticPolicies(context, startDate, reason, b, collection);
    }
	
	
	/**
	 * Update a bitstream's metadata.
	 * 
	 * @param context The DSpace content
	 * @param itemID The item to which the bitstream belongs
	 * @param bitstreamID The bitstream being updated.
	 * @param description The new description of the bitstream
	 * @param formatID The new format ID of the bitstream
	 * @param userFormat Any user supplied formats.
	 * @return A flow result object.
	 */
	public static FlowResult processEditBitstream(Context context, int itemID, int bitstreamID, String bitstreamName,
                                                  String primary, String description, int formatID, String userFormat, Request request) throws SQLException, AuthorizeException
	{
		FlowResult result = new FlowResult();
		result.setContinue(false);
		
		Bitstream bitstream = Bitstream.find(context, bitstreamID);
		BitstreamFormat currentFormat = bitstream.getFormat();

		//Step 1:
		// Update the bitstream's description and name
		if (description != null)
		{
			bitstream.setDescription(description);
		}

        if (bitstreamName != null)
        {
            bitstream.setName(bitstreamName);
        }
		
		//Step 2:
		// Check if the primary bitstream status has changed
		Bundle[] bundles = bitstream.getBundles();
		if (bundles != null && bundles.length > 0)
		{
			if (bitstreamID == bundles[0].getPrimaryBitstreamID())
			{
				// currently the bitstream is primary
				if ("no".equals(primary))
				{
					// However the user has removed this bitstream as a primary bitstream.
					bundles[0].unsetPrimaryBitstreamID();
					bundles[0].update();
				}
			}
			else
			{
				// currently the bitstream is non-primary
				if ("yes".equals(primary))
				{
					// However the user has set this bitstream as primary.
					bundles[0].setPrimaryBitstreamID(bitstreamID);
					bundles[0].update();
				}
			}
		}
		
		
		//Step 2:
		// Update the bitstream's format
		if (formatID > 0)
		{
			if (currentFormat == null || currentFormat.getID() != formatID)
			{
				BitstreamFormat newFormat = BitstreamFormat.find(context, formatID);
				if (newFormat != null)
				{
					bitstream.setFormat(newFormat);
				}
			}
		}
		else
		{
			if (userFormat != null && userFormat.length() > 0)
			{
				bitstream.setUserFormatDescription(userFormat);
			}
		}
		
		//Step 3:
		// Save our changes
		bitstream.update();
		context.commit();

        processAccessFields(context, request, ((Item)bitstream.getParentObject()).getOwningCollection(), bitstream);


        result.setContinue(true);
	     result.setOutcome(true);
	     result.setMessage(T_bitstream_updated);
	        
		
		return result;
	}

	public static FlowResult processDeleteBitstreamLocalMetadata(
		Context context, int itemID, int bitstreamID, Request request) throws SQLException, AuthorizeException
	{
		FlowResult result = new FlowResult();
		result.setContinue(false);

		Bitstream bitstream = Bitstream.find(context, bitstreamID);
		if ( null != bitstream ) {
			bitstream.clearMetadata("local", Item.ANY, Item.ANY, Item.ANY);
			bitstream.update();
			// Save our changes
			context.commit();
			result.setOutcome(true);
			result.setMessage(T_bitstream_updated);
		}else {
			result.setOutcome(false);
			result.setMessage(T_bitstream_not_found);
		}

		result.setContinue(true);

		return result;
	}

	public static FlowResult addBitstreamMetadata(Context context, int bitstreamID, Request request) throws SQLException, AuthorizeException {
		FlowResult result = new FlowResult();
		result.setContinue(false);

		Bitstream bitstream = Bitstream.find(context, bitstreamID);

		String fieldID = request.getParameter("field");
		String value = request.getParameter("value");
		String language = request.getParameter("language");

		MetadataField field = MetadataField.find(context,Integer.valueOf(fieldID));
		MetadataSchema schema = MetadataSchema.find(context,field.getSchemaID());


		if ( null != bitstream ) {
			bitstream.addMetadata(schema.getName(), field.getElement(), field.getQualifier(), language, value);
			bitstream.update();
			// Save our changes
			context.commit();
			result.setOutcome(true);
			result.setMessage(T_bitstream_updated);
		}else {
			result.setOutcome(false);
			result.setMessage(T_bitstream_not_found);
		}

		result.setContinue(true);

		return result;

	}
	/**
	 * Delete the given bitstreams from the bundle and item. If there are no more bitstreams 
	 * left in a bundle then also remove it.
	 * 
	 * @param context Current dspace content
	 * @param itemID The item id from which to remove bitstreams
	 * @param bitstreamIDs A bundleID slash bitstreamID pair of bitstreams to be removed.
	 * @return A flow result
	 */
	public static FlowResult processDeleteBitstreams(Context context, int itemID, String[] bitstreamIDs) throws SQLException, AuthorizeException, IOException, UIException
	{
		FlowResult result = new FlowResult();
		result.setContinue(false);
		
		Item item = Item.find(context, itemID);
		
		for (String id : bitstreamIDs)
		{
			String[] parts = id.split("/");
			
			if (parts.length != 2)
                        {
				throw new UIException("Unable to parse id into bundle and bitstream id: "+id);
                        }
			
			int bundleID = Integer.valueOf(parts[0]);
			int bitstreamID = Integer.valueOf(parts[1]);
			
			Bundle bundle = Bundle.find(context, bundleID);
			Bitstream bitstream = Bitstream.find(context,bitstreamID);
			
			bundle.removeBitstream(bitstream);
			
			if (bundle.getBitstreams().length == 0)
			{
				item.removeBundle(bundle);
			}
		}
		
		item.store_provenance_info("Item was deleted a bitstream", context.getCurrentUser());
		item.update();
		
		context.commit();
		
		result.setContinue(true);
		result.setOutcome(true);
		result.setMessage(T_bitstream_delete);
		
		return result;
	}

    
    /**
     * Set or update embargo.
     * 
     * @param context Current dspace content
     * @param itemID The item id to set emargo to
     * @return A flow result
     */
    public static FlowResult processEmbargoItem(Context context, int itemID, Request request) 
        throws SQLException, AuthorizeException, IOException, UIException
    {
        FlowResult result = new FlowResult();
        result.setContinue(false);

        Item item = Item.find(context, itemID);
        String date_value = request.getParameter("embargo_date");
        
        //
        DCDate lift_date = new DCDate(date_value);
        if ( lift_date.toDate() != null ) {
        	EmbargoManager.setEmbargo(context, item, lift_date);
            result.setMessage(T_emgargo_set);
            result.setOutcome(true);
        }else {
        	result.setMessage(new Message("default", "The embargo could not be set, invalid date supplied!"));
            result.setOutcome(false);
        }
        
        //
        context.commit();


        return result;
    }

	public static FlowResult processEmbargoDelete(Context context, int itemID, Request request)
			throws SQLException, IOException, AuthorizeException {
		FlowResult result = new FlowResult();
		result.setContinue(false);
		Item item = Item.find(context, itemID);
		//set the embargo to now() + 1 min. This will be in future and adds resonable start date on the policies
		EmbargoManager.setEmbargo(context, item, new DCDate(new Date(new Date().getTime() + 60000)));
		EmbargoManager.liftEmbargo(context, item);

		return result;

	}
    

    public static FlowResult processReorderBitstream(Context context, int itemID, Request request) throws SQLException, AuthorizeException {
        String submitButton = Util.getSubmitButton(request, "submit_update_order");
        FlowResult result = new FlowResult();
        result.setContinue(false);

        Item item = Item.find(context, itemID);

        Bundle[] bundles = item.getBundles();
        for (Bundle bundle : bundles) {
            Bitstream[] bitstreams = bundle.getBitstreams();

            int[] newBitstreamOrder = new int[bitstreams.length];
            if (submitButton.equals("submit_update_order")) {
                for (Bitstream bitstream : bitstreams) {
                    //The order is determined by javascript
                    //For each of our bitstream retrieve the order value
                    int order = Util.getIntParameter(request, "order_" + bitstream.getID());
                    //-1 the order since the order needed to start from one
                    order--;
                    //Place the bitstream identifier in the correct order
                    newBitstreamOrder[order] = bitstream.getID();
                }
            }else{
                //Javascript isn't operational retrieve the value from the hidden field
                //Retrieve the button key
                String inputKey = submitButton.replace("submit_order_", "") + "_value";
                if(inputKey.startsWith(bundle.getID() + "_")){
                    String[] vals = request.getParameter(inputKey).split(",");
                    for (int i = 0; i < vals.length; i++) {
                        String val = vals[i];
                        newBitstreamOrder[i] = Integer.parseInt(val);
                    }
                }else{
                    newBitstreamOrder = null;
                }
            }
            if(newBitstreamOrder != null){
                //Set the new order in our bundle !
                bundle.setOrder(newBitstreamOrder);
                bundle.update();
            }
        }

        context.commit();

        result.setContinue(true);
        result.setOutcome(true);
        result.setMessage(T_bitstream_order);

        return result;
    }

        /**
         * processCurateDSO
         *
         * Utility method to process curation tasks
         * submitted via the DSpace GUI
         *
         * @param context
         * @param itemID
         * @param request
         *
         */
        public static FlowResult processCurateItem(Context context, int itemID, Request request)
                                                                throws AuthorizeException, IOException, SQLException, Exception
	{
                String task = request.getParameter("curate_task");
		Curator curator = FlowCurationUtils.getCurator(task);
                try
                {
                    Item item = Item.find(context, itemID);
                    if (item != null)
                    {
                        //Call curate(context,ID) to ensure a Task Performer (Eperson) is set in Curator
                        curator.curate(context, item.getHandle());
                    }
                    return FlowCurationUtils.getRunFlowResult(task, curator, true);
                }
                catch (Exception e) 
                {
                    curator.setResult(task, e.getMessage());
                    return FlowCurationUtils.getRunFlowResult(task, curator, false);
		}
	}

      /**
       * queues curation tasks
       */
        public static FlowResult processQueueItem(Context context, int itemID, Request request)
                                                                throws AuthorizeException, IOException, SQLException, Exception
	{
                String task = request.getParameter("curate_task");
                Curator curator = FlowCurationUtils.getCurator(task);
                String objId = String.valueOf(itemID);
                String taskQueueName = ConfigurationManager.getProperty("curate", "ui.queuename");
                boolean status = false;
                Item item = Item.find(context, itemID);
                if (item != null)
                {
                    objId = item.getHandle();
                    try
                    {
                        curator.queue(context, objId, taskQueueName);
                        status = true;
                    }
                    catch (IOException ioe)
                    {
                        // no-op (the Curator should have already logged any error that occurred)
                    }
                }
                return FlowCurationUtils.getQueueFlowResult(task, status, objId, taskQueueName);
	}

	
	/**
	 * Parse the given name into three parts, divided by an _. Each part should represent the 
	 * schema, element, and qualifier. You are guaranteed that if no qualifier was supplied the 
	 * third entry is null.
	 * 
	 * @param name The name to be parsed.
	 * @return An array of name parts.
	 */
	private static String[] parseName(String name) throws UIException
	{
		String[] parts = new String[3];
		
		String[] split = name.split("_");
		if (split.length == 2) {
			parts[0] = split[0];
			parts[1] = split[1];
			parts[2] = null;
		} else if (split.length == 3) {
			parts[0] = split[0];
			parts[1] = split[1];
			parts[2] = split[2];
		} else {
			throw new UIException("Unable to parse metedata field name: "+name);
		}
		return parts;
	}
}

