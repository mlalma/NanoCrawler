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

import com.nanocrawler.data.Page;
import com.nanocrawler.util.CrawlConfig;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

// Parses content from the web page
public class Parser {
    protected static final Logger logger = Logger.getLogger(Parser.class.getName());

    private final CrawlConfig config;
    
    private List<ContentParser> parsers = new ArrayList<>();

    // Constructor
    public Parser(CrawlConfig config) {
        this.config = config;
        
        parsers.add(new BinaryContentParser(config));
        parsers.add(new PlainTextContentParser());
        parsers.add(new HtmlContentParser(config));
    }

    // Parses the page by looping through the content parsers to see which one is correct
    public boolean parse(Page page, String contextURL) {
        for (ContentParser p : parsers) {
            if (p.canParseContent(page.getContentType())) {
                page.setParseData(p.parseContent(page, contextURL));
                return true;
            }
        }
        return false;
    }
}
