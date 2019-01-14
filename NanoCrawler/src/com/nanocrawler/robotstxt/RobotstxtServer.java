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
package com.nanocrawler.robotstxt;

import com.nanocrawler.data.Page;
import com.nanocrawler.data.PageFetchResult;
import com.nanocrawler.fetcher.PageFetcher;
import com.nanocrawler.urlmanipulation.WebURL;
import com.nanocrawler.util.ContentTypeUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpStatus;

// Fetches the robots.txt file (if one is available) and parses it
public class RobotstxtServer {
    protected RobotstxtConfig config;
    protected final Map<String, HostDirectives> host2directivesCache = new HashMap<>();
    protected PageFetcher pageFetcher;

    // Constructor
    public RobotstxtServer(RobotstxtConfig config, PageFetcher pageFetcher) {
        this.config = config;
        this.pageFetcher = pageFetcher;
    }

    // Returns host of the URL
    private String getHost(URL url) {
        return url.getHost().toLowerCase();
    }

    // Checks if crawling of an URL is allowed based on the URL and host directives from robots.txt
    public boolean allows(WebURL webURL) {
        try {
            URL url = new URL(webURL.getURL());
            String host = getHost(url);
            String path = url.getPath();

            HostDirectives directives = host2directivesCache.get(host);

            if (directives != null && directives.needsRefetch()) {
                synchronized (host2directivesCache) {
                    host2directivesCache.remove(host);
                    directives = null;
                }
            }

            if (directives == null) {
                directives = fetchDirectives(url);
            }
            return directives.allows(path);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return true;
    }

    // Fetches and parses robots.txt file
    private HostDirectives fetchDirectives(URL url) {
        WebURL robotsTxtUrl = new WebURL();
        String host = getHost(url);
        String port = (url.getPort() == url.getDefaultPort() || url.getPort() == -1) ? "" : ":" + url.getPort();
        robotsTxtUrl.setURL("http://" + host + port + "/robots.txt");
        HostDirectives directives = null;
        PageFetchResult fetchResult = null;

        try {
            fetchResult = pageFetcher.fetchHeader(robotsTxtUrl);
            // TO_DO: Does this work on redirects e.g. http://news.ycombinator.com/robots.txt -> https://news.ycombinator.com/robots.txt
            if (fetchResult.getStatusCode() == HttpStatus.SC_OK) {
                Page page = new Page(robotsTxtUrl);
                fetchResult.fetchContent(page);
                if (ContentTypeUtil.hasPlainTextContent(page.getContentType())) {
                    try {
                        String content;
                        if (page.getContentCharset() == null) {
                            content = new String(page.getContentData());
                        } else {
                            content = new String(page.getContentData(), page.getContentCharset());
                        }
                        directives = RobotstxtParser.parse(content, config.getUserAgentName());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } finally {
            if (fetchResult != null) {
                fetchResult.discardContentIfNotConsumed();
            }
        }

        if (directives == null) {
            directives = new HostDirectives();
        }

        synchronized (host2directivesCache) {
            if (host2directivesCache.size() == config.getCacheSize()) {
                String minHost = null;
                long minAccessTime = Long.MAX_VALUE;
                for (Entry<String, HostDirectives> entry : host2directivesCache.entrySet()) {
                    if (entry.getValue().getLastAccessTime() < minAccessTime) {
                        minAccessTime = entry.getValue().getLastAccessTime();
                        minHost = entry.getKey();
                    }
                }
                host2directivesCache.remove(minHost);
            }
            host2directivesCache.put(host, directives);
        }
        return directives;
    }
}
