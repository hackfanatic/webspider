package webspider.core.crawler;

import java.net.URL;
import java.util.Collection;

/**
<<<<<<< HEAD
 *
=======
 * An interface specified by the assignment requirements
>>>>>>> d69181dd8a58fc3b49cf6520248b9a434dfc0dcf
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
    public Collection<URL> getLocalIWUrls();
    /** it returns all the URLs belonging to other sites*/
    public Collection<URL> getExternalIWURLs();
    public Collection<URL> getNonParsableIWURLs();
    public Collection<URL> getDeadIWURLs();
    public Collection<URL> getDisallowedIWURLs();
    
    
}