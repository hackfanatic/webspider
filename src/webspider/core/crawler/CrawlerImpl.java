package webspider.core.crawler;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.net.*;
import java.io.*;

import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.html.parser.ParserDelegator;

import webspider.actions.SpiderActions;
import static webspider.Settings.*;

/**
 * That class implements a web crawler to map a site structure.
 * 
 * @author Zsolt Bitvai based on Jeff Heaton's Crawler
 * @version 1.0 Implement interface
 */
public class CrawlerImpl {

	/**
	 * Header variables
	 */
	public static final String USER_AGENT_FIELD = "User-Agent";
	public static final String USER_AGENT_VALUE = "BDM_crawler_University_of_Sheffield_COM4280/1.0";
	public static final String ACCEPT_LANGUAGE_FIELD = "Accept-Language";
	public static final String ACCEPT_LANGUAGE_VALUE = "en";
	public static final String CONTENT_TYPE_FIELD = "Content-Type";
	public static final String CONTENT_TYPE_VALUE = "application/xwww-form-urlencoded";

	/**
	 * Request properties for the crawler crawling the web
	 */
	private static final Map<String, String> REQUEST_PROPERTIES = new HashMap<String, String>();
	static {
		REQUEST_PROPERTIES.put(USER_AGENT_FIELD, USER_AGENT_VALUE);
		REQUEST_PROPERTIES.put(ACCEPT_LANGUAGE_FIELD, ACCEPT_LANGUAGE_VALUE);
		REQUEST_PROPERTIES.put(CONTENT_TYPE_FIELD, CONTENT_TYPE_VALUE);
	}

	/**
	 * Urls disallowed by robots.txt
	 */
	private Set<URL> robotDisallowedURLs;

	/**
	 * Base url this crawler operates on
	 */
	private URL base;

	/**
	 * Delay between fetching urls
	 */
	private long crawlDelay = 0;

	/**
	 * A collection of URLs that are waiting to be processed
	 */
	private BlockingQueue<URL> activeLinkQueue;

	private Links localLinks;

	private Links externalLinks;

	private Links deadLinks;

	private Links nonParsableLinks;

	private Links disallowedLinks;

	/**
	 * Contains local, external, dead, nonParsable and disallowed links
	 */
	private Collection<Links> allLinks;

	/**
	 * The status of the parser
	 */
	private volatile String status;

	/**
	 * The path to robots.txt
	 */
	private String robotsPath;

	/**
	 * The crawler thread
	 */
	private Thread processingThread;

	/**
	 * Is the crawler working at the moment?
	 */
	private volatile boolean running = false;


	/**
	 * Gui to notify about changes in the state of the crawler
	 */
	private SpiderActions actions;

	/**
	 * The constructor intitalizes the base url, the file paths and reads
	 * robots.txt
	 * 
	 * @param base
	 *            host of the site to crawl
	 * @param action
	 *            Gui to update
	 * 
	 */
	public CrawlerImpl(URL base, SpiderActions actions) {
		this.actions = actions;
		this.base = base;
		this.activeLinkQueue = new LinkedBlockingQueue<URL>();
		this.robotDisallowedURLs = new HashSet<URL>();
		this.localLinks = new Links(DEFAULT_PATH + base.getHost()
				+ "_localIWURLs" + CRAWLER_EXTENSION);
		this.externalLinks = new Links(DEFAULT_PATH + base.getHost()
				+ "_externalIWURLs" + CRAWLER_EXTENSION);
		this.deadLinks = new Links(DEFAULT_PATH + base.getHost()
				+ "_deadIWURLs" + CRAWLER_EXTENSION);
		this.nonParsableLinks = new Links(DEFAULT_PATH + base.getHost()
				+ "_nonparsableIWURLs" + CRAWLER_EXTENSION);
		this.disallowedLinks = new Links(DEFAULT_PATH + base.getHost()
				+ "_disallowedIWURLs" + CRAWLER_EXTENSION);

		getActiveLinkQueue().add(base);
		initAllLinks();
	}

	private void initAllLinks() {
		this.allLinks = new ArrayList<Links>();
		this.allLinks.add(this.localLinks);
		this.allLinks.add(this.externalLinks);
		this.allLinks.add(this.deadLinks);
		this.allLinks.add(this.nonParsableLinks);
		this.allLinks.add(this.disallowedLinks);

	}

	/**
	 * Set up disallowed urls from robots.txt
	 */
	private void readRobotsTxt() {
		try {
			log("Reading robots.txt");

			if (this.base.equals(new URL(DEFAULT_URL))) {
				this.robotsPath = DEFAULT_ROBOTS_TXT_URL;
			} else {
				this.robotsPath = this.base.getHost() + "/robots.txt";
			}

			URL robotURL = new URL(this.robotsPath);
			URLConnection robotConn = robotURL.openConnection();
			Scanner reader = new Scanner(robotConn.getInputStream());
			boolean userAgentMatched = false;

			final String USER_AGENT_ENTRY = "user-agent:";
			final String DISALLOW_ENTRY = "disallow:";
			final String CRAWL_DELAY_ENTRY = "crawl-delay:";

			while (reader.hasNextLine()) {
				String line = reader.nextLine();
				line = line.trim().toLowerCase();

				if (line.startsWith(USER_AGENT_ENTRY)) {

					String userAgentEntryValue = line.substring(
							USER_AGENT_ENTRY.length()).trim();
					if (userAgentEntryValue.equals(USER_AGENT_VALUE)
							|| userAgentEntryValue.equals("*")) {
						userAgentMatched = true;
					} else {
						userAgentMatched = false;
					}
				} else if (line.startsWith(DISALLOW_ENTRY)) {
					if (!userAgentMatched) {
						continue;
					}

					String disallowedEntryValue = line.substring(
							DISALLOW_ENTRY.length()).trim();
					if (disallowedEntryValue.endsWith("/")){
						disallowedEntryValue = disallowedEntryValue.substring(0,disallowedEntryValue.length()-1);
					}
					URL disallowedURL = new URL(this.base, disallowedEntryValue);
					this.robotDisallowedURLs.add(disallowedURL);

				} else if (line.startsWith(CRAWL_DELAY_ENTRY)) {

					String crawlDelayValue = line.substring(
							CRAWL_DELAY_ENTRY.length()).trim();
					this.crawlDelay = (long) (Double
							.parseDouble(crawlDelayValue) * 1000);
				}
			}
		} catch (MalformedURLException e) {
			log("robots.txt doesn't exist");
		} catch (IOException e) {
			log("robots.txt doesn't exist");
		} 
	}
	
	/**
	 * Get the URLs that were waiting to be processed. You should add one URL to
	 * this collection to begin the crawler.
	 * 
	 * @return A collection of URLs.
	 */
	public BlockingQueue<URL> getActiveLinkQueue() {
		return this.activeLinkQueue;
	}

	/**
	 * Add a URL for processing, if it hasn't been visited before
	 * 
	 * @param url
	 */
	public void addURL(URL url) {
		if (getActiveLinkQueue().contains(url)) {
			return;
		}
		for (Links links : this.allLinks) {
			if (links.contains(url)) {
				return;
			}
		}

		log("Adding to workload: " + url);
		getActiveLinkQueue().add(url);

	}

	/**
	 * Takes a url from the active queue and processes it, then prints to file
	 * in the end. Stops if paused.
	 */
	public synchronized void processActiveQueue() {
		while (!getActiveLinkQueue().isEmpty() && this.running) {

			URL currUrl = getActiveLinkQueue().poll();
			processURL(currUrl);
			try {
				Thread.sleep(getCrawlDelay());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
		// print to file, if ended normally
		if (this.running) {
			try {
				printToFile();

				log("Completed crawling");
				this.actions.getCrawlerActions().resetButtons();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		this.running = false;
		log("webCrawler stopped");
	}

	/**
	 * Called internally to process a URL
	 * 
	 * @param url
	 *            The URL to be processed.
	 */
	public void processURL(URL url) {
		log("Processing: " + url);
		try {

			if (!isLocal(url)) {
				log("External link - " + url);
				this.externalLinks.add(url);
				return;
			}
			if (!isRobotAllowed(url)) {
				log("Disallowed by robots.txt - " + url);
				this.disallowedLinks.add(url);
				return;
			}

			URLConnection connection = url.openConnection();
			setRequestProperties(connection);
			if (!isParseable(connection)) {
				log("Not parsable content type: " + connection.getContentType()
						+ " - " + url);
				this.nonParsableLinks.add(url);
				return;
			}

			// read the URL
			InputStream is = connection.getInputStream();
			Reader r = new InputStreamReader(is);
			// parse the URL
			ParserDelegator parser = new ParserDelegator();
			parser.parse(r, new Parser(url), true);

			// mark URL as complete
			this.localLinks.add(url);
			log("Complete: " + url);
		} catch (IOException e) {
			this.deadLinks.add(url);
			log("Error: " + url);
		}
	}

	/**
	 * Adds the Crawler's headers to the connection
	 * 
	 * @param connection
	 *            the connection to set the request properties for
	 */
	public void setRequestProperties(URLConnection connection) {
		for (String key : REQUEST_PROPERTIES.keySet()) {
			connection.setRequestProperty(key, REQUEST_PROPERTIES.get(key));
		}
	}

	/**
	 * Called to start the crawler
	 */
	public void start() {
		this.processingThread = new Thread(new Runnable() {
			@Override
			public void run() {
				processActiveQueue();
			}
		});
		this.processingThread.start();
		this.running = true;
		log("webCrawler started");

	}

	/**
	 * Pauses the crawler
	 */
	public void stop() {
		this.running = false;
	}

	/**
	 * 
	 * @return is the parser running?
	 */
	public boolean isRunning() {
		return (this.processingThread != null) && (this.running);
	}

	/**
	 * Checks that a given url connection is parsable. A connection is parasable
	 * if it has a content type MIME declaration, is of type text/* and is not
	 * javascript or css.
	 * 
	 * @param connection
	 *            the connection to check
	 * @return is it parsable?
	 */
	public boolean isParseable(URLConnection connection) {
		String contentType = connection.getContentType();
		if (contentType == null) {
			return true;
		}
		contentType = contentType.toLowerCase();
		if (contentType.startsWith("text/javascript")
				|| contentType.startsWith("text/css")) {
			return false;
		}

		return contentType.startsWith("text/");
	}

	/**
	 * Checks that a url is local
	 * 
	 * @param url
	 *            the url to check
	 * @return is it local?
	 */
	public boolean isLocal(URL url) {
		return url.getHost().equalsIgnoreCase(this.base.getHost());
	}

	/**
	 * Checks that a url is allowed by robots.txt
	 * 
	 * @param checkURL
	 *            the url to check
	 * @return is the url allowed?
	 */
	public boolean isRobotAllowed(URL checkURL) {
		if (!isRobotsTxtRead()) {
			readRobotsTxt();
		}
		for (URL disallowedUrl : this.robotDisallowedURLs) {
			if (checkURL.getPath().startsWith(disallowedUrl.getPath())) {
				return false;
			}
		}
		return true;
	}
	
	public boolean isRobotsTxtRead(){
		return this.robotsPath != null;
	}

	/**
	 * Called internally to log information. It notifies the user intreface
	 * about changes in the crawler's state
	 * 
	 * @param entry
	 *            The information to be written to the log.
	 */
	public void log(String entry) {
		this.actions.log(entry);
		setStatus(entry);
		this.actions.getCrawlerActions().updateStats();
		// System.out.println(entry);
	}

	/**
	 * Prints all urls to separate files
	 * 
	 * @throws FileNotFoundException
	 */
	public void printToFile() throws FileNotFoundException {
		for (Links links : this.allLinks) {
			links.print();
		}
	}

	/**
	 * @return the localLinks
	 */
	public Links getLocalLinks() {
		return this.localLinks;
	}

	/**
	 * @return the externalLinks
	 */
	public Links getExternalLinks() {
		return this.externalLinks;
	}

	/**
	 * @return the deadLinks
	 */
	public Links getDeadLinks() {
		return this.deadLinks;
	}

	/**
	 * @return the nonParsableLinks
	 */
	public Links getNonParsableLinks() {
		return this.nonParsableLinks;
	}

	/**
	 * @return the disallowedLinks
	 */
	public Links getDisallowedLinks() {
		return this.disallowedLinks;
	}

	/**
	 * @return the status
	 */
	public String getStatus() {
		return this.status;
	}

	/**
	 * @param status
	 *            the status to set
	 */
	private void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Gets all the urls disallowed by robots.txt
	 * 
	 * @return
	 */
	public Set<URL> getRobotDisallowedURLs() {
		return this.robotDisallowedURLs;
	}

	/**
	 * Return the crawl delay between parsing urls
	 * @return
	 */
	public long getCrawlDelay(){
		if (!isRobotsTxtRead()){
			readRobotsTxt();
		}
		return this.crawlDelay;
	}
	/**
	 * A HTML parser callback used by this class to detect links
	 * 
	 */
	public class Parser extends HTMLEditorKit.ParserCallback {
		/**
		 * The url addres to parse
		 */
		private URL parserBase;

		/**
		 * Creates a new HTMLEditorKit.ParserCallback
		 * 
		 * @param base
		 *            te link to parse
		 */
		public Parser(URL base) {
			this.parserBase = base;
		}

		/**
		 * Handles a simple html tag. A link if found if the tag has a href or
		 * src attribute. #'s are checked in the link and subsequent characters
		 * are removed. mailto links are ignored.
		 */
		@Override
		public void handleSimpleTag(HTML.Tag tag,
				MutableAttributeSet attributes, int pos) {
			String href = (String) attributes.getAttribute(HTML.Attribute.HREF);

			if (href == null)
				href = (String) attributes.getAttribute(HTML.Attribute.SRC);

			if (href == null)
				return;

			int i = href.indexOf('#');
			if (i != -1)
				href = href.substring(0, i);

			if (href.toLowerCase().startsWith("mailto:")) {
				return;
			}

			handleLink(href);
		}

		/**
		 * Handles the beginning of a tag the same way. Links are stored in a
		 * Set so they are not added twice
		 */
		@Override
		public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
			handleSimpleTag(t, a, pos); // handle the same way
		}

		/**
		 * Checks that a link is valid and adds it to the processing queue.
		 * 
		 * @param link
		 */
		protected void handleLink(String link) {
			try {
				URL url = new URL(this.parserBase, link);
				if (!url.equals(this.parserBase)){
					addURL(url);
				}
			} catch (MalformedURLException e) {
				log("Found malformed URL: " + link);
			}
		}
	}
}