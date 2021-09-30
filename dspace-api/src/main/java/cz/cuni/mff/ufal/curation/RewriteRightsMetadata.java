/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.curation;

import cz.cuni.mff.ufal.DSpaceApi;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseDefinition;
import cz.cuni.mff.ufal.lindat.utilities.interfaces.IFunctionalities;
import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;

import java.io.IOException;

public class RewriteRightsMetadata extends AbstractCurationTask {

    private int status = Curator.CURATE_UNSET;

    // The log4j logger for this class
    private static Logger log = Logger.getLogger(Curator.class);

	@Override
	public int perform(DSpaceObject dso) throws IOException {

		// The results that we'll return
        StringBuilder results = new StringBuilder();

        // Unless this is  an item, we'll skip this item
        status = Curator.CURATE_SKIP;
		
        if (dso instanceof Item)
        {
        	try {
	            Item item = (Item)dso;
	          
	            IFunctionalities utilities = DSpaceApi.getFunctionalityManager();
	            utilities.openSession();
	            LicenseDefinition license = null;
	            
	            Metadatum[] rights = item.getMetadata("dc", "rights", "uri", Item.ANY);
	            if(rights != null && rights.length>0) {
	            	String licenseDefinition = rights[0].value;            	            
	            	license = utilities.getLicenseByDefinition(licenseDefinition);            
	            }
	
	            if(license!=null) {                        
	            	item.clearMetadata("dc", "rights", "label", Item.ANY);
	            	item.addMetadata("dc", "rights", "label", Item.ANY, license.getLicenseLabel().getLabel());
	            	// null qualifier means unqualified
	            	item.clearMetadata("dc", "rights", null, Item.ANY);
	            	item.addMetadata("dc", "rights", null, Item.ANY, license.getName());
	            	item.update();
	            }
	        	status = Curator.CURATE_SUCCESS;
	        	
	        	utilities.closeSession();
	        	
        	} catch (Exception ex) {
        		status = Curator.CURATE_FAIL;
        	}
        }
        
        report(results.toString());
        setResult(results.toString());
		return status;
	}


}

