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
import java.util.Map;

/**
 * Class to compose the two Junkscore Objects 
 */
public class ComposedJunkScore implements JunkScore {

    JunkScore score1;
    
    JunkScore score2;
    
    /**
     * Construct Class
     * 
     * @param score1 The JunkScore for the whole session
     * @param score2 The JunkScore for the nail
     * @throws IllegalArgumentException get thrown if one of the given JunkScore Objects is null
     */
    public ComposedJunkScore (JunkScore score1, JunkScore score2) throws IllegalArgumentException {
        if (score1 == null || score2 == null ) throw new IllegalArgumentException("JunkScore can not be null");
        this.score1 = score1;
        this.score2 = score2;
    }
    
    /** 
     * @see org.apache.james.util.junkscore.JunkScore#getCompleteStoredScores()
     */
    public double getCompleteStoredScores() {
        return (score1.getCompleteStoredScores() + score2.getCompleteStoredScores());
    }

    
    /**
     * @see org.apache.james.util.junkscore.JunkScore#getStoredScore(java.lang.String)
     */
    public double getStoredScore(String key) {
        return (score1.getStoredScore(key) + score2.getStoredScore(key));
    }

    /**
     * @see org.apache.james.util.junkscore.JunkScore#getStoredScores()
     */
    public Map getStoredScores() {
        // copy the Map
        Map m = new HashMap(score1.getStoredScores());
        m.putAll(score2.getStoredScores());
        return m;
    }

    /**
     * @see org.apache.james.util.junkscore.JunkScore#resetStoredScores()
     */
    public double resetStoredScores() {
        return (score1.resetStoredScores() + score2.resetStoredScores());
    }

    /**
     * Throws an UnsuportedOperationException cause its not supported here
     * 
     * @throws UnsupportedOperationException
     */
    public double setStoredScore(String key, double score) {
    throw new UnsupportedOperationException("Unimplemented Method");
    }
    
}
