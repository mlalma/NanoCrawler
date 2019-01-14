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
package com.nanocrawler.robotstxt;

import java.util.SortedSet;
import java.util.TreeSet;

// Sorted set of rules
public class RuleSet extends TreeSet<String> {
    private static final long serialVersionUID = 1L;

    @Override
    // Adds a new  element to tree set
    public boolean add(String str) {
        SortedSet<String> sub = headSet(str);
        if (!sub.isEmpty() && str.startsWith(sub.last())) {
            // No need to add; prefix is already present
            return false;
        }
        boolean retVal = super.add(str);
        sub = tailSet(str + "\0");
        while (!sub.isEmpty() && sub.first().startsWith(str)) {
            // Remove redundant entries
            sub.remove(sub.first());
        }
        return retVal;
    }

    // Checks for the string on 
    public boolean containsPrefixOf(String s) {
        SortedSet<String> sub = headSet(s);
        // Because redundant prefixes have been eliminated,
        // only a test against last item in headSet is necessary
        if (!sub.isEmpty() && s.startsWith(sub.last())) {
            return true;  // Prefix substring exists
        }
        // Might still exist exactly (headSet does not contain boundary)
        return contains(s);
    }
}
