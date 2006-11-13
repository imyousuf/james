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

public class JunkScoreImplTest extends TestCase {

    private final static String KEY1 = "KEY1";
    private final static double SCORE1 = 20.0;
    private final static String KEY2 = "KEY2";
    private final static double SCORE2 = 2.0;
    
    public void testJunkScoreImpl() {
        JunkScore junk = new JunkScoreImpl();
    
        assertEquals("Empty", junk.getCompleteStoredScores(),0.0);

        assertEquals("No previous stored score", junk.setStoredScore(KEY1, SCORE1), 0.0);
        assertEquals("No previous stored score", junk.setStoredScore(KEY2, SCORE1), 0.0);
    
        assertEquals("Return the previous stored score", junk.setStoredScore(KEY2, SCORE2), SCORE1);
    
        assertEquals("Summarize score", junk.getCompleteStoredScores(), SCORE1 + SCORE2);
    
        assertEquals("Get stored score", junk.getStoredScore(KEY1), SCORE1);
        assertEquals("Get stored score", junk.getStoredScore(KEY2), SCORE2);
    
        assertEquals("Get Map", junk.getStoredScores().size(), 2);
    
        assertEquals("Reset Score", junk.resetStoredScores(), SCORE1 + SCORE2);
     
        assertEquals("No Score", junk.getCompleteStoredScores(), 0.0);
        assertEquals("Empty Map", junk.getStoredScores().size(), 0);
    }
    
}
