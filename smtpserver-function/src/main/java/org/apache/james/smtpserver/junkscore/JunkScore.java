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




package org.apache.james.smtpserver.junkscore;

import java.util.Map;

public interface JunkScore {
    
    /**
     * The key for the JunkScore Object which holds scores per SMTPSession
     */
    public final String JUNK_SCORE_SESSION = "JUNK_SCORE_SESSION";
    
    /**
     * The key for the JunkScore Object which holds scores per mail
     */
    public final String JUNK_SCORE = "JUNK_SCORE";
    
    public final String JUNK_SCORE_SESSION_ATTR = "org.apache.james.junkscore.session";
    
    public final String JUNK_SCORE_ATTR = "org.apache.james.junkscore";
    
    public final String JUNK_SCORE_COMPOSED_ATTR = "org.apache.james.junkscore.composed";
    
    /**
     * Return the summary of stored scores
     * 
     * @return score the summary of all stored scores
     */
    public double getCompleteStoredScores();
    
    
    /**
     * Return a copy of the Map which contains the keys with the correspending scores
     * 
     * @return scoreMape the map which holds all keys and scores
     */
    public Map getStoredScores();
    
    /**
     * Return the score for the given key. Returns 0 if no score with the given key exist
     * 
     * @param key the key to get the score for
     * @return score the score
     */
    public double getStoredScore(String key);
    
    /**
     * Set the score for the given key. Return the previous stored score for the key.
     * 
     * @param key the key under which the score should be stored
     * @param score the store to score
     * @return oldScore the previous scored stored under the given key
     */
    public double setStoredScore(String key, double score);

    /**
     * Reset the all stored scores in the map. Return the summary of the previous stored scores.
     * 
     * @return oldScore the summary of the old score;
     */
    public double resetStoredScores();
}
