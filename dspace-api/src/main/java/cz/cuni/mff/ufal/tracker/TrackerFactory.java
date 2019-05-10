/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.tracker;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;

public class TrackerFactory
{
    /** log4j category */
    private static Logger log = Logger.getLogger(TrackerFactory.class);
    private static final ConfigurationService configurationService = new DSpace().getConfigurationService();

    public static Tracker createOAITrackerInstance(){
        String trackerType = configurationService.getProperty("lr.lr.tracker.type");
        if (trackerType.equalsIgnoreCase("piwik"))
        {
            return PiwikTrackerFactory.createOAITrackerInstance();
        }
        throw new IllegalArgumentException("Invalid tracker type:" + trackerType);
    }

    public static Tracker createBitstreamTrackerInstance(Collection col){
        String trackerType = configurationService.getProperty("lr.lr.tracker.type");
        if (trackerType.equalsIgnoreCase("piwik"))
        {
            return PiwikTrackerFactory.createBitstreamTrackerInstance(col);
        }
        throw new IllegalArgumentException("Invalid tracker type:" + trackerType);
    }
}
