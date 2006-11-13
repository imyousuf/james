/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/


package org.apache.james.util.junkscore;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 
 * The Object which holds all junkscore
 */
public class JunkScoreImpl implements JunkScore {
    
    /**
     * The map to store the scores
     */
    private Map scoreMap = new HashMap();
    
    /**
     * @see org.apache.james.util.junkscore.JunkScore#getCompleteStoredScores()
     */
    public double getCompleteStoredScores() {
        double count = 0;
        Iterator s = scoreMap.keySet().iterator();
    
        while (s.hasNext()) {
            count = count + getStoredScore(s.next().toString());
        }
        return count;
    }
    
    
    /**
     * @see org.apache.james.util.junkscore.JunkScore#getStoredScores()
     */
    public Map getStoredScores() {
        // Get sure we return a copy of the Map so it can not get wrong objects added
        return new HashMap(scoreMap);
    }
    

    /**
     * @see org.apache.james.util.junkscore.JunkScore#getStoredScore(java.lang.String)
     */
    public double getStoredScore(String key) {
        Double s = (Double) scoreMap.get(key);
    
        if (s == null) {
            return 0;
        }
        return s.doubleValue();
    }
    
    /**
     * @see org.apache.james.util.junkscore.JunkScore#setStoredScore(java.lang.String, double)
     */
    public double setStoredScore(String key, double score) {
        Double s = (Double) scoreMap.put(key, new Double(score));
    
        if (s== null) {
            return 0;
        }
        return s.doubleValue();
    }

    /**
     * @see org.apache.james.util.junkscore.JunkScore#resetStoredScores()
     */
    public double resetStoredScores() {
        double oldSum = getCompleteStoredScores();
        scoreMap.clear();
        return oldSum;
    }

}
