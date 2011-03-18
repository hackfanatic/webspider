package webspider.core.crawler;

/**
 *
 */
public interface myIWSpider {
    public void openUserInterface();
    public void closeUserInterface();
    public void startIWSpider(final String mySeed);
    public boolean isIWRobotSafe(final String myUrl);
    public void stopIWSpider ();
    public void resumeIWSpider ();
    public void killIWSpider ();
    /** it returns all the URLs internal to the site */
    public String[] getLocalIWUrls();
    /** it returns all the URLs belonging to other sites*/
    public String[] getExternalIWURLs();
}