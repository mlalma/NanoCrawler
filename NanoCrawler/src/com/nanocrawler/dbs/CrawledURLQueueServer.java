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
package com.nanocrawler.dbs;

import com.nanocrawler.util.ContentTypeUtil;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.OperationStatus;
import com.nanocrawler.urlmanipulation.WebURL;

import java.util.ArrayList;
import java.util.List;

// DB server to handle crawled URLs
public class CrawledURLQueueServer {

    protected Database urlsDB = null;
    protected Environment env;
    protected WebURLTupleBinding webURLBinding;
    protected final Object mutex = new Object();

    // Constructor
    public CrawledURLQueueServer(Environment env, String dbName) throws DatabaseException {
        this.env = env;
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        dbConfig.setTransactional(false);
        dbConfig.setDeferredWrite(true);
        urlsDB = env.openDatabase(null, dbName, dbConfig);
        webURLBinding = new WebURLTupleBinding();
    }

    // Returns URLs to be crawled
    public List<WebURL> getNewURLs(int max) throws DatabaseException {
        synchronized (mutex) {
            int matches = 0;
            List<WebURL> results = new ArrayList<>(max);

            Cursor cursor = null;
            OperationStatus result;
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry value = new DatabaseEntry();
            try {
                cursor = urlsDB.openCursor(null, null);
                result = cursor.getFirst(key, value, null);

                while (matches < max && result == OperationStatus.SUCCESS) {
                    if (value.getData().length > 0) {
                        results.add(webURLBinding.entryToObject(value));
                        matches++;
                    }
                    result = cursor.getNext(key, value, null);
                }
            } catch (DatabaseException e) {
                throw e;
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            return results;
        }
    }

    // Deletes number of URLs from the crawling queue
    public void delete(int count) throws DatabaseException {
        synchronized (mutex) {
            int matches = 0;

            Cursor cursor = null;
            OperationStatus result;
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry value = new DatabaseEntry();

            try {
                cursor = urlsDB.openCursor(null, null);
                result = cursor.getFirst(key, value, null);

                while (matches < count && result == OperationStatus.SUCCESS) {
                    cursor.delete();
                    matches++;
                    result = cursor.getNext(key, value, null);
                }
            } catch (DatabaseException e) {
                throw e;
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    // Important method, return URLs database entry key that determines when it is going to be crawled
    // URLs priority is on first byte, smaller URL priority number equals to "crawl-as-soon-as-possible"
    // Second determinant is the page's depth on the crawl process (lower equals sooner to be crawled)
    // Last bytes match to URLs Doc ID -- the smaller the doc ID (ie. earlier put to Doc ID DB), sooner the URL will be crawled
    protected DatabaseEntry getDatabaseEntryKey(WebURL url) {
        byte[] keyData = new byte[6];
        keyData[0] = url.getPriority();
        keyData[1] = (url.getDepth() > Byte.MAX_VALUE ? Byte.MAX_VALUE : (byte) url.getDepth());
        ContentTypeUtil.putIntInByteArray(url.getDocid(), keyData, 2);
        return new DatabaseEntry(keyData);
    }

    // Puts new URL to queue
    public void putURLToQueue(WebURL url) throws DatabaseException {
        DatabaseEntry value = new DatabaseEntry();
        webURLBinding.objectToEntry(url, value);
        urlsDB.put(null, getDatabaseEntryKey(url), value);
    }

    // Returns amount of URLs on the crawl queue
    public long getCrawlQueueLength() {
        try {
            return urlsDB.count();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    // Synchronizes DB
    public void sync() {
        if (urlsDB == null) {
            return;
        }

        try {
            urlsDB.sync();
        } catch (DatabaseException e) {
            e.printStackTrace();
        }
    }

    // Closes the DB
    public void close() {
        try {
            urlsDB.close();
        } catch (DatabaseException e) {
            e.printStackTrace();
        }
    }
}
