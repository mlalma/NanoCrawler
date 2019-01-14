/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Based on crawler4j project by Yasser Ganjisaffar
 */
package com.nanocrawler.core;

import com.nanocrawler.contentparser.Parser;
import com.nanocrawler.data.Content;
import com.nanocrawler.data.CustomFetchStatus;
import com.nanocrawler.data.HtmlContent;
import com.nanocrawler.data.Page;
import com.nanocrawler.data.PageFetchResult;
import com.nanocrawler.dbs.DocIDServer;
import com.nanocrawler.dbs.Frontier;
import com.nanocrawler.fetcher.PageFetcher;
import com.nanocrawler.robotstxt.RobotstxtServer;
import com.nanocrawler.urlmanipulation.WebURL;
import com.nanocrawler.util.CrawlConfig;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;

// Web crawler instance - implements the crawling logic
public class WebCrawler implements Runnable {

    protected static final Logger logger = Logger.getLogger(WebCrawler.class.getName());

    // Unique id for the crawler
    protected int id;

    // The thread in which the crawler is running
    private Thread myThread = null;

    // Parser for parsing the crawled pages
    private Parser parser;

    // Fetcher fetches every single page
    private PageFetcher pageFetcher;

    // Robots.txt parser for determining the rules per site
    private RobotstxtServer robotstxtServer;

    // Server to map crawled pages to unique IDs
    private DocIDServer docIdServer;

    // Crawl queue manager
    private Frontier frontier;

    // Is crawler working or are all the crawl queue items managed
    private boolean isWaitingForNewURLs;
    
    // Configuration for the crawling process
    private CrawlConfig config;
    
    // Constructor
    public WebCrawler(CrawlConfig config) {    
        this.config = config;
    }

    // Initializes thread
    public void init(int id, CrawlController crawlController, CrawlConfig config) {
        this.id = id;
        this.pageFetcher = crawlController.getPageFetcher();
        this.robotstxtServer = crawlController.getRobotstxtServer();
        this.docIdServer = crawlController.getDocIdServer();
        this.frontier = crawlController.getFrontier();
        this.parser = new Parser(config);
        this.isWaitingForNewURLs = false;        
        this.myThread = new Thread(this, "Crawler" + this.id);        
    }

    // Returns ID of the crawler
    public int getId() { return id; }
    
    // Getter for the thread
    public Thread getThread() { return myThread; }
    
    // Returns status
    public boolean isNotWaitingForNewURLs() { return !isWaitingForNewURLs; }
    
    // Called just before the crawling starts
    public void onStart() {
        // Do nothing by default
        // Sub-classed can override this to add their custom functionality
    }

    // Called at the end of the crawling
    public void onBeforeExit() {
        // Do nothing by default
        // Sub-classed can override this to add their custom functionality
    }

    // Called once the page header has been fetched
    protected void handlePageStatusCode(WebURL webUrl, int statusCode, String statusDescription) {
        // Do nothing by default
        // Sub-classed can override this to add their custom functionality
    }

    // Notifies content fetching errors
    protected void onContentFetchError(WebURL webUrl) {
        // Do nothing by default
        // Sub-classed can override this to add their custom functionality
    }

    // Notifies content parsing errors
    protected void onParseError(WebURL webUrl) {
        // Do nothing by default
        // Sub-classed can override this to add their custom functionality
    }
    
    // Can be used to set per-URL priority
    protected byte URLPriority(WebURL webUrl) {
        return 0;
    }

    // Derived classes can tell the crawler whether the given url should be crawled or not, by default all are OK
    public boolean shouldVisit(WebURL url) {
        return true;
    }

    // Called for each page succesfully fetched & parsed
    public void visit(Page page) {
        // Do nothing by default
        // Sub-classed can override this to add their custom functionality
    }
    
    // The core method -- executed by the thread
    @Override
    public void run() {
        onStart();
        while (true) {
            List<WebURL> assignedURLs = new ArrayList<>(50);
            
            // Should this change to LinkedBlockingQueue instead??
            isWaitingForNewURLs = true;
            frontier.getNextURLsForCrawling(1, assignedURLs);
            isWaitingForNewURLs = false;
            if (assignedURLs.isEmpty()) {
                if (frontier.isFinished()) {
                    return;
                }
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
            } else {
                for (WebURL curURL : assignedURLs) {
                    if (curURL != null) {
                        processPage(curURL);
                        frontier.setNewProcessedPage(curURL);
                    }
                }
            }
        }
    }
    
    // Processes a single page given the URL
    private void processPage(WebURL curURL) {
        PageFetchResult fetchResult;

        if (curURL == null) {
            return;
        }
        
        // First parse header and check everything is OK
        try {
            fetchResult = fetchHeaderAndCheck(curURL);
            if (fetchResult == null) {
                return;
            }
        } catch (Exception ex) {
            return;
        }
               
        // Fetch page, parse it, parse outgoing links and call visit() for custom handling of page
        try {
            fetchAndHandlePage(fetchResult, curURL);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            fetchResult.discardContentIfNotConsumed();
        }
    }    
    
    // Fetches page header and checks for redirection, page length etc to determine whether the page should be processed
    private PageFetchResult fetchHeaderAndCheck(WebURL curURL) {
        PageFetchResult fetchResult = pageFetcher.fetchHeader(curURL);
        int statusCode = fetchResult.getStatusCode();
        handlePageStatusCode(curURL, statusCode, CustomFetchStatus.getStatusDescription(statusCode));
        if (statusCode != HttpStatus.SC_OK) {
            if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY || statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
                if (config.isFollowRedirects()) {
                    String movedToUrl = fetchResult.getMovedToUrl();
                    if (movedToUrl == null) {
                        return null;
                    }
                    
                    int newDocId = docIdServer.getDocId(movedToUrl);
                    if (newDocId > 0) {
                        // Redirect page is already seen
                        return null;
                    }
                    
                    WebURL webURL = new WebURL();
                    webURL.setURL(movedToUrl);
                    webURL.setParentDocid(curURL.getParentDocid());
                    webURL.setParentUrl(curURL.getParentUrl());
                    webURL.setDepth(curURL.getDepth());
                    webURL.setDocid(-1);
                    webURL.setAnchor(curURL.getAnchor());
                    webURL.setPriority(curURL.getPriority());
                    
                    if (shouldVisit(webURL) && robotstxtServer.allows(webURL)) {
                        webURL.setDocid(docIdServer.createOrGetNewDocID(movedToUrl));
                        frontier.scheduleURLForCrawling(webURL);
                    }
                }
            } else if (fetchResult.getStatusCode() == CustomFetchStatus.PageTooBig) {
                logger.info("Skipping a page which was bigger than max allowed size: " + curURL.getURL());
            }
            return null;
        }

        if (!curURL.getURL().equals(fetchResult.getFetchedUrl())) {
            if (docIdServer.isSeenBefore(fetchResult.getFetchedUrl())) {
                // Redirect page is already seen
                return null;
            }
            curURL.setURL(fetchResult.getFetchedUrl());
            curURL.setDocid(docIdServer.createOrGetNewDocID(fetchResult.getFetchedUrl()));
        }
        
        return fetchResult;
    }
    
    // Fetches and handles page
    private void fetchAndHandlePage(PageFetchResult fetchResult, WebURL curURL) {
        Page page = new Page(curURL);
        int docid = curURL.getDocid();

        if (!fetchResult.fetchContent(page)) {
            onContentFetchError(curURL);
            return;
        }

        if (!parser.parse(page, curURL.getURL())) {
            onParseError(curURL);
            return;
        }

        Content parseData = page.getParseData();
        if (parseData instanceof HtmlContent) {
            HtmlContent htmlParseData = (HtmlContent) parseData;
            List<WebURL> toSchedule = new ArrayList<>();
            int maxCrawlDepth = config.getMaxDepthOfCrawling();
            
            // Parse each outgoing link from the page and add relevant ones to crawl queue
            for (WebURL webURL : htmlParseData.getOutgoingUrls()) {
                webURL.setParentDocid(docid);
                webURL.setParentUrl(curURL.getURL());
                int newdocid = docIdServer.getDocId(webURL.getURL());
                if (newdocid > 0) {
                    webURL.setDepth((short) -1);
                    webURL.setDocid(newdocid);
                } else {
                    webURL.setDocid(-1);
                    webURL.setDepth((short) (curURL.getDepth() + 1));
                    if (maxCrawlDepth == -1 || curURL.getDepth() < maxCrawlDepth) {
                        if (shouldVisit(webURL) && robotstxtServer.allows(webURL)) {
                            webURL.setDocid(docIdServer.createOrGetNewDocID(webURL.getURL()));
                            webURL.setPriority(URLPriority(webURL));
                            toSchedule.add(webURL);
                        }
                    }
                }
            }
            frontier.scheduleUrlsForCrawling(toSchedule);
        }

        // Send the end result to visit() method for use
        visit(page);        
    }
}
