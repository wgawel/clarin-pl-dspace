/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.tracker;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.piwik.PiwikException;

public class PiwikBitstreamTracker extends PiwikTracker
{
    /** log4j category */
    private static Logger log = Logger.getLogger(TrackerFactory.class);
    private int siteId;

    PiwikBitstreamTracker(int siteId){
        super();
        this.siteId = siteId;
    }

    @Override
    protected void preTrack(HttpServletRequest request) {
        super.preTrack(request);
        tracker.setIdSite(siteId);
        log.debug("Logging to site " + tracker.getIdSite());
        try
        {
            tracker.setPageCustomVariable("source", "bitstream");
        }
        catch (PiwikException e)
        {
            log.error(e);
        }
    }
}
