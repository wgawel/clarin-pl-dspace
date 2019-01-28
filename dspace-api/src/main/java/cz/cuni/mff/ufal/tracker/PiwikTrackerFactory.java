/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.tracker;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;

import javax.servlet.http.HttpServletRequest;

public class PiwikTrackerFactory
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(PiwikTrackerFactory.class);
    private static final ConfigurationService configurationService = new DSpace().getConfigurationService();
    private static final Tracker noopTracker = new Tracker(){

        @Override
        public void trackDownload(HttpServletRequest request) {
            log.debug("NOOP: trackDownload");
        }

        @Override
        public void trackPage(HttpServletRequest request, String pageName) {
            log.debug("NOOP: trackPage");
        }
    };

    public static Tracker createInstance(TrackingSite site, Object... params)
    {
        switch(site) {
            case OAI:
                return new PiwikOAITracker();                
            case BITSTREAM:
                int defaultSiteId = configurationService.getPropertyAsType("lr.lr.tracker.bitstream.site_id", 0);
                if(params.length == 1 && params[0] instanceof Collection){
                    Collection col = (Collection)params[0];
                    boolean disabled = configurationService.getPropertyAsType("lr.lr.tracker.collection."
                            + col.getHandle() + ".disabled", false);
                    if(disabled){
                        return noopTracker;
                    }else{
                        int siteId = configurationService.getPropertyAsType(
                                "lr.lr.tracker.bitstream.from." + col.getHandle() +".site_id", defaultSiteId);
                        return new PiwikBitstreamTracker(siteId);
                    }
                }else{
                    return new PiwikBitstreamTracker(defaultSiteId);
                }
        }
        throw new IllegalArgumentException("Unknown site: " + site);
    }
}
