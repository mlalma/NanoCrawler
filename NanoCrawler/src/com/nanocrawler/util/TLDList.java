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
package com.nanocrawler.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
 
// List of top level domains for separating subdomains / TLDs on WebURL
public class TLDList {
	
	private final String tldNamesFileName = "tld-names.txt";
	private Set<String> tldSet = new HashSet<>();

        // Constructor
	protected TLDList(String path) {
            try {
                InputStream stream = new FileInputStream(new File(path));
                if (stream == null) {
                    System.err.println("Couldn't find " + tldNamesFileName);
                    System.exit(-1);
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("//")) {
                        continue;
                    }
                    tldSet.add(line);
                }
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
	}
            
        // Checks if TLD is found from the given URL
	public boolean contains(String str) {
            return tldSet.contains(str);
	}

}
