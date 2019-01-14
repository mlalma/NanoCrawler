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
import com.nanocrawler.util.CrawlConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.OperationStatus;
import org.apache.log4j.Logger;

// Document ID server, keeps track of URLs that have been already crawled
public class DocIDServer {
    private static final Logger logger = Logger.getLogger(DocIDServer.class.getName());

    private Database docIDsDB = null;
    private final Object mutex = new Object();
    private int lastDocID;
    private final CrawlConfig config;

    // Constructor
    public DocIDServer(Environment env, CrawlConfig config) throws DatabaseException {
        this.config = config;
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        dbConfig.setTransactional(false);
        dbConfig.setDeferredWrite(true);
        docIDsDB = env.openDatabase(null, "DocIDs", dbConfig);
        lastDocID = 0;
    }

    // Returns the docid of an already added url
    public int getDocId(String url) {
        synchronized (mutex) {
            OperationStatus result;
            DatabaseEntry value = new DatabaseEntry();
            DatabaseEntry key = new DatabaseEntry(url.getBytes());
            result = docIDsDB.get(null, key, value, null);

            if (result == OperationStatus.SUCCESS && value.getData().length > 0) {
                return ContentTypeUtil.byteArray2Int(value.getData());
            } else {
                return -1;
            }
        }
    }

    // Gets existing doc ID or creates new one if URL is not on the DocDB
    public int createOrGetNewDocID(String url) {
        synchronized (mutex) {
            // Make sure that we have not already assigned a docid for this URL
            int docid = getDocId(url);
            if (docid > 0) {
                return docid;
            }

            lastDocID++;
            docIDsDB.put(null, new DatabaseEntry(url.getBytes()), new DatabaseEntry(ContentTypeUtil.int2ByteArray(lastDocID)));
            return lastDocID;
        }
    }

    // Adds URL with a specific doc ID -- the docId has to be bigger than current lastDocId
    public void addUrlAndDocId(String url, int docId) throws Exception {
        synchronized (mutex) {
            if (docId <= lastDocID) {
                throw new Exception("Requested doc id: " + docId + " is not larger than: " + lastDocID);
            }

            // Make sure that we have not already assigned a docid for this URL
            int prevDocid = getDocId(url);
            if (prevDocid > 0) {
                if (prevDocid == docId) {
                    return;
                }
                throw new Exception("Doc id: " + prevDocid + " is already assigned to URL: " + url);
            }

            docIDsDB.put(null, new DatabaseEntry(url.getBytes()), new DatabaseEntry(ContentTypeUtil.int2ByteArray(docId)));
            lastDocID = docId;
        }
    }

    // Check if URL is already on DB
    public boolean isSeenBefore(String url) {
        return getDocId(url) != -1;
    }

    // Returns of amount of URLs (DB entries) there are
    public int getDocCount() {
        try {
            return (int) docIDsDB.count();
        } catch (DatabaseException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // Syncs the database, i.e. writes cached information to disk
    public void sync() {
        try {
            docIDsDB.sync();
        } catch (DatabaseException e) {
            e.printStackTrace();
        }
    }

    // Closes the database
    public void close() {
        try {
            docIDsDB.close();
        } catch (DatabaseException e) {
            e.printStackTrace();
        }
    }
}
