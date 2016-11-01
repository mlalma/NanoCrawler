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
 */
package com.nanocrawler.feedfinder;

import com.cfta.cf.feeds.RSSFeedCleaner;
import com.cfta.cf.feeds.RSSFeedParser;
import com.cfta.cf.handlers.protocol.RSSFeedResponse;
import com.nanocrawler.core.WebCrawler;
import com.nanocrawler.data.HtmlContent;
import com.nanocrawler.data.Page;
import com.nanocrawler.urlmanipulation.WebURL;
import com.nanocrawler.util.CrawlConfig;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import yarfraw.core.datamodel.FeedFormat;
import yarfraw.utils.FeedFormatDetector;

// RSS feed crawler - checks & prioritizes RSS feeds over all other contents
public class RSSFeedCrawler extends WebCrawler {

    private final String baseDomain;
    private final RSSFeedCleaner c = new RSSFeedCleaner();
        
    // Filter out image, audio, video and similar binary files
    private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|bmp|gif|jpe?g" 
                                                          + "|png|tiff?|mid|mp2|mp3|mp4"
                                                          + "|wav|avi|mov|mpeg|ram|m4v|pdf" 
                                                          + "|rm|smil|wmv|swf|wma|zip|rar|gz))$");
    
    // Constructor
    public RSSFeedCrawler(CrawlConfig config, String baseDomain) {
        super(config);
        this.baseDomain = baseDomain;
    }
        
    @Override
    // Checks if the page give the url should be visited based on the domain
    public boolean shouldVisit(WebURL url) {
        String href = url.getURL().toLowerCase();                
        return !FILTERS.matcher(href).matches() && href.startsWith(baseDomain);
    }
    
    // Tests feed that it can be parsed OK and that it has RSS feed items that are not too old
    private boolean testFeed(String feed, String feedUrl) {
        final long RSS_ITEM_CUTOFF_TIME = 1000L * 60L * 60L * 24L * 30L;
        boolean feedOk = false;
        try {
            RSSFeedParser feedParser = new RSSFeedParser();
            RSSFeedResponse response = feedParser.parseFeedFromString(feed);
            if (response != null) {
                if (response.rssItems.size() > 0) {
                    System.out.println("" + response.rssItems.size() + " items on feed: " + feedUrl);
                    for (RSSFeedResponse.RSSItem item : response.rssItems) {
                        if (item.date.getTime() > System.currentTimeMillis() - RSS_ITEM_CUTOFF_TIME) {
                            feedOk = true;
                            break;
                        }
                    }
                    
                    if (!feedOk) {
                        System.out.println("All items too old: " + feedUrl);
                    }
                } else {
                    System.out.println("Zero items on feed: " + feedUrl);
                }
            }
        } catch (Exception ex) {
            System.out.println("Failed to get/read the feed: " + feedUrl);
            ex.printStackTrace();
        }
        
        return feedOk;
    }
    
    // Checks if page is a valid feed
    public boolean isRSSFeed(String page) {
        boolean isFeed = false;
        try {
            if (page.length() > 0) {              
                page = page.trim();
                page = c.cleanFeedString(page);
                FeedFormat fformat = FeedFormatDetector.getFormat(IOUtils.toInputStream(page.trim()), false);
                if (fformat != FeedFormat.UNKNOWN) {
                    isFeed = true;
                }
            }    
        } catch (Exception ex) {            
        }        
        return isFeed;
    }

    // Can be used to set per-URL priority
    @Override
    protected byte URLPriority(WebURL webUrl) {
        byte priority = 100;
        
        // Scanning priority for url, very simple heuristics, giving preference to links which have "rss" or "feed" in text or in path
        // This will cause (of course) false positives, but works pretty well in general
        if (webUrl.getAnchor().toLowerCase().contains("rss") || webUrl.getAnchor().toLowerCase().contains("feed")  ||
            webUrl.getURL().toLowerCase().contains("xml") || webUrl.getURL().toLowerCase().contains("rss") || webUrl.getAnchor().toLowerCase().contains("feed")) {
            priority = 0;
        }
                       
        return priority;
    }
    
    @Override
    // Called after a page has been crawled; crawler can examine the page and perform data manipulations
    public void visit(Page page) {                 
        //System.out.println("Parsed page: " + page.getWebURL().getURL());
        if (page.getParseData() instanceof HtmlContent) {
            HtmlContent htmlParseData = (HtmlContent) page.getParseData();
            String html = htmlParseData.getHtml();
            if (isRSSFeed(html)) {
                System.out.println("Found feed: " + page.getWebURL().getURL());
                testFeed(html, page.getWebURL().getURL());
            } else {
                System.out.println("NOT RSS feed: " + page.getWebURL().getURL());
            }
        }
    }
}
