/*
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

import com.nanocrawler.core.WebCrawler;
import com.nanocrawler.data.HtmlContent;
import com.nanocrawler.data.Page;
import com.nanocrawler.urlmanipulation.WebURL;
import com.nanocrawler.util.CrawlConfig;

import java.util.List;
import java.util.regex.Pattern;

// Simple implementation of a crawler, adapted from crawler4j example
public class SimpleCrawler extends WebCrawler {

    private final String baseDomain;

    // Filter out image, audio, video and similar binary files
    private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|bmp|gif|jpe?g"
            + "|png|tiff?|mid|mp2|mp3|mp4"
            + "|wav|avi|mov|mpeg|ram|m4v|pdf"
            + "|rm|smil|wmv|swf|wma|zip|rar|gz|ico|svg))$");

    // Constructor
    SimpleCrawler(CrawlConfig config, String baseDomain) {
        super(config);
        this.baseDomain = baseDomain;
    }

    @Override
    // Checks if the page give the url should be visited based on the domain
    public boolean shouldVisit(WebURL url) {
        String href = url.getURL().toLowerCase();
        return !FILTERS.matcher(href).matches() && href.startsWith(baseDomain);
    }

    @Override
    // Called after a page has been crawled; crawler can examine the page and perform data manipulations
    public void visit(Page page) {
        int docid = page.getWebURL().getDocid();
        String url = page.getWebURL().getURL();
        String domain = page.getWebURL().getDomain();
        String path = page.getWebURL().getPath();
        String subDomain = page.getWebURL().getSubDomain();
        String parentUrl = page.getWebURL().getParentUrl();
        String anchor = page.getWebURL().getAnchor();

        // Uncomment these to see more information about the crawled page        
        System.out.println("Docid: " + docid);
        System.out.println("URL: " + url);
        System.out.println("Domain: '" + domain + "'");
        System.out.println("Sub-domain: '" + subDomain + "'");
        System.out.println("Path: '" + path + "'");
        System.out.println("Parent page: " + parentUrl);
        System.out.println("Anchor text: " + anchor);

        if (page.getParseData() instanceof HtmlContent) {
            HtmlContent htmlParseData = (HtmlContent) page.getParseData();
            String text = htmlParseData.getText();
            String html = htmlParseData.getHtml();
            List<WebURL> links = htmlParseData.getOutgoingUrls();

            System.out.println("Text length: " + text.length());
            System.out.println("Html length: " + html.length());
            System.out.println("Number of outgoing links: " + links.size());

            System.out.println("Crawled web page: " + url);
        }

        /*
        Header[] responseHeaders = page.getFetchResponseHeaders();
        if (responseHeaders != null) {
            System.out.println("Response headers:");
            for (Header header : responseHeaders) {
                System.out.println("\t" + header.getName() + ": " + header.getValue());
            }
        }

        System.out.println("=============");          
        */
    }
}
