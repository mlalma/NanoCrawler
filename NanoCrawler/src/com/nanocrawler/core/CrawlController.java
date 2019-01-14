/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Based on crawler4j project by Yasser Ganjisaffar
 */
package com.nanocrawler.core;

import com.nanocrawler.dbs.DocIDServer;
import com.nanocrawler.dbs.Frontier;
import com.nanocrawler.fetcher.PageFetcher;
import com.nanocrawler.robotstxt.RobotstxtServer;
import com.nanocrawler.urlmanipulation.URLCanonicalizer;
import com.nanocrawler.urlmanipulation.WebURL;
import com.nanocrawler.util.CrawlConfig;
import com.nanocrawler.util.IO;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

// Controller for managing the crawling session and WebCrawler instances
public class CrawlController implements Runnable {

    static final Logger logger = Logger.getLogger(CrawlController.class.getName());

    // Status flag indicating if the crawling has completed
    protected boolean finished;

    // Status flag indicating for an external shut down command
    protected boolean shuttingDown;

    protected PageFetcher pageFetcher;
    protected RobotstxtServer robotstxtServer;
    protected Frontier frontier;
    protected DocIDServer docIdServer;
    protected CrawlConfig config;

    protected final Object waitingLock = new Object();

    protected List<Thread> threads = new ArrayList<>();
    protected List<WebCrawler> crawlers = new ArrayList<>();

    // Constructor
    public CrawlController(CrawlConfig config, PageFetcher pageFetcher, RobotstxtServer robotstxtServer) throws Exception {
        this.config = config;
        config.validate();

        // Creates a folder for temporary data
        File folder = new File(config.getCrawlStorageFolder());
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                throw new Exception("Couldn't create this folder: " + folder.getAbsolutePath());
            }
        }

        logger.info("Setting envconfig");

        // Configuration for Berkley DB instances
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(false);
        envConfig.setLocking(false);
        File envHome = new File(config.getCrawlStorageFolder() + "/frontier");
        if (!envHome.exists()) {
            if (!envHome.mkdir()) {
                throw new Exception("Couldn't create this folder: " + envHome.getAbsolutePath());
            }
        }
        IO.deleteFolderContents(envHome);
        Environment env = new Environment(envHome, envConfig);

        logger.info("setting servers");

        // Berkley DB servers for storing data
        docIdServer = new DocIDServer(env, config);
        frontier = new Frontier(env, docIdServer, config);

        this.pageFetcher = pageFetcher;
        this.robotstxtServer = robotstxtServer;

        finished = true;
        shuttingDown = false;
    }

    // Adds a new seed URL. A seed URL is a URL that is fetched by the crawler
    // to extract new URLs in it and follow them for crawling. If specified docId is -1, 
    // then a new docId is assigned for the page URL 
    public void addSeed(String pageUrl, int docId) {
        String canonicalUrl = URLCanonicalizer.getCanonicalURL(pageUrl);
        if (canonicalUrl == null) {
            logger.error("Invalid seed URL: " + pageUrl);
            return;
        }
        if (docId < 0) {
            docId = docIdServer.getDocId(canonicalUrl);
            if (docId > 0) {
                // This URL is already seen.
                return;
            }
            docId = docIdServer.createOrGetNewDocID(canonicalUrl);
        } else {
            try {
                docIdServer.addUrlAndDocId(canonicalUrl, docId);
            } catch (Exception e) {
                logger.error("Could not add seed: " + e.getMessage());
            }
        }

        WebURL webUrl = new WebURL();
        webUrl.setURL(canonicalUrl);
        webUrl.setDocid(docId);
        webUrl.setDepth((short) 0);

        if (!robotstxtServer.allows(webUrl)) {
            logger.info("Robots.txt does not allow this seed: " + pageUrl);
        } else {
            frontier.scheduleURLForCrawling(webUrl);
        }
    }

    // Adds "seen URL" to doc ID database
    public void addSeenUrl(String url, int docId) {
        String canonicalUrl = URLCanonicalizer.getCanonicalURL(url);
        if (canonicalUrl == null) {
            logger.error("Invalid Url: " + url);
            return;
        }
        try {
            docIdServer.addUrlAndDocId(canonicalUrl, docId);
        } catch (Exception e) {
            logger.error("Could not add seen url: " + e.getMessage());
        }
    }

    // Start crawling using list of seed urls, list of crawlers and flag indicating whether the main thread 
    // should be blocked until the crawling has completed
    public void startCrawling(List<WebCrawler> c, List<String> seedUrls, boolean isBlocking) {
        for (String seed : seedUrls) {
            addSeed(seed, -1);
        }
        this.start(c, isBlocking);
    }

    // Starts the crawling
    private void start(List<WebCrawler> c, boolean isBlocking) {
        if (!finished) {
            throw new RuntimeException("Cannot start crawling when it's already underway!");
        }

        try {
            finished = false;

            threads.clear();
            crawlers.clear();

            for (WebCrawler crawler : c) {
                crawler.getThread().start();
                crawlers.add(crawler);
                threads.add(crawler.getThread());
                logger.info("Crawler " + crawler.getId() + " started.");
            }

            Thread monitorThread = new Thread(this);
            monitorThread.start();

            if (isBlocking) {
                waitUntilFinish();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Synchronizes the waiting lock to ensure that crawling has completed before returning
    public void waitUntilFinish() {
        while (!finished) {
            synchronized (waitingLock) {
                if (finished) {
                    return;
                }
                try {
                    waitingLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Crude way to implement polling on thread
    private void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (Exception ignored) {
        }
    }

    // Monitors crawler threads to check if they have stopped
    @Override
    public void run() {
        try {
            synchronized (waitingLock) {
                while (true) {
                    sleep(5);
                    boolean someoneIsWorking = false;
                    for (int i = 0; i < threads.size(); i++) {
                        Thread thread = threads.get(i);
                        if (!thread.isAlive()) {
                            if (!shuttingDown) {
                                logger.info("Thread " + i + " is dead.");
                            }
                        } else if (crawlers.get(i).isNotWaitingForNewURLs()) {
                            someoneIsWorking = true;
                        }
                    }
                    if (!someoneIsWorking) {
                        logger.info("It looks like no thread is working, waiting for 5 seconds to make sure...");
                        sleep(5);

                        someoneIsWorking = false;
                        for (int i = 0; i < threads.size(); i++) {
                            Thread thread = threads.get(i);
                            if (thread.isAlive() && crawlers.get(i).isNotWaitingForNewURLs()) {
                                someoneIsWorking = true;
                            }
                        }

                        if (!someoneIsWorking) {
                            if (!shuttingDown) {
                                long queueLength = frontier.getCrawlQueueLength();
                                if (queueLength > 0) {
                                    continue;
                                }
                                logger.info("No thread is working and no more URLs are in queue waiting for another 5 seconds to make sure...");
                                sleep(5);
                                queueLength = frontier.getCrawlQueueLength();
                                if (queueLength > 0) {
                                    continue;
                                }
                            }

                            logger.info("All of the crawlers are stopped. Finishing the process...");

                            // Frontier informs web crawler threads to stop
                            frontier.finish();
                            for (WebCrawler crawler : crawlers) {
                                crawler.onBeforeExit();
                            }

                            logger.info("Waiting for 5 seconds before final clean up...");
                            sleep(5);

                            frontier.close();
                            docIdServer.close();
                            pageFetcher.shutDown();

                            finished = true;
                            waitingLock.notifyAll();

                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Stop all operations
            shutdown();
        }
    }

    // Shuts down the crawling process / cancels the 
    public void shutdown() {
        logger.info("Shutting down...");
        this.shuttingDown = true;
        frontier.finish();
    }

    // Setters / getters for various variables

    public void setPageFetcher(PageFetcher pageFetcher) {
        this.pageFetcher = pageFetcher;
    }

    public PageFetcher getPageFetcher() {
        return pageFetcher;
    }

    public RobotstxtServer getRobotstxtServer() {
        return robotstxtServer;
    }

    public void setRobotstxtServer(RobotstxtServer robotstxtServer) {
        this.robotstxtServer = robotstxtServer;
    }

    public void setFrontier(Frontier frontier) {
        this.frontier = frontier;
    }

    public Frontier getFrontier() {
        return frontier;
    }

    public void setDocIdServer(DocIDServer docIdServer) {
        this.docIdServer = docIdServer;
    }

    public DocIDServer getDocIdServer() {
        return docIdServer;
    }

    public boolean isFinished() {
        return this.finished;
    }

    public boolean isShuttingDown() {
        return shuttingDown;
    }
}
