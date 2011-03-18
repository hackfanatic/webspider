package webspider.core.indexer;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import webspider.Settings;
import webspider.actions.SpiderActions;
import webspider.gui.IndexerPanel;

/**
 *
 * @author esh
 */
public class Indexer implements myIWSearchEngine{

    private IndexerImpl indexer;
    SpiderActions actions;

    /*
     * Constructor
     * @param actions instance of SpiderActions class
     */
    public Indexer(SpiderActions actions){
        this.actions = actions;
    }

    /*
     * Calls the start indexing function
     * @param inputFileName input file name
     * @param outputFileName output file name
     */
    public void IndexCrawledPages(String inputFileName, String outputFileName)
    {
        this.indexer = new IndexerImpl(inputFileName, outputFileName, actions);
        this.indexer.startIndexing();
    }

    /*
     * Calls the startLoadIndex function which loads the index from a file in
     * a new thread
     * @param filename name of file from which index is loaded
     * @return Map Map containing the index table
     */

    public Map loadIndexTable(String fileName)
    {
        Map<String,Set<URL>> indexMap = this.indexer.loadIndexTable(fileName);
        log("Index loaded into memory from file");
        return indexMap;
    }


    /*
     * Calls the start search function
     * @param keyword keyword on which search is run
     * @return Set<URL> set of urls containing the keyword
     */
    public Set<URL> search(String keyword)
    {
        Set<URL> results = this.indexer.startSearch(keyword);
        printSearchResults(results);
        return results;
    }

    public void openUserInterface() {
        Settings.BACK_BUTTON = false;
        actions.setPanel(new IndexerPanel(actions));
        actions.openInterface();
    }

    public void closeUserInterface() {
        actions.closeInterface();
    }

    /*
     * Prints the search results to the screen
     * @param search Set of URLs containing a keyword
     */
    private void printSearchResults(Set<URL> search)
    {
        Iterator seIt = search.iterator();
        if(seIt.hasNext())
        {
            log("Printing search results");
        }else
        {
            log("No search results found");
        }

        while(seIt.hasNext())
        {
            log((String)seIt.next());
        }
    }

    /*
     * Logs text to the screen
     * @param text text to be logged to the screen
     */

    public void log(String text)
    {
        actions.log(text);
    }

    /*
     * Kills the indexer
     */
    public void killIndexer()
    {
        this.indexer = null;
    }

    /*
     * Stops the indexer
     */
    public void stopInxer()
    {
        this.indexer.setIndexerRunning(false);
    }

    /*
     * Resumes the indexer
     */
    public void resume() throws IOException
    {
        this.indexer.setIndexerRunning(true);
        this.indexer.resume();
    }

}
