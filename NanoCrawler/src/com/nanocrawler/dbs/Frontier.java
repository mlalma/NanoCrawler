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
package com.nanocrawler.dbs;

import com.nanocrawler.util.CrawlConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.nanocrawler.urlmanipulation.WebURL;
import java.util.List;
import org.apache.log4j.Logger;

// Keeps track of the URLs that are still to be crawled
public class Frontier  {
    protected static final Logger logger = Logger.getLogger(Frontier.class.getName());

    protected final Object mutex = new Object();
    protected final Object waitingList = new Object();

    protected boolean isFinished = false;
    
    protected CrawledURLQueueServer workQueues;
    protected long scheduledPages;
    protected DocIDServer docIdServer;
    protected CrawlStatisticsServer crawlStatisticsServer;
    
    private final CrawlConfig config;

    // Constructor
    public Frontier(Environment env, DocIDServer docIdServer, CrawlConfig config) {
        this.config = config;
        this.crawlStatisticsServer = new CrawlStatisticsServer(env);
        this.docIdServer = docIdServer;
        workQueues = new CrawledURLQueueServer(env, "PendingURLsDB");
        scheduledPages = 0;
    }

    // Adds to list of crawled pages new URLs from the argument urls
    public void scheduleUrlsForCrawling(List<WebURL> urls) {
        int maxPagesToFetch = config.getMaxPagesToFetch();
        synchronized (mutex) {
            int newScheduledPage = 0;
            for (WebURL url : urls) {
                if (maxPagesToFetch > 0 && (scheduledPages + newScheduledPage) >= maxPagesToFetch) {
                    break;
                }
                try {
                    workQueues.putURLToQueue(url);
                    newScheduledPage++;
                } catch (DatabaseException e) {
                    logger.error("Error while puting the url in the work queue.");
                }
            }
            
            if (newScheduledPage > 0) {
                scheduledPages += newScheduledPage;
                crawlStatisticsServer.increment(CrawlStatisticsServer.SCHEDULED_PAGES, newScheduledPage);	
            }
            
            synchronized (waitingList) {
                waitingList.notifyAll();
            }
        }
    }

    // Adds a new url to crawled page URL list
    public void scheduleURLForCrawling(WebURL url) {
        int maxPagesToFetch = config.getMaxPagesToFetch();
        synchronized (mutex) {
            try {
                if (maxPagesToFetch < 0 || scheduledPages < maxPagesToFetch) {
                    workQueues.putURLToQueue(url);
                    scheduledPages++;
                    crawlStatisticsServer.increment(CrawlStatisticsServer.SCHEDULED_PAGES);
                }
            } catch (DatabaseException e) {
                logger.error("Error while puting the url in the work queue.");
            }
        }
    }

    // Returns new URLs for crawling
    public void getNextURLsForCrawling(int max, List<WebURL> result) {
        while (true) {
            synchronized (mutex) {
                if (isFinished) {
                    return;
                }
                try {
                    List<WebURL> curResults = workQueues.getNewURLs(max);
                    workQueues.delete(curResults.size());
                    result.addAll(curResults);
                } catch (DatabaseException e) {
                    logger.error("Error while getting next urls: " + e.getMessage());
                    e.printStackTrace();
                }
                if (result.size() > 0) {
                    return;
                }
            }

            // If there are no new URLs to be crawled, put the crawl thread on halt until new URLs have emerged
            try {
                synchronized (waitingList) {
                    waitingList.wait();
                }
            } catch (InterruptedException ex) {
            }
            
            if (isFinished) {
                return;
            }
        }
    }

    // Set statistics on processed pages
    public void setNewProcessedPage(WebURL webURL) {
        crawlStatisticsServer.increment(CrawlStatisticsServer.PROCESSED_PAGES);
    }

    // Returns number of processed pages
    public long getNumberOfProcessedPages() {
        return crawlStatisticsServer.getValue(CrawlStatisticsServer.PROCESSED_PAGES);
    }

    // Returns the length of crawl queue (num of URLs to be crawled)
    public long getCrawlQueueLength() {
        return workQueues.getCrawlQueueLength();
    }

    // Explicitly synchronize the servers
    public void sync() {
        workQueues.sync();
        docIdServer.sync();
        crawlStatisticsServer.sync();
    }

    // Closes databases
    public void close() {
        sync();
        workQueues.close();
        crawlStatisticsServer.close();
    }

    // Returns the status of crawling process
    public boolean isFinished() {
        return isFinished;
    }

    // Stops the crawling process
    public void finish() {
        isFinished = true;
        synchronized (waitingList) {
            waitingList.notifyAll();
        }
    }
}
