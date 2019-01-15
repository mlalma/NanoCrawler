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
package com.nanocrawler.testapp;

import com.nanocrawler.core.CrawlController;
import com.nanocrawler.core.WebCrawler;
import com.nanocrawler.fetcher.PageFetcher;
import com.nanocrawler.robotstxt.RobotstxtConfig;
import com.nanocrawler.robotstxt.RobotstxtServer;
import com.nanocrawler.util.CrawlConfig;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

// Simple test application that crawls pages under a pre-set domain
public class NanoCrawlerSimpleTestApp {

    private final String BASE_DOMAIN = "http://www.hs.fi/";

    private void doMain() throws Exception {
        //BasicConfigurator.configure();
        //Logger logger = Logger.getRootLogger();
        //logger.setLevel(Level.DEBUG);
        // Set this to a valid temp directory that is used during the crawl 
        String crawlStorageFolder = "/tmp";

        // Check that path is valid and the file is there
        CrawlConfig config = new CrawlConfig("./res/tld-names.txt");
        config.setCrawlStorageFolder(crawlStorageFolder);

        PageFetcher pageFetcher = new PageFetcher(config);
        pageFetcher.initialize();

        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
        CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);

        List<String> seeds = new ArrayList<>();
        seeds.add(BASE_DOMAIN);

        List<WebCrawler> crawlers = new ArrayList<>();
        SimpleCrawler c = new SimpleCrawler(config, BASE_DOMAIN);
        c.init(1, controller, config);
        crawlers.add(c);

        controller.startCrawling(crawlers, seeds, true);
    }

    // Entry point to the test app
    public static void main(String[] args) {
        try {
            NanoCrawlerSimpleTestApp main = new NanoCrawlerSimpleTestApp();
            main.doMain();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
