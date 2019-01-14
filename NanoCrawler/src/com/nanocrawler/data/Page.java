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

import com.nanocrawler.urlmanipulation.WebURL;
import java.nio.charset.Charset;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;

// Contains all the data of a single web page
public class Page {

    protected WebURL url;
    protected byte[] contentData;
    protected String contentType;
    protected String contentEncoding;
    protected String contentCharset;
    protected Header[] fetchResponseHeaders;
    protected Content parseData;

    // Constructor
    public Page(WebURL url) {
        this.url = url;
    }

    // Loads a pages data
    public void load(HttpEntity entity) throws Exception {
        contentType = null;
        Header type = entity.getContentType();
        if (type != null) {
            contentType = type.getValue();
        }

        contentEncoding = null;
        Header encoding = entity.getContentEncoding();
        if (encoding != null) {
            contentEncoding = encoding.getValue();
        }

        Charset charset = ContentType.getOrDefault(entity).getCharset();
        if (charset != null) {
            contentCharset = charset.displayName();	
        }

        contentData = EntityUtils.toByteArray(entity);
    }

    // Setters and getters
    
    public WebURL getWebURL() { return url; }
    public void setWebURL(WebURL url) { this.url = url; }

    public Header[] getFetchResponseHeaders() { return fetchResponseHeaders; }
    public void setFetchResponseHeaders(Header[] headers) { fetchResponseHeaders = headers; }

    public Content getParseData() { return parseData; }
    public void setParseData(Content parseData) { this.parseData = parseData; }

    public byte[] getContentData() { return contentData; }
    public void setContentData(byte[] contentData) { this.contentData = contentData; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getContentEncoding() { return contentEncoding; }
    public void setContentEncoding(String contentEncoding) { this.contentEncoding = contentEncoding; }

    public String getContentCharset() { return contentCharset; }
    public void setContentCharset(String contentCharset) { this.contentCharset = contentCharset; }
}
