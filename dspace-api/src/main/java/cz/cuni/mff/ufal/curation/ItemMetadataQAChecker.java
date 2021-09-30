/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.curation;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import cz.cuni.mff.ufal.IsoLangCodes;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.content.*;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.handle.HandleManager;
import org.dspace.utils.DSpace;

import static cz.cuni.mff.ufal.curation.RequiredMetadata.addMagicString;


/**
 * Check basic properties
 */
@SuppressWarnings("deprecation")
public class ItemMetadataQAChecker extends AbstractCurationTask {

    public static final int CURATE_WARNING = -1000;
    /** Expected types. */
    private static final String[] DCTYPE_VALUES = new DSpace().getConfigurationService().getPropertyAsType("lr.curation.metadata.expected.types" ,
            new String[]{ "corpus", "lexicalConceptualResource", "languageDescription", "toolService" });
    private static final Set<String> DCTYPE_VALUES_SET = new HashSet<String>(
        Arrays.asList(DCTYPE_VALUES));

    private static final String[] rightsMdStrings = {"dc.rights.uri", "dc.rights.label", "dc.rights"};

    private Map<String, String> _item_titles;
    private String _handle_prefix;

    private Map<String, Integer> _complex_inputs;

    // The log4j logger for this class
    private static Logger log = Logger.getLogger(Curator.class);

    //
    //
    //

    @Override
    public
    void init(Curator curator, String taskId) throws IOException
    {
        super.init(curator, taskId);
        _item_titles = new HashMap<String,String>();
        _handle_prefix = ConfigurationManager.getProperty("handle.canonical.prefix");

        _complex_inputs = new HashMap<String, Integer>();
        loadComplexInputs();
    }

	private void loadComplexInputs() {
		try {
			DCInputsReader reader = new DCInputsReader();
			int numPages = reader.getNumberInputPages(null);
			for (int i = 0; i < numPages; i++) {
				for (DCInput input : reader.getInputs(null).getPageRows(i,
						false, true)) {
					if ("complex".equals(input.getInputType())) {
						String name = StringUtils.isBlank(input.getQualifier()) ? String
								.format("%s.%s", input.getSchema(),
										input.getElement()) : String.format(
								"%s.%s.%s", input.getSchema(),
								input.getElement(), input.getQualifier());
						_complex_inputs.put(name, input.getComplexDefinition()
								.getInputNames().size());
					}
				}
			}
		} catch (DCInputsReaderException e) {
			log.error("Problems fetching input-forms.xml");
		}
	}

	private String get_handle(Item item) {
        if ( null != item.getHandle() ) {
            return _handle_prefix + item.getHandle();
        }else {
            return "item id: " +item.getHandle();
        }
    }


    @Override
    public
    int perform(DSpaceObject dso) throws IOException 
    {
        int status = Curator.CURATE_UNSET;
        StringBuilder results = new StringBuilder();
        String err_str = "Unknown error";

        // do on Items only
        if (dso instanceof Item) 
        {
            Item item = (Item) dso;
            if (item.getHandle() != null) 
            {
                Metadatum[] dcs = item.getMetadata(
                    MetadataSchema.DC_SCHEMA, Item.ANY, Item.ANY, Item.ANY);
                
                // no metadata?
                if ( dcs == null || dcs.length == 0) {
                    err_str = "Does not have any metadata";
                    status = Curator.CURATE_FAIL;
                }else 
                {
                    // perform the validation
                    try {
                        //
                        validate_dc_type(item, dcs, results);
                        //
                        validate_title(item, dcs, results);
                        // check whether the language iso code is valid
                        validate_dc_language_iso(item, dcs, results);
                        //
                        validate_relation(item, dcs, results);
                        //
                        validate_empty_metadata(item, dcs, results);
                        //
                        validate_duplicate_metadata(item, dcs, results);
                        //
                        validate_strange_metadata(item, dcs, results);
                        //
                        validate_branding_consistency(item, dcs, results);
                        //
                        validate_rights_labels(item, dcs, results);
                        //
                        item_with_files_has_license(item);
                        //
                        validate_highly_recommended_metadata(item, dcs, results);
                        //
                        validate_complex_inputs(item, dcs, results);

                        status = Curator.CURATE_SUCCESS;
                    }catch(CurateException exc) {
                        err_str = exc.getMessage();
                        status = exc.err_code;
                    }
                }

            // no handle!
            } else {
                err_str = "Does not have a handle";
                status = Curator.CURATE_FAIL;
            }
            
            // format the error if any
            switch (status)
            {
                case Curator.CURATE_SUCCESS:
                    break;
                case CURATE_WARNING:
                    results.append( String.format("Warning: %s %s",
                            err_str, addMagicString(get_handle(item)))
                    );
                    break;
                default:
                    results.append( String.format("ERROR! %s %s",
                        err_str, addMagicString(get_handle(item)))
                    );
                    break;
            }
        }

        report(results.toString());
        setResult(results.toString());
        return status;
    }

    //
    // dc type checker
    //
    
    private void validate_dc_type(Item item, Metadatum[] dcs, StringBuilder results) 
                    throws CurateException
    {
        Metadatum[] dcs_type = item.getMetadataByMetadataString("dc.type");
        // no metadata?
        if ( dcs_type == null || dcs_type.length == 0) {
            throw new CurateException("Does not have dc.type metadata",
                Curator.CURATE_FAIL );
        }
        
        // check array is not null or length > 0
        for (Metadatum dcsEntry : dcs_type) 
        {
            String typeVal = dcsEntry.value.trim();
            
            // check if original and trimmed versions match
            if (!typeVal.equals(dcsEntry.value)) {
                throw new CurateException("leading or trailing spaces",
                    Curator.CURATE_FAIL);
            }
            
            // check if the dc.type field is empty
            if (Pattern.matches("^\\s*$", typeVal)) {
                throw new CurateException("empty value",
                    Curator.CURATE_FAIL);
            }

            // check if the value is valid
            if (!DCTYPE_VALUES_SET.contains(typeVal)) {
                throw new CurateException("invalid type" + "(" + typeVal + ")",
                    Curator.CURATE_FAIL);
            }
        }
    }

    /**
     * Checks the language code (dc.language.iso) against the possible language codes
     *
     * @param item
     * @param dcs
     * @param results
     * @throws CurateException
     */

    private void validate_dc_language_iso(Item item, Metadatum[] dcs, StringBuilder results) throws CurateException {
    	Metadatum[] dcs_language_iso = item.getMetadataByMetadataString("dc.language.iso");
        if ( dcs_language_iso != null ) {
        	for (Metadatum langCodeDC : dcs_language_iso) {
        		String langCode = langCodeDC.value;
        		if (IsoLangCodes.getLangForCode(langCode) == null) {
                    throw new CurateException(
                            String.format("Invalid language code - %s", langCode),
                            Curator.CURATE_FAIL );        			
        		}
        	}
        }
    }
    
    
    //
    // relation checker
    //
    
    private void validate_title(Item item, Metadatum[] dcs, StringBuilder results) 
                    throws CurateException
    {
        String title = item.getName();
        if ( _item_titles.containsKey(title)) {
            String msg = String.format("Title [%s] duplicate in [%s]",
                            title, _item_titles.get(title));
            throw new CurateException(msg, Curator.CURATE_FAIL);
        }
        _item_titles.put(title, get_handle(item));
    }
    
    //
    // relation checker
    //
    
    private void validate_relation(Item item, Metadatum[] dcs, StringBuilder results) 
                    throws CurateException
    {
        Context context = null;
        String handle_prefix = ConfigurationManager.getProperty("handle.canonical.prefix");
        try {
            for ( String[] two_way_relation : new String[][] {
                new String[] {
                    "dc.relation.isreplacedby",
                    "dc.relation.replaces"
                },
            })
            {
                String lhs_relation = two_way_relation[0];
                String rhs_relation = two_way_relation[1];

                Metadatum[] dcs_replaced = item.getMetadataByMetadataString(lhs_relation);
                if (dcs_replaced.length == 0) {
                    return;
                }

                int status = Curator.CURATE_FAIL;
                context = new Context();
                for (Metadatum dc : dcs_replaced) {
                    String handle = dc.value.replaceAll(handle_prefix, "");
                    DSpaceObject dso_mentioned = HandleManager.resolveToObject(context, handle);
                    if (dso_mentioned instanceof Item) {
                        Item item_mentioned = (Item) dso_mentioned;
                        Metadatum[] dcs_mentioned = item_mentioned.getMetadataByMetadataString(rhs_relation);
                        for (Metadatum dc_mentioned : dcs_mentioned) {
                            String handle_mentioned = dc_mentioned.value.replaceAll(
                                handle_prefix, "");
                            // compare the handles
                            if (handle_mentioned.equals(item.getHandle())) {
                                status = Curator.CURATE_SUCCESS;
                                results.append(
                                    String.format("Item [%s] meets relation requirements", get_handle(item)));
                                break;
                            }
                        }
                    }
                }

                // indicate fail
                if (status != Curator.CURATE_SUCCESS) {
                    throw new CurateException(
                        String.format("contains %s but the referenced object " +
                            "does not contain %s or does not point to the item itself!\n",
                            lhs_relation, rhs_relation),
                        status);
                }
            }

            if (context != null) {
                context.complete();
            }

        } catch (Exception e) {
            if ( context != null ) {
                context.abort();
            }
            throw new CurateException(e.getMessage(), Curator.CURATE_FAIL);
        }
    }
    
    private void validate_empty_metadata(Item item, Metadatum[] dcs, StringBuilder results) 
                    throws CurateException
    {
        for ( Metadatum dc : dcs ) 
        {
            if ( null == dc.value ) {
                throw new CurateException(
                    String.format("value [%s.%s.%s] is null", dc.schema, dc.element, dc.qualifier),
                    Curator.CURATE_FAIL);
            }
            if ( 0 == dc.value.trim().length() ) {
                throw new CurateException(
                    String.format("value [%s.%s.%s] is empty", dc.schema, dc.element, dc.qualifier),
                    Curator.CURATE_FAIL);
            }
        }
    }

    private void validate_duplicate_metadata(Item item, Metadatum[] dcs, StringBuilder results)
        throws CurateException
    {
        for ( String no_duplicate : new String[] {
            "local.branding",
            "dc.type",
            "dc.date.accessioned",
            "dc.rights.label",
            "dc.date.available",
            "dc.source.uri",
            "metashare.ResourceInfo#DistributionInfo#LicenseInfo.license"
        })
        {
            Metadatum[] vals = item.getMetadataByMetadataString(no_duplicate);
            if ( null != vals && vals.length > 1 ) {
                throw new CurateException(
                    String.format("value [%s] is present multiple times", no_duplicate),
                    Curator.CURATE_FAIL);
            }
        }
    }

    private void validate_branding_consistency(Item item, Metadatum[] dcs, StringBuilder results)
        throws CurateException
    {
        try {
            Community c[] = item.getCommunities();
            if ( c!=null && c.length>0) {
                String c_name = c[0].getName();
                Metadatum[] brandings = item.getMetadata("local", "branding", null, Item.ANY);
                if ( 1 != brandings.length ) {
                    throw new CurateException(
                        String.format("local.branding present [%d] count", brandings.length),
                            Curator.CURATE_FAIL);
                }
                if (!c_name.equals(brandings[0].value)) {
                    throw new CurateException(
                        String.format("local.branding [%s] does not match community [%s]",
                            brandings[0].value, c_name),
                        Curator.CURATE_FAIL);
                }
            }
        } catch (SQLException e) {
            throw new CurateException(
                String.format("has invalid community [%s]", e.getMessage()),
                Curator.CURATE_FAIL);

        }
    }

    private void validate_rights_labels(Item item, Metadatum[] dcs, StringBuilder results)
        throws CurateException
    {
        Metadatum[] dcvs = item.getMetadata("dc", "rights", "label", Item.ANY);
        try {
            if (null != item.getHandle() && !item.hasUploadedFiles() && dcvs != null && dcvs.length > 0) {
                StringBuilder labels = new StringBuilder();
                for (Metadatum label : dcvs) {
                    labels.append(label.value).append(" ");
                }
                throw new CurateException(
                    String.format("has labels [%s] but no files", labels.toString()),
                    Curator.CURATE_FAIL);
            }
        } catch (SQLException e) {
            throw new CurateException(
                String.format("has internal problems [%s]", e.getMessage()),
                Curator.CURATE_FAIL);
        }
    }

    private void validate_highly_recommended_metadata(Item item, Metadatum[] dcs, StringBuilder results)
        throws CurateException
    {
        for ( String md : new String[] {
            "dc.subject",
        })
        {
            Metadatum[] vals = item.getMetadataByMetadataString(md);
            if ( null == vals || 0 == vals.length ) {
                throw new CurateException(
                    String.format("does not contain any [%s] values", md),
                    CURATE_WARNING);
            }
        }
    }


    private void validate_strange_metadata(Item item, Metadatum[] dcs, StringBuilder results)
                    throws CurateException
    {
        for ( String md : new String[] {
            "dc.description.uri",
        }) 
        {
            Metadatum[] vals = item.getMetadataByMetadataString(md);
            if ( null != vals && vals.length > 0 ) {
                throw new CurateException(
                    String.format("contains suspicious [%s] metadata", md),
                    Curator.CURATE_FAIL);
            }
        }
    }
    
	private void validate_complex_inputs(Item item, Metadatum[] dcs,
			StringBuilder results) throws CurateException {
		for (Entry<String, Integer> entry : _complex_inputs.entrySet()) {

			for (Metadatum dval : item.getMetadataByMetadataString(entry.getKey())) {
				String val = dval.value;
				if (val.split(DCInput.ComplexDefinition.SEPARATOR, -1).length != entry
						.getValue()) {
					throw new CurateException(
							String.format(
									"%s is a component with %s values but is not stored as such. [%s]",
									entry.getKey(), entry.getValue(), val),
							Curator.CURATE_FAIL);
				}
			}
		}
	}

	private void item_with_files_has_license(Item item) throws CurateException {
        try {
            boolean fail = false;
            StringBuilder sb = new StringBuilder();
            if (item.getNonInternalBitstreams().length > 0) {
                for(String mdString : rightsMdStrings){
                    final Metadatum[] vals = item.getMetadataByMetadataString(mdString);
                    if(vals == null || vals.length == 0){
                        fail = true;
                        sb.append(mdString).append(", ");
                    }
                }
            }
            if(fail){
                throw new CurateException("There are bitsreams but incomplete rights metadata. Missing: " + sb.toString(), Curator.CURATE_FAIL);
            }
        } catch (SQLException throwables) {
            throw  new CurateException(throwables.getMessage(), Curator.CURATE_ERROR);
        }


    }

} // class ItemMetadataQAChecker


/**
 * Curate exception.
 */
class CurateException extends Exception 
{
    int err_code;
    
    public CurateException(String message, int err_code) {
        super(message);
        this.err_code = err_code;
    }
}
