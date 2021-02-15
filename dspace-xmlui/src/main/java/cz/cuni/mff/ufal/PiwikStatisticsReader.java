package cz.cuni.mff.ufal;

import java.io.*;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.reading.AbstractReader;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;
import org.xml.sax.SAXException;

public class PiwikStatisticsReader extends AbstractReader {

	private static Logger log = Logger.getLogger(PiwikStatisticsReader.class);
	
    /** The Cocoon response */
    protected Response response;

    /** The Cocoon request */
    protected Request request;
    
    /** Item requesting statistics */
    private Item item = null;

    /** True if user agent making this request was identified as spider. */
    private boolean isSpider = false;
    
    private String rest = "";

	private String dspaceURL = ConfigurationManager.getProperty("dspace.url");

	/** Piwik configurations */
    private static final String PIWIK_API_MODE = ConfigurationManager.getProperty("lr", "lr.statistics.api.mode");
    private static final String PIWIK_API_URL = ConfigurationManager.getProperty("lr", "lr.statistics.api.url");
    private static final String PIWIK_API_URL_CACHED = ConfigurationManager.getProperty("lr", "lr.statistics.api.cached.url");
    private static final String PIWIK_AUTH_TOKEN = ConfigurationManager.getProperty("lr", "lr.statistics.api.auth.token");
    private static final String PIWIK_SITE_ID = ConfigurationManager.getProperty("lr", "lr.statistics.api.site_id");
	private static final String PIWIK_DOWNLOAD_SITE_ID = ConfigurationManager.getProperty("lr", "lr.tracker.bitstream.site_id");	

    /**
     * Set up the PiwikStatisticsReader
     *
     * See the class description for information on configuration options.
     */
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters par) throws ProcessingException, SAXException, IOException {
        super.setup(resolver, objectModel, src, par);
        
        try
        {
        	
            Context context = ContextUtil.obtainContext(objectModel);
        	
            this.request = ObjectModelHelper.getRequest(objectModel);
            this.response = ObjectModelHelper.getResponse(objectModel);
            
            rest = par.getParameter("rest", null);

            String handle = par.getParameter("handle", null);
            
            this.isSpider = par.getParameter("userAgent", "").equals("spider");
            
         	// Reference by an item's handle.
            DSpaceObject dso = HandleManager.resolveToObject(context, handle);

            if (dso instanceof Item) {
                item = (Item)dso;                
            } else {
            	throw new ResourceNotFoundException("Unable to locate item");
            }

            EPerson eperson = context.getCurrentUser();
            
            if(eperson == null) {
            	throw new AuthorizeException();
            }
            
        } catch (AuthorizeException | SQLException | IllegalStateException e) {
            throw new ProcessingException("Unable to read piwik statistics", e);
        }

    }


	@Override
	public void generate() throws ProcessingException {
		try {
			String mergedResult = "";
			if(PIWIK_API_MODE.equals("cached")) {
				log.debug("========CACHED MODE");
				mergedResult = getDataFromLindatPiwikCacheServer();
			} else {
				// direct mode as default
				log.debug("========DIRECT MODE");
				mergedResult = getDataFromPiwikServer();
			}
			out.write(mergedResult.getBytes());
			out.flush();
		} catch (Exception e) {
			throw new ProcessingException("Unable to read piwik statisitcs", e);
		}
	}

	private String getDataFromPiwikServer() throws Exception {
		String viewsURL = buildViewsURL();
		String downloadURL = buildDownloadsURL();
		String bulkApiGetRequestURL = buildBulkApiGetRequestURL(viewsURL, downloadURL);

		log.debug(String.format("Fetching data from piwik server; requesting \"%s\"", bulkApiGetRequestURL));

		String report = PiwikHelper.readFromURL(bulkApiGetRequestURL);
		return PiwikHelper.transformJSONResults(report);
	}

	private String buildBulkApiGetRequestURL(String... urls){
		String piwikBulkApiGetQuery = "module=API&method=API.getBulkRequest&format=JSON"
				+ "&token_auth=" + PIWIK_AUTH_TOKEN;
		StringBuilder sb = new StringBuilder();
		sb.append(PIWIK_API_URL)
				.append(rest)
				.append("?")
				.append(piwikBulkApiGetQuery);
		for(int i=0; i<urls.length; i++){
			sb.append("&urls[")
				.append(i)
				.append("]=")
				.append(urls[i]);
		}
		return sb.toString();
	}

	private String buildViewsURL() throws UnsupportedEncodingException, ParseException {
		return buildURL(PIWIK_SITE_ID, dspaceURL + "/handle/" + item.getHandle());
	}

	private String buildDownloadsURL() throws UnsupportedEncodingException, ParseException {
		return buildURL(PIWIK_DOWNLOAD_SITE_ID, dspaceURL + "/bitstream/handle/" + item.getHandle());
	}

	private String buildURL(String siteID, String filterPattern) throws UnsupportedEncodingException, ParseException {
		// should contain the period
		String period = request.getParameter("period");
		String dateRange = DateRange.fromDateString(request.getParameter("date")).toString();

		/*
		The Actions API lets you request reports for all your Visitor Actions: Page URLs, Page titles, Events,
		Content Tracking, File Downloads and Clicks on external websites.
		Actions.getPageUrls:
		 - stats(nb_visits, nb_hits, etc) per url, in given date broken down by period
		expanded:
		- some API functions have a parameter 'expanded'. If 'expanded' is set to 1, the returned data will contain the first level results, as well as all sub-tables.
		- basically fetches subtable (if present as idsubdatatable)
		- eg. urls broken down by directory structure:
			- lvl 1: <segment>pageUrl=^https%253A%252F%252Fdivezone.net%252Fdiving</segment>
			- lvl 2: <segment>pageUrl==https%253A%252F%252Fdivezone.net%252Fdiving%252Fbali</segment>; <segment>pageUrl==https%253A%252F%252Fdivezone.net%252Fdiving%252Fthailand</segment>
			- lvl 1 has no url, ie. it's cummulative for the "underlying" urls
		flat:
		- some API functions have a parameter 'expanded', which means that the data is hierarchical. For such API function, if 'flat' is set to 1, the returned data will contain the flattened view of the table data set. The children of all first level rows will be aggregated under one row. This is useful for example to see all Custom Variables names and values at once, for example, Matomo forum user status, or to see the full URLs not broken down by directory or structure.
		- this will remove the cummulative results; all rows will be of the same type (having url field)
		 */
		String piwikApiGetQuery = "method=Actions.getPageUrls&expanded=1&flat=1";
		String params =
				"&date=" + dateRange
				+ "&period=" + period
				+ "&token_auth=" + PIWIK_AUTH_TOKEN
				+ "&showColumns=label,url,nb_visits,nb_hits"
				// don't want to handle paging
				+ "&filter_limit=-1"
				+ "&filter_column=url"
				+ "&filter_pattern=" + filterPattern
				+ "&idSite=" + siteID;
		return URLEncoder.encode(piwikApiGetQuery + params, "UTF-8");
	}

	private String getDataFromLindatPiwikCacheServer() throws IOException {
		String period = request.getParameter("period");
		String url = PIWIK_API_URL_CACHED + "handle?h=" + item.getHandle() + "&period=" + period;

		if(request.getParameter("date")!=null) {
			String date = request.getParameter("date");
			url += "&date=" + date;
		}

		return PiwikHelper.readFromURL(url);
	}

	private static class DateRange {
		private static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

		private final Date startDate;
		private final Date endDate;

		private DateRange(Date startDate, Date endDate){
			this.startDate = startDate;
			this.endDate = endDate;
		}

		private static DateRange fromDateString(String date) throws ParseException {
			Date startDate;
			Date endDate;
			if(date!=null) {
				String sdate = date;
				String edate = date;
				if(sdate.length()==4) {
					sdate += "-01-01";
					edate += "-12-31";
				} else if(sdate.length()==7) {
					Calendar cal = Calendar.getInstance();
					cal.set(Calendar.YEAR, Integer.parseInt(sdate.substring(0,4)));
					cal.set(Calendar.MONTH, Integer.parseInt(sdate.substring(5,7))-1);
					cal.set(Calendar.DATE, cal.getActualMaximum(Calendar.DATE));
					sdate += "-01";
					edate = df.format(cal.getTime());
				}
				startDate = df.parse(sdate);
				endDate = df.parse(edate);
			} else {
				// default start and end data
				startDate = df.parse("2014-01-01");
				endDate = Calendar.getInstance().getTime();
			}
			return new DateRange(startDate, endDate);
		}

		public String getFormattedStartDate(){
			return df.format(startDate);
		}

		public String getFormattedEndDate(){
			return df.format(endDate);
		}

		@Override
		public String toString(){
			return getFormattedStartDate() + "," + getFormattedEndDate();
		}

	}

}



