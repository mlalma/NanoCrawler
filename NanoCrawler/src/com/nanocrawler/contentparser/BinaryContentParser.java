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
package com.nanocrawler.contentparser;

import com.nanocrawler.data.BinaryContent;
import com.nanocrawler.data.Content;
import com.nanocrawler.data.Page;
import com.nanocrawler.util.CrawlConfig;

// Simple binary content parser that is stub
public class BinaryContentParser implements ContentParser {

    CrawlConfig crawlConfig = null;
    
    // Constructor
    public BinaryContentParser(CrawlConfig config) {
        crawlConfig = config;
    }
    
    @Override
    // Checks if the mime type matches known binary content mime types
    public boolean canParseContent(String mimeType) {
        String typeStr = mimeType.toLowerCase();
        if (typeStr.contains("image") || typeStr.contains("audio") || typeStr.contains("video") || typeStr.contains("application")) {
            if (crawlConfig.isIncludeBinaryContentInCrawling()) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
    
    @Override
    // This is a stub implementation, binary content is not actually manipulated in any way
    public Content parseContent(Page content, String contextURL) {
        Content c = new BinaryContent();
        return c;
    }
}
