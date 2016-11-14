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
package com.nanocrawler.dbs;

import com.sleepycat.je.*;
import java.util.HashMap;
import java.util.Map;

// Calculates total statistics of the crawl jobs
public class CrawlStatisticsServer {
	
    public final static String SCHEDULED_PAGES = "Scheduled-Pages";
    public final static String PROCESSED_PAGES = "Processed-Pages";

    protected final Object mutex = new Object();
    protected Map<String, Long> counterValues;

    // Constructor
    public CrawlStatisticsServer(Environment env) throws DatabaseException {
        this.counterValues = new HashMap<>();        
    }

    // Returns value of e.g. scheduled (pages in queue) and processed pages
    public long getValue(String name) {
        synchronized (mutex) {
            Long value = counterValues.get(name);
            if (value == null) {
                return 0;
            }
            return value.longValue();
        }
    }

    // Sets directly hash map value
    public void setValue(String name, long value) {
        synchronized (mutex) {
            counterValues.put(name, new Long(value));
        }
    }

    // Increments value behind hash key name
    public void increment(String name, long addition) {
        synchronized (mutex) {
            long prevValue = getValue(name);
            setValue(name, prevValue + addition);
        }
    }

    // Does increment of a value in a single step
    public void increment(String name) {
        increment(name, 1);
    }

    // Always in sync, there is not DB behind this, only hash set
    public void sync() {
    }

    // Does nothing as stats are not saved locally
    public void close() {
    }
}
