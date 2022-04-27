package cz.cuni.mff.ufal;

import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;

import org.apache.tools.ant.filters.StringInputStream;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class PiwikHelper {
	private static org.apache.log4j.Logger log = Logger.getLogger(PiwikHelper.class);

	private String dspaceURL = ConfigurationManager.getProperty("dspace.url");
	/** Piwik configurations */
	private static final String PIWIK_API_MODE = ConfigurationManager.getProperty("lr", "lr.statistics.api.mode");
	private static final String PIWIK_API_URL = ConfigurationManager.getProperty("lr", "lr.statistics.api.url");
	private static final String PIWIK_API_URL_CACHED = ConfigurationManager.getProperty("lr", "lr.statistics.api.cached.url");
	private static final String PIWIK_AUTH_TOKEN = ConfigurationManager.getProperty("lr", "lr.statistics.api.auth.token");
	private static final String PIWIK_SITE_ID = ConfigurationManager.getProperty("lr", "lr.statistics.api.site_id");
	private static final String PIWIK_DOWNLOAD_SITE_ID = ConfigurationManager.getProperty("lr", "lr.tracker.bitstream.site_id");

	/**
	 *
	 * @param keys - The order of keys should match the json object indexes
	 * @param report - The downloaded json
	 * @return
	 * @throws Exception
	 */
	public static String transformJSONResults(Set<String> keys, String report) throws Exception {
		JSONParser parser = new JSONParser();
		JSONArray json = (JSONArray)parser.parse(report);
		JSONObject views = null;
		JSONObject downloads = null;
		int i = 0;
		for(String key: keys){
			if(key.toLowerCase().contains("itemview")){
				views = mergeJSONReports(views, (JSONObject)json.get(i));
			}else if(key.toLowerCase().contains("downloads")){
				downloads = mergeJSONReports(downloads, (JSONObject)json.get(i));
			}
			i++;
		}
		JSONObject response = new JSONObject();
		JSONObject result = new JSONObject();		
		response.put("response", result);		
		
		result.put("views", transformJSON(views));
		result.put("downloads", transformJSON(downloads));
		
		return response.toJSONString().replace("\\/", "/");
	}

	@SuppressWarnings("unchecked")
	private static JSONObject mergeJSONReports(JSONObject o1, JSONObject o2) {
		if (o1 == null) {
			return o2;
		} else {
		    //just concatenate the dmy arrays, transformJSON should do the rest
			Set<String> keys = o1.keySet();
			for (String dmyKey : keys) {
				if(o2.containsKey(dmyKey)){
					JSONArray a = (JSONArray)o1.get(dmyKey);
					a.addAll((JSONArray)o2.get(dmyKey));
				}
			}
			keys = o2.keySet();
			for (String dmyKey : keys) {
			    if(!o1.containsKey(dmyKey)){
			        o1.put(dmyKey, o2.get(dmyKey));
				}
			}
			return o1;
		}
	}
	
	
	public static JSONObject transformJSON(JSONObject views) {
		JSONObject result = new JSONObject();
		JSONObject total = new JSONObject();		
		for(Object key : views.keySet()) {
			JSONArray view_data = (JSONArray) views.get(key);
			
			if(view_data.size()==0) continue;
			
			String dmy[] = key.toString().split("-");
			if(dmy.length==1) {
				int y = Integer.parseInt(dmy[0]);
				JSONObject year = null;
				if(!result.containsKey(y)) {
					year = new JSONObject();
					result.put(y, year);
				} else {
					year = (JSONObject)result.get(y);
				}
				for (int i = 0 ; i < view_data.size(); i++) {
					JSONObject row = (JSONObject)view_data.get(i);
					String url = row.get("label").toString();
					url = url.split("\\?|@")[0];					
					JSONObject v = null;
					int nb_visits = 0;
					int nb_hits = 0;
					if(year.containsKey(url)) {
						v = (JSONObject)year.get(url);						
						nb_visits = Integer.parseInt(v.get("nb_visits").toString());
						nb_hits = Integer.parseInt(v.get("nb_hits").toString());
					}
					v = new JSONObject();	
					nb_visits = nb_visits + Integer.parseInt(row.get("nb_visits").toString());
					nb_hits = nb_hits + Integer.parseInt(row.get("nb_hits").toString());
					v.put("nb_hits", nb_hits);
					v.put("nb_visits", nb_visits);
					year.put(url, v);
					
					int total_nb_visits = 0;
					int total_nb_hits = 0;					
					
					if(total.containsKey(y)) {
						v = (JSONObject)total.get(y);						
						total_nb_visits = Integer.parseInt(v.get("nb_visits").toString());
						total_nb_hits = Integer.parseInt(v.get("nb_hits").toString());
					}
					v = new JSONObject();	
					total_nb_visits = total_nb_visits + Integer.parseInt(row.get("nb_visits").toString());
					total_nb_hits = total_nb_hits + Integer.parseInt(row.get("nb_hits").toString());
					v.put("nb_hits", total_nb_hits);
					v.put("nb_visits", total_nb_visits);
					total.put(y, v);
					
					total_nb_visits = 0;
					total_nb_hits = 0;					
					
					if(total.containsKey("nb_visits")) {
						total_nb_visits = Integer.parseInt(total.get("nb_visits").toString());
					}
					
					if(total.containsKey("nb_hits")) {
						total_nb_hits = Integer.parseInt(total.get("nb_hits").toString());						
					}
					total_nb_visits = total_nb_visits + Integer.parseInt(row.get("nb_visits").toString());
					total_nb_hits = total_nb_hits + Integer.parseInt(row.get("nb_hits").toString());					
					
					total.put("nb_hits", total_nb_hits);
					total.put("nb_visits", total_nb_visits);
					
				}
			} else if(dmy.length==2) {
				JSONObject year = null;
				int y = Integer.parseInt(dmy[0]);
				if(!result.containsKey(y)) {
					year = new JSONObject();
					result.put(y, year);
				} else {
					year = (JSONObject)result.get(y);
				}
				int m = Integer.parseInt(dmy[1]);
				JSONObject month = null;
				if(!year.containsKey(m)) {
					month = new JSONObject();
					year.put(m, month);
				} else {
					month = (JSONObject)year.get(m);
				}
				for (int i = 0 ; i < view_data.size(); i++) {
					JSONObject row = (JSONObject)view_data.get(i);
					String url = row.get("label").toString();
					url = url.split("\\?|@")[0];					
					JSONObject v = null;
					int nb_visits = 0;
					int nb_hits = 0;
					if(month.containsKey(url)) {
						v = (JSONObject)month.get(url);						
						nb_visits = Integer.parseInt(v.get("nb_visits").toString());
						nb_hits = Integer.parseInt(v.get("nb_hits").toString());
					}
					v = new JSONObject();	
					nb_visits = nb_visits + Integer.parseInt(row.get("nb_visits").toString());
					nb_hits = nb_hits + Integer.parseInt(row.get("nb_hits").toString());
					v.put("nb_hits", nb_hits);
					v.put("nb_visits", nb_visits);
					month.put(url, v);
					
					JSONObject tyear = null;
					
					if(total.containsKey(y)) {
						tyear = (JSONObject)total.get(y);						
					} else {
						tyear = new JSONObject();
						total.put(y, tyear);
					}
					
					JSONObject tmonth = null;
								
					if(tyear.containsKey(m)) {
						tmonth = (JSONObject)tyear.get(m);
					} else {
						tmonth = new JSONObject();
						tyear.put(m, tmonth);
					}
					
					int total_nb_visits = 0;
					int total_nb_hits = 0;			
					
					if(tmonth.containsKey("nb_visits")) {
						total_nb_visits = Integer.parseInt(tmonth.get("nb_visits").toString());
					}
					if(tmonth.containsKey("nb_hits")) {
						total_nb_hits = Integer.parseInt(tmonth.get("nb_hits").toString());
					}

					v = new JSONObject();	
					total_nb_visits = total_nb_visits + Integer.parseInt(row.get("nb_visits").toString());
					total_nb_hits = total_nb_hits + Integer.parseInt(row.get("nb_hits").toString());
					v.put("nb_hits", total_nb_hits);
					v.put("nb_visits", total_nb_visits);
					tyear.put(m, v);					
					
				}				
			} else if(dmy.length==3) {
				JSONObject year = null;
				int y = Integer.parseInt(dmy[0]);
				if(!result.containsKey(y)) {
					year = new JSONObject();
					result.put(y, year);
				} else {
					year = (JSONObject)result.get(y);
				}
				int m = Integer.parseInt(dmy[1]);
				JSONObject month = null;
				if(!year.containsKey(m)) {
					month = new JSONObject();
					year.put(m, month);
				} else {
					month = (JSONObject)year.get(m);
				}
				int d = Integer.parseInt(dmy[2]);
				JSONObject day = null;
				if(!month.containsKey(d)) {
					day = new JSONObject();
					month.put(d, day);
				} else {
					day = (JSONObject)month.get(d);
				}
	
				for (int i = 0 ; i < view_data.size(); i++) {
					JSONObject row = (JSONObject)view_data.get(i);
					String url = row.get("label").toString();
					url = url.split("\\?|@")[0];					
					JSONObject v = null;
					int nb_visits = 0;
					int nb_hits = 0;
					if(day.containsKey(url)) {
						v = (JSONObject)day.get(url);						
						nb_visits = Integer.parseInt(v.get("nb_visits").toString());
						nb_hits = Integer.parseInt(v.get("nb_hits").toString());
					}
					v = new JSONObject();	
					nb_visits = nb_visits + Integer.parseInt(row.get("nb_visits").toString());
					nb_hits = nb_hits + Integer.parseInt(row.get("nb_hits").toString());
					v.put("nb_hits", nb_hits);
					v.put("nb_visits", nb_visits);
					day.put(url, v);
					JSONObject tyear = null;
					
					if(total.containsKey(y)) {
						tyear = (JSONObject)total.get(y);						
					} else {
						tyear = new JSONObject();
						total.put(y, tyear);
					}
					
					JSONObject tmonth = null;
								
					if(tyear.containsKey(m)) {
						tmonth = (JSONObject)tyear.get(m);
					} else {
						tmonth = new JSONObject();
						tyear.put(m, tmonth);
					}

					JSONObject tday = null;
					
					if(tmonth.containsKey(d)) {
						tday = (JSONObject)tmonth.get(d);
					} else {
						tday = new JSONObject();
						tmonth.put(d, tday);
					}
					
					int total_nb_visits = 0;
					int total_nb_hits = 0;			
					
					if(tday.containsKey("nb_visits")) {
						total_nb_visits = Integer.parseInt(tday.get("nb_visits").toString());
					}
					if(tday.containsKey("nb_hits")) {
						total_nb_hits = Integer.parseInt(tday.get("nb_hits").toString());
					}
				
					v = new JSONObject();	
					total_nb_visits = total_nb_visits + Integer.parseInt(row.get("nb_visits").toString());
					total_nb_hits = total_nb_hits + Integer.parseInt(row.get("nb_hits").toString());
					v.put("nb_hits", total_nb_hits);
					v.put("nb_visits", total_nb_visits);
					tmonth.put(d, v);					
				}

			}
			result.put("total", total);			
		}			
		return result;
	}
	

	public static String readFromURL(String url) throws IOException {
		StringBuilder output = new StringBuilder();		
		URL widget = new URL(url);
        String old_value = "false";
        try{
            old_value = System.getProperty("jsse.enableSNIExtension");
            System.setProperty("jsse.enableSNIExtension", "false");

            long fetchingStart = 0L;
            if(log.isDebugEnabled()){
                fetchingStart = System.currentTimeMillis();
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(widget.openStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                output.append(inputLine).append("\n");
            }
            in.close();
            if(log.isDebugEnabled()) {
                long fetchingEnd = System.currentTimeMillis();
                log.debug(String.format("PiwikHelper fetching took %s", fetchingEnd - fetchingStart));
            }
        }finally {
        	//true is the default http://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html
        	old_value = (old_value == null) ? "true" : old_value;
            System.setProperty("jsse.enableSNIExtension", old_value);
        }
		return output.toString();
	}

	String period;
	String date;
	Item item;
	String rest;
	public PiwikHelper(String period, String date, Item item, String rest){
		this.period = period;
		this.date = date;
		this.item = item;
		this.rest = rest;
	}

	public String getDataAsJsonString() throws Exception {
		String mergedResult;
		if(PIWIK_API_MODE.equals("cached")) {
			log.debug("========CACHED MODE");
			mergedResult = getDataFromLindatPiwikCacheServer();
		} else {
			// direct mode as default
			log.debug("========DIRECT MODE");
			mergedResult = getDataFromPiwikServer();
		}
		return mergedResult;
	}

	public List<String[]> getCountryData() throws Exception{
		try {
			if (PIWIK_API_MODE.equals("cached")) {
				log.debug("========CACHED MODE");
				return getCountryDataFromLindatPiwikCacheServer();
			} else {
				// direct mode as default
				log.debug("========DIRECT MODE");
				return getCountryDataFromPiwik();
			}
		} catch (FileNotFoundException e){
			log.info(String.format("No country data for '%s'", e.getMessage()));
			return new ArrayList<>();
		}
	}

	private List<String[]> getCountryDataFromLindatPiwikCacheServer() throws Exception {
		String url = PIWIK_API_URL_CACHED + "handle?h=" + item.getHandle() + "&period=month&country=true";

		if(date!=null) {
			url += "&date=" + date;
		}
		JSONParser parser = new JSONParser();
		JSONObject countriesReport = (JSONObject)parser.parse(PiwikHelper.readFromURL(url));

		List<String[]> result = new ArrayList<>(10);

		for (Object key : countriesReport.keySet() ) {
			if(key instanceof String){
				String date = (String) key;
				JSONArray countryData = (JSONArray) countriesReport.get(date);
				for(Object country: countryData){
					if(country instanceof JSONObject){
						JSONObject c = (JSONObject) country;
						String label = (String) c.get("label");
						String count = ((Long) c.get("nb_visits")).toString();
						result.add(new String[]{label, count});
					}
				}
				// expecting only one date key
				break;
			}
		}
		return result;
	}

	private List<String[]> getCountryDataFromPiwik() throws Exception {

		String countryReportURL = PIWIK_API_URL + "index.php"
				+ "?module=API"
				+ "&method=UserCountry.getCountry"
				+ "&idSite=" + PIWIK_SITE_ID
				+ "&period=month"
				+ "&date=" + date
				+ "&expanded=1"
				+ "&token_auth=" + PIWIK_AUTH_TOKEN
				+ "&filter_limit=10"
				+ "&format=xml"
				+ "&segment=pageUrl=@" + URLEncoder.encode(dspaceURL + "/handle/" + item.getHandle(), "UTF-8");


		String xml = PiwikHelper.readFromURL(countryReportURL);

		Document doc = parseXML(xml);

		if(doc==null) throw new Exception("Unable to parse XML");

		List<String[]> data = new ArrayList<String[]>();

		XPath xPath =  XPathFactory.newInstance().newXPath();
		XPathExpression eachResultNode = xPath.compile("//result/row");

		NodeList results = (NodeList)eachResultNode.evaluate(doc, XPathConstants.NODESET);

		for(int i=0;i<results.getLength();i++) {
			Element row = (Element)results.item(i);
			String country = row.getElementsByTagName("label").item(0).getTextContent();
			String count = row.getElementsByTagName("nb_visits").item(0).getTextContent();
			data.add(new String[]{country, count});
		}

		return data;

	}

	private static Document parseXML(String xml) {
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		Document doc = null;
		try {
			builder = builderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		try {
			doc = builder.parse(new StringInputStream(xml));
		} catch (Exception e) {
			log.error(e);
		}
		return doc;
	}


	private String getDataFromLindatPiwikCacheServer() throws IOException {
		String url = PIWIK_API_URL_CACHED + "handle?h=" + item.getHandle() + "&period=" + period;

		if(date!=null) {
			url += "&date=" + date;
		}

		return PiwikHelper.readFromURL(url);
	}

	private String getDataFromPiwikServer() throws Exception {
		SortedMap<String, String> urls = buildViewsURL();
		urls.put("downloads", buildDownloadsURL());
		String bulkApiGetRequestURL = buildBulkApiGetRequestURL(urls);

		log.debug(String.format("Fetching data from piwik server; requesting \"%s\"", bulkApiGetRequestURL));

		String report = PiwikHelper.readFromURL(bulkApiGetRequestURL);
		return PiwikHelper.transformJSONResults(urls.keySet(), report);
	}
	private String buildBulkApiGetRequestURL(SortedMap<String, String> urls){
		String piwikBulkApiGetQuery = "module=API&method=API.getBulkRequest&format=JSON"
				+ "&token_auth=" + PIWIK_AUTH_TOKEN;
		StringBuilder sb = new StringBuilder();
		sb.append(PIWIK_API_URL)
				.append(rest)
				.append("?")
				.append(piwikBulkApiGetQuery);
		int i = 0;
		for(String url : urls.values()){
			sb.append("&urls[")
					.append(i++)
					.append("]=")
					.append(url);
		}
		return sb.toString();
	}

	private SortedMap<String, String> buildViewsURL() throws UnsupportedEncodingException, ParseException {
		// use Actions.getPageUrl; call it twice; once with ?show=full
		String paramsFmt = "method=Actions.getPageUrl&pageUrl=%s";
		String summaryItemView = buildURL(PIWIK_SITE_ID,
				String.format(paramsFmt, URLEncoder.encode(dspaceURL + "/handle/" + item.getHandle(), "UTF-8")));
		String fullItemView = buildURL(PIWIK_SITE_ID,
				String.format(paramsFmt, URLEncoder.encode(
						dspaceURL + "/handle" + "/" + item.getHandle() + "?show=full", "UTF-8")));
		SortedMap<String, String> ret = new TreeMap<>();
		ret.put("summaryItemView", summaryItemView);
		ret.put("fullItemView", fullItemView);
		return ret;
	}

	private String buildDownloadsURL() throws UnsupportedEncodingException, ParseException {
		String filterPattern =  URLEncoder.encode(dspaceURL + "/bitstream/handle/" + item.getHandle(), "UTF-8");
		String params =
				"method=Actions.getPageUrls" +
						"&expanded=1&flat=1" +
						"&filter_column=url" +
						"&filter_pattern=" + filterPattern;
		return buildURL(PIWIK_DOWNLOAD_SITE_ID, params);
	}

	private String buildURL(String siteID, String specificParams) throws UnsupportedEncodingException,
			ParseException {
		String dateRange = DateRange.fromDateString(date).toString();

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
		String params =
				specificParams
						+ "&date=" + dateRange
						+ "&period=" + period
						+ "&token_auth=" + PIWIK_AUTH_TOKEN
						+ "&showColumns=label,url,nb_visits,nb_hits"
						// don't want to handle "paging" (summary views)
						+ "&filter_limit=-1"
						+ "&idSite=" + siteID;
		return URLEncoder.encode(params, "UTF-8");
	}

	private static class DateRange {
		private static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		private static final SimpleDateFormat odf = new SimpleDateFormat("yyyy-MM");

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
				} else if(sdate.length()==6 || sdate.length()==7) {
					Calendar cal = Calendar.getInstance();
					cal.set(Calendar.YEAR, Integer.parseInt(sdate.substring(0,4)));
					cal.set(Calendar.MONTH, Integer.parseInt(sdate.substring(5))-1);
					cal.set(Calendar.DATE, cal.getActualMaximum(Calendar.DATE));
					sdate = odf.format(cal.getTime()) + "-01";
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
