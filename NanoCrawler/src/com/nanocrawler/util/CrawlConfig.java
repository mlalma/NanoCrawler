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
package com.nanocrawler.util;

// Singleton class containing the configurations for the whole crawler
public class CrawlConfig {

    private String crawlStorageFolder;

    private int maxDepthOfCrawling = -1;
    private int maxPagesToFetch = -1;

    private String userAgentString = "NanoCrawler";

    // In case robots.txt won't provide crawl-delay directive
    private int politenessDelay = 200;

    private boolean includeHttpsPages = true;
    private boolean includeBinaryContentInCrawling = false;

    private int maxConnectionsPerHost = 100;
    private int maxTotalConnections = 100;

    private int socketTimeout = 20000;
    private int connectionTimeout = 30000;

    private int maxOutgoingLinksToFollow = 5000;

    private int maxDownloadSize = 1048576;

    private boolean followRedirects = true;

    private String tldResourceFilePath = "";

    // Proxy configuration parameters in case
    private String proxyHost = null;
    private int proxyPort = 80;
    private String proxyUsername = null;
    private String proxyPassword = null;

    private static TLDList TLDListInstance;

    // Constructor
    public CrawlConfig(String tldResourceFilePath) {
        TLDListInstance = new TLDList(tldResourceFilePath);
    }

    // Configuration validation
    public void validate() throws Exception {
        if (crawlStorageFolder == null) {
            throw new Exception("Crawl storage folder is not set in the CrawlConfig.");
        }
        if (politenessDelay < 0) {
            throw new Exception("Invalid value for politeness delay: " + politenessDelay);
        }
        if (maxDepthOfCrawling < -1) {
            throw new Exception("Maximum crawl depth should be either a positive number or -1 for unlimited depth.");
        }
        if (maxDepthOfCrawling > Short.MAX_VALUE) {
            throw new Exception("Maximum value for crawl depth is " + Short.MAX_VALUE);
        }
    }

    // Setters / getters for the configuration

    public static TLDList getTLDListInstance() {
        return TLDListInstance;
    }

    public String getCrawlStorageFolder() {
        return crawlStorageFolder;
    }

    public void setCrawlStorageFolder(String crawlStorageFolder) {
        this.crawlStorageFolder = crawlStorageFolder;
    }

    public int getMaxDepthOfCrawling() {
        return maxDepthOfCrawling;
    }

    public void setMaxDepthOfCrawling(int maxDepthOfCrawling) {
        this.maxDepthOfCrawling = maxDepthOfCrawling;
    }

    public int getMaxPagesToFetch() {
        return maxPagesToFetch;
    }

    public void setMaxPagesToFetch(int maxPagesToFetch) {
        this.maxPagesToFetch = maxPagesToFetch;
    }

    public String getUserAgentString() {
        return userAgentString;
    }

    public void setUserAgentString(String userAgentString) {
        this.userAgentString = userAgentString;
    }

    public int getPolitenessDelay() {
        return politenessDelay;
    }

    public void setPolitenessDelay(int politenessDelay) {
        this.politenessDelay = politenessDelay;
    }

    public boolean isIncludeHttpsPages() {
        return includeHttpsPages;
    }

    public void setIncludeHttpsPages(boolean includeHttpsPages) {
        this.includeHttpsPages = includeHttpsPages;
    }

    public boolean isIncludeBinaryContentInCrawling() {
        return includeBinaryContentInCrawling;
    }

    public void setIncludeBinaryContentInCrawling(boolean includeBinaryContentInCrawling) {
        this.includeBinaryContentInCrawling = includeBinaryContentInCrawling;
    }

    public int getMaxConnectionsPerHost() {
        return maxConnectionsPerHost;
    }

    public void setMaxConnectionsPerHost(int maxConnectionsPerHost) {
        this.maxConnectionsPerHost = maxConnectionsPerHost;
    }

    public int getMaxTotalConnections() {
        return maxTotalConnections;
    }

    public void setMaxTotalConnections(int maxTotalConnections) {
        this.maxTotalConnections = maxTotalConnections;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getMaxOutgoingLinksToFollow() {
        return maxOutgoingLinksToFollow;
    }

    public void setMaxOutgoingLinksToFollow(int maxOutgoingLinksToFollow) {
        this.maxOutgoingLinksToFollow = maxOutgoingLinksToFollow;
    }

    public int getMaxDownloadSize() {
        return maxDownloadSize;
    }

    public void setMaxDownloadSize(int maxDownloadSize) {
        this.maxDownloadSize = maxDownloadSize;
    }

    public boolean isFollowRedirects() {
        return followRedirects;
    }

    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getProxyUsername() {
        return proxyUsername;
    }

    public void setProxyUsername(String proxyUsername) {
        this.proxyUsername = proxyUsername;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

    @Override
    public String toString() {
        String s = "";
        s += "Crawl storage folder: " + getCrawlStorageFolder() + "\n";
        s += "Max depth of crawl: " + getMaxDepthOfCrawling() + "\n";
        s += "Max pages to fetch: " + getMaxPagesToFetch() + "\n";
        s += "User agent string: " + getUserAgentString() + "\n";
        s += "Include https pages: " + isIncludeHttpsPages() + "\n";
        s += "Include binary content: " + isIncludeBinaryContentInCrawling() + "\n";
        s += "Max connections per host: " + getMaxConnectionsPerHost() + "\n";
        s += "Max total connections: " + getMaxTotalConnections() + "\n";
        s += "Socket timeout: " + getSocketTimeout() + "\n";
        s += "Max total connections: " + getMaxTotalConnections() + "\n";
        s += "Max outgoing links to follow: " + getMaxOutgoingLinksToFollow() + "\n";
        s += "Max download size: " + getMaxDownloadSize() + "\n";
        s += "Should follow redirects?: " + isFollowRedirects() + "\n";
        s += "Proxy host: " + getProxyHost() + "\n";
        s += "Proxy port: " + getProxyPort() + "\n";
        s += "Proxy username: " + getProxyUsername() + "\n";
        s += "Proxy password: " + getProxyPassword() + "\n";
        return s;
    }
}
