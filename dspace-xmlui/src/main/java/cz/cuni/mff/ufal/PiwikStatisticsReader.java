package cz.cuni.mff.ufal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

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
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
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
    
    
    /** Piwik configurations */
    private static final String PIWIK_API_MODE = ConfigurationManager.getProperty("lr", "lr.statistics.api.mode");
    private static final String PIWIK_API_URL = ConfigurationManager.getProperty("lr", "lr.statistics.api.url");
    private static final String PIWIK_API_URL_CACHED = ConfigurationManager.getProperty("lr", "lr.statistics.api.cached.url");
    private static final String PIWIK_AUTH_TOKEN = ConfigurationManager.getProperty("lr", "lr.statistics.api.auth.token");
    private static final String PIWIK_SITE_ID = ConfigurationManager.getProperty("lr", "lr.statistics.api.site_id");
	private static final String PIWIK_DOWNLOAD_SITE_ID = ConfigurationManager.getProperty("lr", "lr.tracker.bitstream.site_id");	
	//private static final int PIWIK_SHOW_LAST_N_DAYS = ConfigurationManager.getIntProperty("lr", "lr.statistics.show_last_n", 7);

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
            DSpaceObject dso = dso = HandleManager.resolveToObject(context, handle);

            if (dso instanceof Item) {
                item = (Item)dso;                
            } else {
            	throw new ResourceNotFoundException("Unable to locate item");
            }

            EPerson eperson = context.getCurrentUser();
            
            if(eperson == null) {
            	throw new AuthorizeException();
            }
            
            /*if(!(AuthorizeManager.isAdmin(context) || item.getSubmitter().getID()==eperson.getID())) {
            	throw new AuthorizeException();
            }*/
            
        } catch (AuthorizeException | SQLException | IllegalStateException e) {
            throw new ProcessingException("Unable to read piwik statistics", e);
        }

    }


	@Override
	public void generate() throws IOException, SAXException, ProcessingException {
		
		try {
			
			
			String mergedResult = "";
			
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");			
			
			// should contain the period
			String queryString = request.getQueryString();									
			String period = request.getParameter("period");
			
			
			if(PIWIK_API_MODE==null || PIWIK_API_MODE.equals("direct")) {
				
				Calendar cal = Calendar.getInstance();
				// default start and end data
				Date startDate = df.parse("2014-01-01");
				Date endDate = cal.getTime();
							
				if(request.getParameter("date")!=null) {
					String sdate = request.getParameter("date");
					String edate = request.getParameter("date");
					if(sdate.length()==4) {
						sdate += "-01-01";
						edate += "-12-31";
					} else 
					if(sdate.length()==7) {	
						cal.set(Calendar.YEAR, Integer.parseInt(sdate.substring(0,4)));
						cal.set(Calendar.MONTH, Integer.parseInt(sdate.substring(5,7))-1);
						cal.set(Calendar.DATE, cal.getActualMaximum(Calendar.DATE));
						sdate += "-01";
						edate = df.format(cal.getTime());
					}
					startDate = df.parse(sdate);
					endDate = df.parse(edate);				
				}
							
				String dspaceURL = ConfigurationManager.getProperty("dspace.url");
	
				String urlParams =
					  "&date=" + df.format(startDate) + "," + df.format(endDate)
					+ "&period=" + period
					+ "&idSite=" + PIWIK_SITE_ID
					+ "&token_auth=" + PIWIK_AUTH_TOKEN
					+ "&segment=pageUrl=@" + dspaceURL + "/handle/" + item.getHandle()
					+ "&showColumns=label,url,nb_visits,nb_hits";
				String downloadUrlParams =
					  "&date=" + df.format(startDate) + "," + df.format(endDate)
					+ "&period=" + period
					+ "&idSite=" + PIWIK_DOWNLOAD_SITE_ID
					+ "&token_auth=" + PIWIK_AUTH_TOKEN
					+ "&segment=pageUrl=@" + dspaceURL + "/bitstream/handle/" + item.getHandle()
					+ "&showColumns=label,url,nb_visits,nb_hits";
	
	
				final boolean multi_requests = false;
				queryString += "&token_auth=" + PIWIK_AUTH_TOKEN + "&module=API";
				String piwikApiGetQuery = "method=Actions.getPageUrls&expanded=1&flat=1";
	
				if ( multi_requests ) {
	
					String report = PiwikHelper.readFromURL(
						PIWIK_API_URL + rest + "?" + queryString + "&" + piwikApiGetQuery + urlParams
					);
					String downloadReport = PiwikHelper.readFromURL(
						PIWIK_API_URL + rest + "?" + queryString + "&" + piwikApiGetQuery + downloadUrlParams
					);
					mergedResult = PiwikHelper.mergeJSON(report, downloadReport);
				}else {
					String piwikBulkApiGetQuery = "module=API&method=API.getBulkRequest&format=JSON"
							+ "&token_auth=" + PIWIK_AUTH_TOKEN;
	
					
					String url0 = URLEncoder.encode(piwikApiGetQuery + urlParams, "UTF-8");
					String url1 = URLEncoder.encode(piwikApiGetQuery + downloadUrlParams, "UTF-8");
					String report = PiwikHelper.readFromURL(
						PIWIK_API_URL + rest + "?"
							+ piwikBulkApiGetQuery
							+ "&urls[0]=" + url0
							+ "&urls[1]=" + url1
					);
					//mergedResult = PiwikHelper.mergeJSONResults(report);
					mergedResult = PiwikHelper.transformJSONResults(report);
					//mergedResult = report;
	
				}
			} else 
			if(PIWIK_API_MODE.equals("cached")) {
				
				String url = PIWIK_API_URL_CACHED + "handle?h=" + item.getHandle() + "&period=" + period;
				
				if(request.getParameter("date")!=null) {
					String date = request.getParameter("date");
					url += "&date=" + date;
				}
				
				mergedResult = PiwikHelper.readFromURL(url);
				
			}
			
			out.write(mergedResult.getBytes());
			out.flush();
			
		} catch (Exception e) {
			throw new ProcessingException("Unable to read piwik statisitcs", e);
		}
	}	
	
}



