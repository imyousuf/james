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

import junit.framework.TestCase;

public class ComposedJunkScoreTest extends TestCase {

    private final static String KEY1 = "KEY1";
    private final static double SCORE1 = 20.0;
    private final static String KEY2 = "KEY2";
    private final static double SCORE2 = 2.0;
    
    private JunkScore getJunkScore(String key, double score) {
        JunkScore junk = new JunkScoreImpl();
        if (key != null) {
            junk.setStoredScore(key, score);
        } 
        return junk;
    }
    
    public void testIllegalArguments() {
        boolean exception1 = false;
        boolean exception2 = false;
        boolean exception3 = false;
    
        try {
            JunkScore junk = new ComposedJunkScore(null,null);
        } catch (IllegalArgumentException e) {
            exception1 = true;
        }
        assertTrue("Exception thrown", exception1);
    
        try {
            JunkScore junk = new ComposedJunkScore(null,getJunkScore(null,0));
        } catch (IllegalArgumentException e) {
            exception2 = true;
        }
        assertTrue("Exception thrown", exception2);
    
        try {
            JunkScore junk = new ComposedJunkScore(getJunkScore(null,0),null);
        } catch (IllegalArgumentException e) {
            exception3 = true;
        }
        assertTrue("Exception thrown", exception3);
    
    }
    
    public void testComposedJunkScore() {
        JunkScore junk = new ComposedJunkScore(getJunkScore(KEY1, SCORE1), getJunkScore(KEY2, SCORE2));
    
        assertEquals("Summarize score", junk.getCompleteStoredScores(),SCORE1 + SCORE2, 0d);

        assertEquals("Get stored score", junk.getStoredScore(KEY1), SCORE1, 0d);
        assertEquals("Get stored score", junk.getStoredScore(KEY2), SCORE2, 0d);
    
        assertEquals("Get Map", junk.getStoredScores().size(), 2);
    
        assertEquals("Reset Score", junk.resetStoredScores(), SCORE1 + SCORE2, 0d);
    
        assertEquals("No Score", junk.getCompleteStoredScores(), 0.0, 0d);
        assertEquals("Empty Map", junk.getStoredScores().size(), 0);
    
    }
    
    public void testUnsuportedOperation() {
        boolean exception1 = false;
    
        JunkScore junk = new ComposedJunkScore(getJunkScore(KEY1, SCORE1), getJunkScore(KEY2, SCORE2));
        try {
            junk.setStoredScore(KEY1, SCORE1);
        } catch (UnsupportedOperationException e) {
            exception1 = true;
        }
        
        assertTrue("Unsupported operation", exception1);
    
    }
}
