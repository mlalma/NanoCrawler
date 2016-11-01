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

import java.util.concurrent.TimeUnit;
import org.apache.http.impl.conn.PoolingClientConnectionManager;

// Closes long-running HTTP connections for releasing I/O resources
public class IdleConnectionMonitorThread extends Thread {    
    
    private final PoolingClientConnectionManager connMgr;
    private volatile boolean shutdown;

    // Constructor
    public IdleConnectionMonitorThread(PoolingClientConnectionManager connMgr) {
        super("Connection Manager");
        this.connMgr = connMgr;
    }

    @Override
    // Perioidical checker of closing idle connections
    public void run() {
        try {
            while (!shutdown) {
                synchronized (this) {
                    wait(5000);
                    connMgr.closeExpiredConnections();
                    connMgr.closeIdleConnections(30, TimeUnit.SECONDS);
                }
            }
        } catch (InterruptedException ex) {
            // terminate
        }
    }
    
    // Called upon shutdown
    public void shutdown() {
        shutdown = true;
        synchronized (this) {
            notifyAll();
        }
    }   
}

