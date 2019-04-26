/* Created for LINDAT/CLARIN */
package pl.edu.administrative;

import cz.cuni.mff.ufal.DSpaceApi;
import cz.cuni.mff.ufal.ExtraLicenseField;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseDefinition;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseLabel;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseResourceMapping;
import cz.cuni.mff.ufal.lindat.utilities.interfaces.IFunctionalities;
import org.apache.cocoon.environment.wrapper.RequestWrapper;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.administrative.FlowResult;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;


/**
 * Display information about the item and allow the user to change 
 * 
 * @author Amir Kamran
 */


public class CmdiProfileForm extends AbstractDSpaceTransformer {

	Logger log = Logger.getLogger(CmdiProfileForm.class);

	/** Language strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");
	protected static final Message T_head_define = message("xmlui.Submission.submit.UFALLicenseStep.define.head");
    protected static final Message T_license_name = message("xmlui.Submission.submit.UFALLicenseStep.define.name");
    protected static final Message T_license_label = message("xmlui.administrative.item.DefineLicenseForm.license_labels");
    protected static final Message T_license_required = message("xmlui.administrative.item.DefineLicenseForm.license_required");
	protected static final Message T_license_url = message("xmlui.Submission.submit.UFALLicenseStep.define.url");
	protected static final Message T_license_confirmation = message("xmlui.Submission.submit.UFALLicenseStep.define.confirmation");

	private static final Message T_save = message("xmlui.general.save");
	private static final Message T_return = message("xmlui.general.return");

	private static final Message T_title = message("xmlui.administrative.LicenseForm.title");
	private static final Message T_trail = message("xmlui.administrative.LicenseForm.trail");
	private static final Message T_head = message("xmlui.administrative.LicenseForm.head1");

	public void addPageMeta(PageMeta pageMeta) throws WingException
	{
		pageMeta.addMetadata("title").addContent(T_title);

		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrail().addContent(T_trail);
	}

	public void addBody(Body body) throws SQLException, WingException
	{
		// Get our parameters and state
		String option = parameters.getParameter("option", "license_list");
		boolean delete = parameters.getParameterAsBoolean("delete", false);

    	String licenseIDs = parameters.getParameter("licenseIDs", "");

    	ArrayList<Integer> licenseIDs_list = new ArrayList<Integer>();
    	for(String id : licenseIDs.split("\\D+")) {
    		try {
    			licenseIDs_list.add(Integer.parseInt(id));
    		}catch(NumberFormatException e) {

    		}
    	}

		String baseURL = contextPath+"/admin/profiles?administrative-continue="+knot.getId();

		// DIVISION: main
		Division main = body.addInteractiveDivision("edit-license", contextPath+"/admin/profiles", Division.METHOD_MULTIPART,"primary administrative licenses");
		main.setHead(T_head);

		IFunctionalities functionalityManager = DSpaceApi.getFunctionalityManager();
		functionalityManager.openSession();

		// LIST: options
		List options = main.addList("options",List.TYPE_SIMPLE,"horizontal");

		if(delete && licenseIDs_list.size()>0){
			options.addItem().addHighlight("bold").addXref(baseURL + "&license_list", new Message("default", "All Licenses"));
			options.addItem().addXref(baseURL + "&license_define", new Message("default", "Define License"));
			options.addItem().addXref(baseURL + "&label_define", new Message("default", "Define License Label"));

			Division confirm_div = main.addDivision("confirmation_message", "alert alert-error");

			confirm_div.addPara().addContent("You are about to delete the following License(s). This action will remove the license from bitstream(s) that are using these licenses.");
			confirm_div.addPara().addContent("Please press confirm only if you are SURE !");

			Table table = main.addTable("listLicenses", licenseIDs_list.size(), 4, "ds-table");
	    	Row r;
	    	Cell c;

	    	//header row
	    	r = table.addRow(Row.ROLE_HEADER);
	    	r.addCell().addContent("");
	    	r.addCell().addContent("License Name");
	    	r.addCell().addContent("Definition (URL)");
	    	r.addCell().addContent("Used by Bitstreams");

	    	for(int licenseId : licenseIDs_list) {
	    		LicenseDefinition license = functionalityManager.getLicenseByID(licenseId);

	    		r = table.addRow();

				c = r.addCell();
				c.addCheckBox("license_id").addOption(true, "" + license.getLicenseId());

	    		c = r.addCell();
    			c.addContent(license.getName());

    			c = r.addCell();
    			c.addContent(license.getDefinition());

    			int bitstreamCount = functionalityManager.getLicenseResources(licenseId);
    			c = r.addCell();
    			c.addContent(bitstreamCount);

	    	}

	    	r = table.addRow();
	    	Cell footer = r.addCell(1, 4);
    		footer.addButton("confirm").setValue("Confirm");
    		footer.addButton("cancel").setValue("Cancel");

		} else if(option.equals("license_list")) {
			options.addItem().addHighlight("bold").addXref(baseURL + "&license_list", new Message("default", "All Licenses"));
			options.addItem().addXref(baseURL + "&license_define", new Message("default", "Define License"));
			options.addItem().addXref(baseURL + "&label_define", new Message("default", "Define License Label"));

			java.util.List<LicenseDefinition> licenses = functionalityManager.getAllLicenses();
			java.util.List<LicenseLabel> labels = functionalityManager.getAllLicenseLabels();
			java.util.List<LicenseLabel> extendedLabels = functionalityManager.getAllLicenseLabelsExtended();

	    	main.addHidden("licenseIDs").setValue(licenseIDs);
	    	if(licenseIDs_list.isEmpty()) {
				Table table = main.addTable("listLicenses", licenses.size(), 5, "ds-table");
		    	Row r;
		    	Cell c;

		    	//header row
		    	r = table.addRow(Row.ROLE_HEADER);
		    	r.addCell().addContent("");
		    	r.addCell().addContent("License Name");
		    	r.addCell().addContent("Definition (URL)");
                r.addCell().addContent("Confirmation");
                r.addCell().addContent("Required user info");
		    	r.addCell().addContent("License Label");
		    	r.addCell().addContent("Extended Labels");
	            r.addCell().addContent("Used by Bitstreams");
		    	LicenseConfirmation licenseConfirmation[] = LicenseConfirmation.values();

		    	for(LicenseDefinition license : licenses) {
		    		r = table.addRow(null, null, "font_smaller");

	    			if(licenseIDs_list.size()==0) {
	    				c = r.addCell();
	    				c.addCheckBox("license_id").addOption(license.getLicenseId());
	    				c = r.addCell();
	    			} else {
	    				c = r.addCell(1, 2);
	    			}

	    			c.addContent(license.getName());

	    			c = r.addCell(null, null, "ldef");
	    			c.addContent(license.getDefinition());

                    c = r.addCell();
                    c.addContent(licenseConfirmation[license.getConfirmation()].msg);

                    c = r.addCell();
                    c.addContent(license.getRequiredInfo());

	    			c = r.addCell();
	    			c.addContent(license.getLicenseLabel().getLabel());

	    			c = r.addCell();
	    			for(LicenseLabel label : license.getLicenseLabelExtendedMappings()) {
	    				c.addContent(label.getLabel());
	    				c.addContent(" ");
	    			}

	                int bitstreamCount = functionalityManager.getLicenseResources(
	                                license.getLicenseId());
	                c = r.addCell();
	                c.addContent(bitstreamCount);

		    	}

		    	r = table.addRow();
		    	Cell footer = r.addCell(1, 8);
	    		footer.addButton("edit").setValue("Edit");
	    		footer.addButton("delete").setValue("Delete");

	    	} else {

		    	LicenseConfirmation licenseConfirmation[] = LicenseConfirmation.values();

	    		for(LicenseDefinition license : licenses) {

	    			if(!licenseIDs_list.contains(license.getLicenseId())) continue;

	    			Division edit_license_div = main.addDivision("edit_license_div", "edit_license_div");

	    			List edit_list = edit_license_div.addList("edit_form");

	    			edit_list.addLabel("License Name");
	    			edit_list.addItem().addText("license_name_" + license.getLicenseId(), "license_input").setValue(license.getName());

	    			edit_list.addLabel("Definition (URL)");
	    			edit_list.addItem().addText("license_url_" + license.getLicenseId(), "license_input").setValue(license.getDefinition());

	    			edit_list.addLabel("Confirmation");
		 			Select conf = edit_list.addItem().addSelect("license_confirmation_" + license.getLicenseId(), "license_input");
		 			for(LicenseConfirmation lc : licenseConfirmation) {
		 				conf.addOption(lc.ordinal(), lc.msg);
		 			}
		 			conf.setOptionSelected(license.getConfirmation());

		 			edit_list.addLabel("Label");
		 			Select lab = edit_list.addItem().addSelect("license_label_" + license.getLicenseId(), "license_input");
		 			for(LicenseLabel label : labels) {
		 				lab.addOption(label.getLabelId(), label.getLabel());
		 			}
		 			lab.setOptionSelected(license.getLicenseLabel().getLabelId());

		 			edit_list.addLabel("Extended Label(s)");
		 			Select labEx = edit_list.addItem().addSelect("license_label_extended_" + license.getLicenseId(), "license_input");
		 			labEx.setMultiple();
		 			labEx.setSize(4);

		 			for(LicenseLabel label : extendedLabels) {
		 				labEx.addOption(label.getLabelId(), label.getLabel());
		 			}

		 			for(LicenseLabel label : license.getLicenseLabelExtendedMappings()) {
		 				labEx.setOptionSelected(label.getLabelId());
		 			}

                    edit_list.addLabel(T_license_required);

                    ArrayList<String> requiredInfo = new ArrayList<String>();
                    if(license.getRequiredInfo() != null) {
	                    for(String rInfo : license.getRequiredInfo().split(",")) {
	                    	requiredInfo.add(rInfo.trim());
	                    }
                    }

            		CheckBox eFieldChk = edit_list.addItem().addCheckBox("license_required_" + license.getLicenseId(), "license_input");
            		for(ExtraLicenseField eField : ExtraLicenseField.values()) {
            			eFieldChk.addOption(requiredInfo.contains(eField.toString()), eField.toString(), message("xmlui.ExtraLicenseField.LicenseForm." + eField.toString()));
            		}


		 			int licensedBitstreams = functionalityManager.getLicenseResources(license.getLicenseId());

		 			if(licensedBitstreams > 0) {
		 				edit_license_div.addPara("bitsteram_notice", "alert")
		 					.addContent("This license is used by " + licensedBitstreams + " bitstreams. Updating the license information will also update the metadata of related items.");
		 			}


	    		}

	    		Table footer = main.addTable("buttons", 1, 1);
	    		Row r = footer.addRow();
		    	r.addCell().addButton("update").setValue("Update");
		    	r.addCell().addButton("cancel").setValue("Cancel");
	    	}

	    } else if(option.equals("license_define")) {
			options.addItem().addXref(baseURL + "&license_list", new Message("default", "All Licenses"));
			options.addItem().addHighlight("bold").addXref(baseURL + "&license_define", new Message("default", "Define License"));
			options.addItem().addXref(baseURL + "&label_define", new Message("default", "Define License Label"));
			defineLicenseForm(main);
		}
		functionalityManager.closeSession();

		main.addPara().addHidden("administrative-continue").setValue(knot.getId());

	}

	public static FlowResult removeLicense(Context context, int itemID) throws SQLException, AuthorizeException {
		FlowResult result = new FlowResult();
		result.setContinue(false);

		// clearing the metadata for old license
		Item item = Item.find(context, itemID);
		item.clearMetadata("dc", "rights", null, Item.ANY);
		item.clearMetadata("dc", "rights", "uri", Item.ANY);
		item.clearMetadata("dc", "rights", "label", Item.ANY);

		IFunctionalities functionalityManager = DSpaceApi.getFunctionalityManager();
		functionalityManager.openSession();

        Bundle[] bundles = item.getBundles("ORIGINAL");

        for(Bundle bundle : bundles){
        	Bitstream[] bitstreams = bundle.getBitstreams();
			for (Bitstream bitstream : bitstreams) {
				functionalityManager.detachLicenses(bitstream.getID());
			}
        }

        item.store_provenance_info("Removing license", context.getCurrentUser());
		item.update();

		result.setContinue(true);
		result.setOutcome(true);
		result.setMessage(message("xmlui.administrative.item.EditItemLicenseForm.updated_successfully"));

		functionalityManager.closeSession();
		return result;
	}

	public static FlowResult updateLicense(Context context, int itemID, int licenseID) throws SQLException, AuthorizeException {
		FlowResult result = new FlowResult();
		result.setContinue(false);

		if(licenseID != -1) {

			IFunctionalities functionalityManager = DSpaceApi.getFunctionalityManager();
			functionalityManager.openSession();

			LicenseDefinition license = functionalityManager.getLicenseByID(licenseID);

			// clearing the metadata for old license
			Item item = Item.find(context, itemID);
			item.clearMetadata("dc", "rights", null, Item.ANY);
			item.clearMetadata("dc", "rights", "uri", Item.ANY);
			item.clearMetadata("dc", "rights", "label", Item.ANY);

			// adding metadata for the new license
			item.addMetadata("dc", "rights", null, Item.ANY, license.getName());
			item.addMetadata("dc", "rights", "uri", Item.ANY, license.getDefinition());
			item.addMetadata("dc", "rights", "label", Item.ANY, license.getLicenseLabel().getLabel());


	        Bundle[] bundles = item.getBundles("ORIGINAL");

	        for(Bundle bundle : bundles){
	        	Bitstream[] bitstreams = bundle.getBitstreams();
				for (Bitstream bitstream : bitstreams) {
					functionalityManager.detachLicenses(bitstream.getID());
					functionalityManager.attachLicense(license.getLicenseId(), bitstream.getID());
				}
	        }

	        item.store_provenance_info("Adding license " + license.getName(), context.getCurrentUser());
			item.update();

			result.setContinue(true);
			result.setOutcome(true);
			result.setMessage(message("xmlui.administrative.item.EditItemLicenseForm.updated_successfully"));
			functionalityManager.closeSession();

		} else {
			result.setContinue(false);
			result.setOutcome(false);
			result.addError("licenseSelect");
			result.setMessage(message("xmlui.administrative.item.EditItemLicenseForm.select_license"));
		}
		return result;
	}

	public static FlowResult defineLicense(Context context, String name, String url, String confirmation, Object requiredInfo, String labelID, Object exLables) {
		FlowResult result = new FlowResult();

		EPerson currentUser = context.getCurrentUser();
		if(currentUser == null) {
			result.setMessage(message("xmlui.administrative.item.DefineLicenseForm.login_error"));
			return result;
		}

		if(name==null || name.equals("")) {
			result.setContinue(false);
			result.setOutcome(false);
			result.addError("License Name cannot be empty");
		}

		if(url==null || url.equals("")) {
			result.setContinue(false);
			result.setOutcome(false);
			result.addError("License Definition URL cannot be empty");
		}

		if(result.getErrors() != null) {
			result.setMessage(new Message("default", result.getErrorString().replace("," , ", ")));
			return result;
		}

		IFunctionalities functionalityManager = DSpaceApi.getFunctionalityManager();
		functionalityManager.openSession();

		functionalityManager.setErrorMessage("");

		String requiredInfoStr = null;

		if(requiredInfo != null) {
			if(requiredInfo instanceof String) {
				requiredInfoStr = (String)requiredInfo;
			} else {
				String temp = ((Vector<String>)requiredInfo).toString();
				requiredInfoStr = temp.substring(1, temp.length()-1);
			}
		}

		boolean success = functionalityManager.defineLicense(name, currentUser.getID(), url, Integer.parseInt(confirmation), requiredInfoStr, Integer.parseInt(labelID));
		String errorMessage = "";
		if(success) {
			Vector<String> extendedLabelIDs = new Vector<String>();

			if(exLables!=null) {
	    		if(exLables instanceof String) {
	    			extendedLabelIDs.add(exLables.toString());
	    		} else {
	    			extendedLabelIDs = (Vector<String>)exLables;
	    		}
			}

			LicenseDefinition license = functionalityManager.getLicenseByDefinition(url);


			Set<LicenseLabel> lables = new HashSet<LicenseLabel>();

			for(String lab : extendedLabelIDs) {
				LicenseLabel l = new LicenseLabel();
				l.setLabelId(Integer.parseInt(lab));
				lables.add(l);
			}

			license.setLicenseLabelExtendedMappings(lables);

			functionalityManager.update(LicenseDefinition.class, license);

			errorMessage = functionalityManager.getErrorMessage();

			result.setContinue(true);
			result.setOutcome(true);
			result.setMessage(message("xmlui.administrative.item.DefineLicenseForm.success_message"));
		} else {
			result.setOutcome(false);
			result.setContinue(false);
			if(errorMessage!=null && errorMessage.contains("license_definition_license_id_key")) {
				result.setMessage(message("xmlui.administrative.item.DefineLicenseForm.already_exists"));
			} else {
				result.setMessage(message("xmlui.administrative.item.DefineLicenseForm.unknown_error"));
			}
		}

		functionalityManager.closeSession();

		return result;
	}


	public static void defineLicenseForm(Division main) throws WingException {
    	Table table = main.addTable("defineLicense", 4, 2, "ds-table");
    	table.setHead(T_head_define);

    	Row r = table.addRow();
    	Cell c = r.addCell();
    	c.addContent(T_license_name);
    	c = r.addCell();
		Text text = c.addText("license_name");

		r = table.addRow();
		c = r.addCell();
		c.addContent(T_license_url);
		c = r.addCell();
		text = c.addText("license_url");

		r = table.addRow();
		c = r.addCell();
		c.addContent(T_license_confirmation);
		c = r.addCell();



		Select confirmation = c.addSelect("license_confirmation");

		for(LicenseConfirmation lc : LicenseConfirmation.values()) {
			confirmation.addOption(lc.ordinal(), lc.msg);
		}


		r = table.addRow();
		c = r.addCell();
		c.addContent(T_license_label);
		c = r.addCell();
		IFunctionalities licenseManager = DSpaceApi.getFunctionalityManager();
		licenseManager.openSession();

		java.util.List<LicenseLabel> label_defs = licenseManager.getAllLicenseLabels();
		if (label_defs!=null && !label_defs.isEmpty()) {

			Radio labels = c.addRadio("license_label");

			boolean selected = true; // first option will be selected by default

			for (LicenseLabel l : label_defs) {
				String item_label = l.getLabel();
				if (!l.getTitle().isEmpty()) {
					item_label = String.format("%s (%s)", l.getTitle(), l.getLabel());
				}
				labels.addOption(selected, l.getLabelId(), item_label);
				selected = false;
			}
		}

		r = table.addRow();
		c = r.addCell();
		c.addContent(T_license_label);
		c = r.addCell();
		label_defs = licenseManager.getAllLicenseLabelsExtended();
		if (label_defs!=null && !label_defs.isEmpty()) {

			CheckBox labels = c.addCheckBox("license_label_extended");

			for (LicenseLabel l : label_defs) {
				String item_label = l.getLabel();
				if (!l.getTitle().isEmpty()) {
					item_label = String.format("%s (%s)", l.getTitle(), l.getLabel());
				}
				labels.addOption(l.getLabelId(), item_label);
			}
		}

		/*String possibleReq = DSpaceApi.getExtraLicenseFields().toString();

		r = table.addRow();
		r.addCell().addContent("Possible values for the required field:");
		r.addCell().addContent(possibleReq);
		//
        r = table.addRow();
        r.addCell().addContent(T_license_required);
        r.addCell().addText("license_required");*/


		r = table.addRow();
		r.addCell().addContent(T_license_required);
		c = r.addCell();
		CheckBox eFieldChk = c.addCheckBox("license_required");
		for(ExtraLicenseField eField : ExtraLicenseField.values()) {
			eFieldChk.addOption(eField.toString(), message("xmlui.ExtraLicenseField.LicenseForm." + eField.toString()));
		}

        // controls
		r = table.addRow();
		c = r.addCell(0, 2);
		c.addButton("new_license_save").setValue(T_save);
		c.addButton("new_license_return").setValue(T_return);

		licenseManager.closeSession();
	}

	public static FlowResult updateLicense(Context context, RequestWrapper request) {
		FlowResult result = new FlowResult();

		//if everything works
		result.setContinue(true);
		result.setOutcome(true);
		result.setMessage(new Message("default", "License(s) updated successfully"));

		IFunctionalities licenseManager = DSpaceApi.getFunctionalityManager();
		licenseManager.openSession();

    	String licenseIDs = (String)request.get("licenseIDs");
    	ArrayList<Integer> licenseIDs_list = new ArrayList<Integer>();
    	for(String id : licenseIDs.split("\\D+")) {
    		try {
    			licenseIDs_list.add(Integer.parseInt(id));
    		}catch(NumberFormatException e) {

    		}
    	}
    	licenseIDs = "[";
    	for(int licenseId : licenseIDs_list) {

    		String name = ((String)request.get("license_name_" + licenseId)).trim();
    		String definition = ((String)request.get("license_url_" + licenseId)).trim();
    		String confirmation = (String)request.get("license_confirmation_" + licenseId);
            String label = (String)request.get("license_label_" + licenseId);

            String requiredInfoStr = null;

            Object requiredInfo = request.get("license_required_" + licenseId);

            if(requiredInfo != null) {
            	if(requiredInfo instanceof String) {
            		requiredInfoStr = (String)requiredInfo;
            	} else {
            		String temp = requiredInfo.toString();
            		requiredInfoStr = temp.substring(1, temp.length()-1);
            	}
            }

    		Vector<String> extendedLabelIDs = new Vector<String>();
    		Object exLabIds = request.get("license_label_extended_" + licenseId);
    		if(exLabIds!=null) {
	    		if(exLabIds instanceof String) {
	    			extendedLabelIDs.add(exLabIds.toString());
	    		} else {
	    			extendedLabelIDs = (Vector<String>)request.get("license_label_extended_" + licenseId);
	    		}
    		}

    		if(name==null || name.equals("")) {
    			result.setContinue(false);
    			result.setOutcome(false);
    			result.addError("License name cannot be empty.");
    			licenseIDs += licenseId + ", ";
    			continue;
    		}

    		if(definition==null || definition.equals("")) {
    			result.setContinue(false);
    			result.setOutcome(false);
    			result.addError("License definition(URL) cannot be empty.");
    			licenseIDs += licenseId + ", ";
    			continue;
    		}

    		LicenseDefinition license = licenseManager.getLicenseByID(licenseId);
    		license.setName(name);
    		license.setDefinition(definition);
    		license.setConfirmation(Integer.parseInt(confirmation));
    		license.setRequiredInfo(requiredInfoStr);
    		LicenseLabel lLabel = (LicenseLabel)licenseManager.findById(LicenseLabel.class, Integer.parseInt(label));
    		license.setLicenseLabel(lLabel);

    		Set<LicenseLabel> exLab = new HashSet<LicenseLabel>();
    		license.setLicenseLabelExtendedMappings(exLab);

    		for(String labelId : extendedLabelIDs) {
    			LicenseLabel exLabel = (LicenseLabel)licenseManager.findById(LicenseLabel.class, Integer.parseInt(labelId));
    			exLab.add(exLabel);
    		}

    		boolean status = licenseManager.update(LicenseDefinition.class, license);

    		if(!status) {
    			result.setContinue(false);
    			result.setOutcome(false);
    			result.addError(licenseManager.getErrorMessage());
    			licenseIDs += licenseId + ", ";
    		}

    		try {
    			for(LicenseResourceMapping lm : license.getLicenseResourceMappings()) {
    				Bitstream bitstream = Bitstream.find(context, lm.getBitstreamId());
    				Item item = (Item)bitstream.getParentObject();
    				if(item!=null) {
    					item.clearMetadata("dc", "rights", null, Item.ANY);
    					item.clearMetadata("dc", "rights", "uri", Item.ANY);
    					item.clearMetadata("dc", "rights", "label", Item.ANY);

    					// adding metadata for the new license
    					item.addMetadata("dc", "rights", null, Item.ANY, license.getName());
    					item.addMetadata("dc", "rights", "uri", Item.ANY, license.getDefinition());
    					item.addMetadata("dc", "rights", "label", Item.ANY, license.getLicenseLabel().getLabel());

    					item.update();
    				}
    			}
    			context.commit();
    		} catch (Exception e) {
    			result.setContinue(false);
    			result.setOutcome(false);
    			result.addError(e.getLocalizedMessage());
    			licenseIDs += licenseId + ", ";
    		}


    	}

    	licenseIDs += "]";
    	result.setParameter("licenseIDs", licenseIDs);


    	if(result.getErrorString()!=null) {
    		result.setMessage(new Message("default", result.getErrorString()));
    	}

    	licenseManager.closeSession();

		return result;
	}

	public static FlowResult deleteLicense(Context context, RequestWrapper request) {
		FlowResult result = new FlowResult();

		IFunctionalities licenseManager = DSpaceApi.getFunctionalityManager();
		licenseManager.openSession();
		
    	String licenseIDs[] = request.getParameterValues("license_id");
    	ArrayList<Integer> licenseIDs_list = new ArrayList<Integer>();
    	for(String id : licenseIDs) {
    		try {
    			licenseIDs_list.add(Integer.parseInt(id));
    		}catch(NumberFormatException e) {
    			
    		}
    	}
    	
    	for(int licenseId : licenseIDs_list) {
    		LicenseDefinition license = licenseManager.getLicenseByID(licenseId);
    		Set<LicenseResourceMapping> mappings
    							= (Set<LicenseResourceMapping>)license.getLicenseResourceMappings();
    		java.util.List<Integer> items = new ArrayList<Integer>();
    		for(LicenseResourceMapping mapping : mappings) {
    			int bitstreamID = mapping.getBitstreamId();
    			
    			try {
					Bitstream bitstream = Bitstream.find(context, bitstreamID);
					Item item = (Item)bitstream.getParentObject();
					if(!items.contains(item.getID())) {
						items.add(item.getID());
    					item.clearMetadata("dc", "rights", null, Item.ANY);
    					item.clearMetadata("dc", "rights", "uri", Item.ANY);
    					item.clearMetadata("dc", "rights", "label", Item.ANY);	
    					item.update();
					}
				} catch (Exception e) {
				}
    		}
    		    		
    		boolean status = licenseManager.delete(LicenseDefinition.class, license);    		
    		if(!status) {
    			result.setContinue(false);
    			result.setOutcome(false);
    			result.addError(licenseManager.getErrorMessage());
    		}
    	}
    	
    	if(result.getErrorString()!=null) {
    		result.setMessage(new Message("default", result.getErrorString()));
    	} else {
			result.setContinue(true);
			result.setOutcome(true);
    		result.setMessage(new Message("default", "License(s) deleted successfully"));
    	}
    	
    	try {
			context.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
    	
    	licenseManager.closeSession();
    	
    	return result;
	}

	enum LicenseConfirmation {
		
		NO_CONFIRMATION("Not required"),
		ONLY_ONCE("Ask only once"),
		ALWAYS_ASK("Ask always");

		private final String msg;
		
		
		private LicenseConfirmation(String msg) {
			this.msg = msg;
		}
		
		@Override
	    public String toString() {
			return msg;
		}
		
	};

}


