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
package com.nanocrawler.data;

import java.io.EOFException;
import java.io.IOException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

// Class to handle page header/content fetching and data related to it
public class PageFetchResult {
    protected static final Logger logger = Logger.getLogger(PageFetchResult.class);
    protected int statusCode;
    protected HttpEntity entity = null;
    protected Header[] responseHeaders = null;
    protected String fetchedUrl = null;
    protected String movedToUrl = null;

    // Constructor
    public PageFetchResult() {        
    }

    // Fetches page content and sets the response headers
    public boolean fetchContent(Page page) {
        try {
            page.load(entity);
            page.setFetchResponseHeaders(responseHeaders);
            return true;
        } catch (Exception e) {
            logger.info("Exception while fetching content for: " + page.getWebURL().getURL() + " [" + e.getMessage() + "]");
        }
        return false;
    }

    // Discards unparsed content from the HttpEntity
    public void discardContentIfNotConsumed() {
        try {
            if (entity != null) {
                EntityUtils.consume(entity);
            }
        } catch (EOFException e) {
            // We can ignore this exception. It can happen on compressed streams
            // which are not repeatable
        } catch (IOException e) {
            // We can ignore this exception. It can happen if the stream is
            // closed.
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Setters and getters
    
    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }

    public HttpEntity getEntity() { return entity; }
    public void setEntity(HttpEntity entity) { this.entity = entity; }

    public Header[] getResponseHeaders() { return responseHeaders; }
    public void setResponseHeaders(Header[] responseHeaders) { this.responseHeaders = responseHeaders; }

    public String getFetchedUrl() { return fetchedUrl; }
    public void setFetchedUrl(String fetchedUrl) { this.fetchedUrl = fetchedUrl; }

    public String getMovedToUrl() { return movedToUrl; }
    public void setMovedToUrl(String movedToUrl) { this.movedToUrl = movedToUrl; }
}
