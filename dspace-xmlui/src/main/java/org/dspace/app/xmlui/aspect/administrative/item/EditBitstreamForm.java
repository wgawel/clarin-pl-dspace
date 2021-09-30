/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.item;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.dspace.app.xmlui.aspect.submission.submit.AccessStepUtil;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.*;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.xml.sax.SAXException;

/**
 * 
 * Show a form allowing the user to edit a bitstream's metadata, the description & format.
 * 
 * @author Scott Phillips
 */
public class EditBitstreamForm extends AbstractDSpaceTransformer
{

	/** Language strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");
	private static final Message T_submit_save = message("xmlui.general.save");
	private static final Message T_submit_cancel = message("xmlui.general.cancel");
	private static final Message T_item_trail = message("xmlui.administrative.item.general.item_trail");
	
	private static final Message T_title = message("xmlui.administrative.item.EditBitstreamForm.title");
	private static final Message T_trail = message("xmlui.administrative.item.EditBitstreamForm.trail");
	private static final Message T_head1 = message("xmlui.administrative.item.EditBitstreamForm.head1");
	private static final Message T_file_label = message("xmlui.administrative.item.EditBitstreamForm.file_label");
	private static final Message T_primary_label = message("xmlui.administrative.item.EditBitstreamForm.primary_label");
	private static final Message T_primary_option_yes = message("xmlui.administrative.item.EditBitstreamForm.primary_option_yes");
	private static final Message T_primary_option_no = message("xmlui.administrative.item.EditBitstreamForm.primary_option_no");
	private static final Message T_description_label = message("xmlui.administrative.item.EditBitstreamForm.description_label");
	private static final Message T_description_help = message("xmlui.administrative.item.EditBitstreamForm.description_help");
	private static final Message T_para1 = message("xmlui.administrative.item.EditBitstreamForm.para1");
	private static final Message T_format_label = message("xmlui.administrative.item.EditBitstreamForm.format_label");
	private static final Message T_format_default = message("xmlui.administrative.item.EditBitstreamForm.format_default");
	private static final Message T_para2 = message("xmlui.administrative.item.EditBitstreamForm.para2");
	private static final Message T_user_label = message("xmlui.administrative.item.EditBitstreamForm.user_label");
	private static final Message T_user_help = message("xmlui.administrative.item.EditBitstreamForm.user_help");
    private static final Message T_filename_label = message("xmlui.administrative.item.EditBitstreamForm.name_label");
    private static final Message T_filename_help = message("xmlui.administrative.item.EditBitstreamForm.name_help");

	private static final Message T_name_label = message("xmlui.administrative.item.EditItemMetadataForm.name_label");
	private static final Message T_value_label = message("xmlui.administrative.item.EditItemMetadataForm.value_label");
	private static final Message T_lang_label = message("xmlui.administrative.item.EditItemMetadataForm.lang_label");
	private static final Message T_submit_add = message("xmlui.administrative.item.EditItemMetadataForm.submit_add");

    private boolean isAdvancedFormEnabled=true;

	public void addPageMeta(PageMeta pageMeta) throws WingException
	{
		pageMeta.addMetadata("title").addContent(T_title);

		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrailLink(contextPath + "/admin/item",T_item_trail);
		pageMeta.addTrail().addContent(T_trail);
        pageMeta.addMetadata("javascript", "static").addContent("static/js/editItemUtil.js");
		pageMeta.addMetadata("include-library", "datepicker");
		pageMeta.addMetadata("include-library", "select2");
	}

	public void addBody(Body body) throws SAXException, WingException,
	UIException, SQLException, IOException, AuthorizeException
	{

        isAdvancedFormEnabled= ConfigurationManager.getBooleanProperty("webui.submission.restrictstep.enableAdvancedForm", false);

		// Get our parameters
		int bitstreamID = parameters.getParameterAsInteger("bitstreamID",-1);

		// Get the bitstream and all the various formats
                // Administrator is allowed to see internal formats too.
		Bitstream bitstream = Bitstream.find(context, bitstreamID);
		BitstreamFormat currentFormat = bitstream.getFormat();
                BitstreamFormat[] bitstreamFormats = AuthorizeManager.isAdmin(context) ?
                    BitstreamFormat.findAll(context) :
                    BitstreamFormat.findNonInternal(context);
		
		boolean primaryBitstream = false;
		Bundle[] bundles = bitstream.getBundles();
		if (bundles != null && bundles.length > 0)
		{
			if (bitstreamID == bundles[0].getPrimaryBitstreamID())
			{
				primaryBitstream = true;
			}
		}

		// File name & url
		String fileUrl = contextPath + "/bitstream/id/" +bitstream.getID() + "/" + bitstream.getName();
		String fileName = bitstream.getName();




		// DIVISION: main
		Division div = body.addInteractiveDivision("edit-bitstream", contextPath+"/admin/item", Division.METHOD_MULTIPART, "primary administrative item");    	
		div.setHead(T_head1);

		// LIST: edit form
		List edit = div.addList("edit-bitstream-list", List.TYPE_FORM);
        edit.addLabel(T_file_label);
        edit.addItem().addXref(fileUrl, fileName);

        Text bitstreamName = edit.addItem().addText("bitstreamName");
        bitstreamName.setLabel(T_filename_label);
        bitstreamName.setHelp(T_filename_help);
        bitstreamName.setValue(fileName);
		
		Select primarySelect = edit.addItem().addSelect("primary");
		primarySelect.setLabel(T_primary_label);
		primarySelect.addOption(primaryBitstream,"yes",T_primary_option_yes);
		primarySelect.addOption(!primaryBitstream,"no",T_primary_option_no);
		
		Text description = edit.addItem().addText("description");
		description.setLabel(T_description_label);
		description.setHelp(T_description_help);
		description.setValue(bitstream.getDescription());

        // EMBARGO FIELD
        // if AdvancedAccessPolicy=false: add Embargo Fields.
        if(!isAdvancedFormEnabled){
            AccessStepUtil asu = new AccessStepUtil(context);
            // if the item is embargoed default value will be displayed.
            asu.addEmbargoDateSimpleForm(bitstream, edit, -1);
            asu.addReason(null, edit, -1);
        }

		edit.addItem(T_para1);

		// System supported formats
		Select format = edit.addItem().addSelect("formatID");
		format.setLabel(T_format_label);

                // load the options menu, skipping the "Unknown" format since "Not on list" takes its place
                int unknownFormatID = BitstreamFormat.findUnknown(context).getID();
		format.addOption(-1,T_format_default);
		for (BitstreamFormat bitstreamFormat : bitstreamFormats)
		{
            if (bitstreamFormat.getID() == unknownFormatID)
            {
                continue;
            }
			String supportLevel = "Unknown";
			if (bitstreamFormat.getSupportLevel() == BitstreamFormat.KNOWN)
            {
                supportLevel = "known";
            }
			else if (bitstreamFormat.getSupportLevel() == BitstreamFormat.SUPPORTED)
            {
                supportLevel = "Supported";
            }
			String name = bitstreamFormat.getShortDescription()+" ("+supportLevel+")";
            if (bitstreamFormat.isInternal())
            {
                name += " (Internal)";
            }
			int id = bitstreamFormat.getID();

			format.addOption(id,name);
		}
		if (currentFormat != null)
		{
			format.setOptionSelected(currentFormat.getID());
		}
		else
		{
			format.setOptionSelected(-1);
		}

		edit.addItem(T_para2);

		// User supplied format
		Text userFormat = edit.addItem().addText("user_format");
		userFormat.setLabel(T_user_label);
		userFormat.setHelp(T_user_help);
		userFormat.setValue(bitstream.getUserFormatDescription());

		// LIST: add new metadata
		String previousFieldID = ObjectModelHelper.getRequest(objectModel).getParameter("field");
		List addForm = div.addList("addBitstreamMetadata",List.TYPE_FORM);
		addForm.setHead(T_head1);

		Select addName = addForm.addItem().addSelect("field", "select2");
		addName.setLabel(T_name_label);
		MetadataField[] fields = MetadataField.findAll(context);
		for (MetadataField field : fields)
		{
			int fieldID = field.getFieldID();
			MetadataSchema schema = MetadataSchema.find(context, field.getSchemaID());
			String name = schema.getName() +"."+field.getElement();
			if (field.getQualifier() != null)
			{
				name += "." + field.getQualifier();
			}
            addName.addOption(fieldID, name);
		}
		if (previousFieldID != null)
		{
			addName.setOptionSelected(previousFieldID);
		}


		Composite addComposite = addForm.addItem().addComposite("value");
		addComposite.setLabel(T_value_label);
		TextArea addValue = addComposite.addTextArea("value");
		Text addLang = addComposite.addText("language");

		addValue.setSize(4, 35);
		addLang.setLabel(T_lang_label);
		addLang.setSize(6);

		addForm.addItem().addButton("submit_add").setValue(T_submit_add);

		// Local fields
		List localMetadata = edit.addList("localMetadata");
		//localMetadata.setLabel("Local metadata");
		localMetadata.setHead("Local Metadata");
		for (Metadatum m : bitstream.getMetadata("local", "bitstream", Item.ANY, Item.ANY)) {
			String title = "local_bitstream_" + ((null != m.qualifier) ? m.qualifier : "");
			Text t = localMetadata.addItem().addText(title);
			t.setValue(m.value);
			t.setSize(50);
			t.setHelp(title);
		}
		localMetadata.addLabel("List of additional metadata in local.bitstream.*");


		// ITEM: form actions
		org.dspace.app.xmlui.wing.element.Item actions = edit.addItem();
		actions.addButton("submit_save").setValue(T_submit_save);
		actions.addButton("submit_cancel").setValue(T_submit_cancel);
		actions.addButton("submit_clear_local_metadata").setValue("Clear Local Metadata");

		div.addHidden("administrative-continue").setValue(knot.getId()); 

	}
}

