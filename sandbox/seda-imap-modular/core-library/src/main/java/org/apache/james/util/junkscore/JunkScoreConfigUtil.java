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

/**
 * Utility class for providing static method for JunkScore configuration
 */
public class JunkScoreConfigUtil {

    public static final String JUNKSCORE = "junkscore";
    public static final String JUNKSCORE_DELIMITER = ":";
    
    /**
     * Return the junkscore which was supplied as raw config String
     * 
     * @param raw configuration String
     * @return junkScore
     * @throws IllegalArgumentException get thrown on invalid config
     */
    public static double getJunkScore(String raw) throws IllegalArgumentException {
        double score = 0;
        raw = raw.toLowerCase();
        if (raw.indexOf(JUNKSCORE_DELIMITER) > 0 && raw.startsWith(JUNKSCORE)) {
            try {
                score = Double.parseDouble(raw.trim().split(JUNKSCORE_DELIMITER)[1]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid input: " + raw);
            }
        } else {
            throw new IllegalArgumentException("Invalid input: " + raw);
        }
        return score;
    }
    
    
    /**
     * Return true if the given raw config is a valid JunkScore configuration String
     * 
     * @param raw configuration String
     * @return true of false
     */
    public static boolean isValidJunkScoreConfig(String raw) {
        try {
            getJunkScore(raw);
        } catch (IllegalArgumentException e){
            return false;
        }
        return true;
    }
}
