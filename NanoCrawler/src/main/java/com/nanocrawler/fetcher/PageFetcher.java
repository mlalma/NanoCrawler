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
package com.nanocrawler.fetcher;

import com.nanocrawler.data.CustomFetchStatus;
import com.nanocrawler.data.PageFetchResult;
import com.nanocrawler.urlmanipulation.URLCanonicalizer;
import com.nanocrawler.urlmanipulation.WebURL;
import com.nanocrawler.util.CrawlConfig;
import java.io.IOException;
import java.util.Date;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParamBean;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;

// Page fetcher class
public class PageFetcher {
    
    protected static final Logger logger = Logger.getLogger(PageFetcher.class);
    
    protected DefaultHttpClient httpClient;
    protected final Object mutex = new Object();
    protected long lastFetchTime = 0;
    
    protected PoolingClientConnectionManager connectionManager;    
    protected IdleConnectionMonitorThread connectionMonitorThread = null;    
    
    private final CrawlConfig config;
    
    private static PageFetcher instance = null;
   
    // Returns HTTP client used in fetching pages
    public HttpClient getHttpClient() {
        return httpClient;
    }
    
    // Constructor
    public PageFetcher(CrawlConfig config) {
        this.config = config;
        
        HttpParams params = new BasicHttpParams();
        HttpProtocolParamBean paramsBean = new HttpProtocolParamBean(params);
        paramsBean.setVersion(HttpVersion.HTTP_1_1);
        paramsBean.setContentCharset("UTF-8");
        paramsBean.setUseExpectContinue(false);
        params.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
        params.setParameter(CoreProtocolPNames.USER_AGENT, config.getUserAgentString());
        params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, config.getSocketTimeout());
        params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, config.getConnectionTimeout());
        params.setBooleanParameter("http.protocol.handle-redirects", false);

        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
        if (config.isIncludeHttpsPages()) {
            schemeRegistry.register(new Scheme("https", 443, SSLSocketFactory.getSocketFactory()));
        }

        connectionManager = new PoolingClientConnectionManager(schemeRegistry);
        connectionManager.setMaxTotal(config.getMaxTotalConnections());
        connectionManager.setDefaultMaxPerRoute(config.getMaxConnectionsPerHost());
        httpClient = new DefaultHttpClient(connectionManager, params);
    }
            
    // Initializes fetcher and starts connection monitoring
    public void initialize() {
        if (config.getProxyHost() != null) {
            if (config.getProxyUsername() != null) {
                httpClient.getCredentialsProvider().setCredentials(
                    new AuthScope(config.getProxyHost(), config.getProxyPort()),
                    new UsernamePasswordCredentials(config.getProxyUsername(), config.getProxyPassword()));
            }

            HttpHost proxy = new HttpHost(config.getProxyHost(), config.getProxyPort());
            httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        }

        // Since fetcher is manually working on HTTP responses, gzipped response data has to be decompressed manually
        httpClient.addResponseInterceptor(new HttpResponseInterceptor() {
            @Override
            public void process(final HttpResponse response, final HttpContext context) throws HttpException, IOException {
                HttpEntity entity = response.getEntity();
                Header contentEncoding = entity.getContentEncoding();
                               
                if (contentEncoding != null) {
                    HeaderElement[] codecs = contentEncoding.getElements();
                    for (HeaderElement codec : codecs) {
                        if (codec.getName().equalsIgnoreCase("gzip")) {                            
                            response.setEntity(new GzipDecompressingEntity(response.getEntity()));
                            return;
                        }
                    }
                }
            }
        });

        if (connectionMonitorThread == null) {
            connectionMonitorThread = new IdleConnectionMonitorThread(connectionManager);
        }
        connectionMonitorThread.start();
    }

    // Waits for the next fetch start - single fetcher, called from several crawlers, hence mutex object is used
    private void waitForFetchStart() throws InterruptedException {
        synchronized (mutex) {
            long now = (new Date()).getTime();
            // TO_DO: CRAWL DELAY HERE!
            if (now - lastFetchTime < config.getPolitenessDelay()) {
                Thread.sleep(config.getPolitenessDelay() - (now - lastFetchTime));
            }
            lastFetchTime = (new Date()).getTime();
        }        
    }
        
    // Checks the header and returns true / false depending on status code from the server
    private boolean checkHeader(PageFetchResult fetchResult, HttpResponse response, String toFetchURL, HttpGet get) {
        boolean headerOk = false;
        
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != HttpStatus.SC_OK) {
            if (statusCode != HttpStatus.SC_NOT_FOUND) {
                if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY || statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
                    Header header = response.getFirstHeader("Location");
                    if (header != null) {
                        String movedToUrl = header.getValue();
                        movedToUrl = URLCanonicalizer.getCanonicalURL(movedToUrl, toFetchURL);
                        fetchResult.setMovedToUrl(movedToUrl);
                    } 
                    fetchResult.setStatusCode(statusCode);
                }
                logger.info("Failed: " + response.getStatusLine().toString() + ", while fetching " + toFetchURL);
            }
            fetchResult.setStatusCode(response.getStatusLine().getStatusCode());
        } else {
            fetchResult.setFetchedUrl(toFetchURL);
            String uri = get.getURI().toString();
            if (!uri.equals(toFetchURL)) {
                if (!URLCanonicalizer.getCanonicalURL(uri).equals(toFetchURL)) {
                    fetchResult.setFetchedUrl(uri);
                }
            }

            headerOk = true;
        }
    
        return headerOk;
    }
    
    // Checks content length of the the body of the response (and if there is one)
    private boolean checkBody(PageFetchResult fetchResult, HttpResponse response) {
        boolean bodyOk = false;
        if (fetchResult.getEntity() != null) {
            long size = fetchResult.getEntity().getContentLength();
            if (size == -1) {
                Header length = response.getLastHeader("Content-Length");
                if (length == null) {
                    length = response.getLastHeader("Content-length");
                }
                if (length != null) {
                    size = Integer.parseInt(length.getValue());
                } else {
                    size = -1;
                }
            }

            if (size > config.getMaxDownloadSize()) {
                fetchResult.setStatusCode(CustomFetchStatus.PageTooBig);
            } else {
                fetchResult.setStatusCode(HttpStatus.SC_OK);
                bodyOk = true;
            }
        }        
        return bodyOk;
    }    

    // Fetches header of a page given the URL
    public PageFetchResult fetchHeader(WebURL webUrl) {
        PageFetchResult fetchResult = new PageFetchResult();
        String toFetchURL = webUrl.getURL();
        HttpGet get = null;
        
        try {
            get = new HttpGet(toFetchURL);
            get.addHeader("Accept-Encoding", "gzip");

            waitForFetchStart();
            
            HttpResponse response = httpClient.execute(get);
            fetchResult.setEntity(response.getEntity());
            fetchResult.setResponseHeaders(response.getAllHeaders());

            if (checkHeader(fetchResult, response, toFetchURL, get)) {
                if (checkBody(fetchResult, response)) {
                    // All good, this page checks out with regards to header & body parameters
                } else {
                    get.abort();
                }
            } else {
                get.abort();
            }
            
            return fetchResult;
        } catch (IOException e) {
            logger.error("Fatal transport error: " + e.getMessage() + " while fetching " + toFetchURL + " (link found in doc #" + webUrl.getParentDocid() + ")");
            fetchResult.setStatusCode(CustomFetchStatus.FatalTransportError);
            get.abort();
            return fetchResult;
        } catch (IllegalStateException e) {
            // Ignoring exceptions that occur because of not registering https and other schemes
        } catch (Exception e) {
            if (e.getMessage() == null) {
                    logger.error("Error while fetching " + webUrl.getURL());
            } else {
                    logger.error(e.getMessage() + " while fetching " + webUrl.getURL());
            }
        }
        
        fetchResult.setStatusCode(CustomFetchStatus.UnknownError);
        return fetchResult;
    }
    
    // Shuts down the connection manager 
    public synchronized void shutDown() {
        if (connectionMonitorThread != null) {
            connectionManager.shutdown();
            connectionMonitorThread.shutdown();
        }
    }
}
