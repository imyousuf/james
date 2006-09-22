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




package org.apache.james.management;

import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Compiler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * immutual collection of filters used to specify which mail should be processed by SpoolManagement
 * criterias are:
 * exact state match
 * headerValue regular expressions match all related headers 
 */
public class SpoolFilter {

    public static final String ERROR_STATE = "error";
    
    public static final SpoolFilter ERRORMAIL_FILTER = new SpoolFilter(ERROR_STATE);

    private String state = null;

    /**
     * Map<String headerName, String headerValueRegex>
     */
    private final Map headerFilters = new HashMap();

    /**
     * Map<String headerName, Pattern>
     */
    private final Map headerFiltersCompiled = new HashMap();

    /**
     * Construct the SpoolFilter
     * 
     * @param state the message state on which message the filter should be used
     * @param header the headername on which the given regex should be used
     * @param headerValueRegex the regex to use on the value of the given header
     */
    public SpoolFilter(String state, String header, String headerValueRegex) {
        this.state = state;
        if (header != null) headerFilters.put(header, headerValueRegex);
    }

    /**
     * Construct the SpoolFilter
     * 
     * @param state the message state on which message the filter should be used
     */
    public SpoolFilter(String state) {
        this.state = state;
    }

    /**
     * Construct the SpoolFilter
     * 
     * @param header the headername on which the given regex should be used
     * @param headerValueRegex the regex to use on the value of the given header
     */
    public SpoolFilter(String header, String headerValueRegex) {
        this.state = null;
        if (header != null) headerFilters.put(header, headerValueRegex);
    }

    /**
     * Construct the SpoolFilter
     * 
     * @param state the message state on which message the filter should be used
     * @param headerFilters a Map which contains filters to use
     */
    public SpoolFilter(String state, Map headerFilters) {
        this.state = state;
        this.headerFilters.putAll(headerFilters);
        this.headerFilters.remove(null); // NULL is not acceptable here
    }

    public boolean doFilter() {
        return doFilterHeader() || doFilterState();
    }

    /**
     * Return true if any state was given on init the SpoolFilter
     * 
     * @return true if state was given on init. False otherwise 
     */
    public boolean doFilterState() {
        return state != null;
    }

    /**
     * Return true if any filters should be used on the headers
     * 
     * @return true if filters should be used. False if not
     */
    public boolean doFilterHeader() {
        return headerFilters.size() > 0;
    }

    /**
     * Return the message state on which the filter will be used
     * 
     * @return state the message state
     */
    public String getState() {
        return state;
    }

    /**
     * Return an Iterator which contains all headers which should be filtered
     * 
     * @return headers an Iterator which contains all headers which should be filtered
     */
    public Iterator getHeaders() {
        return headerFilters.keySet().iterator();
    }

    /**
     * Return the regex which should be used on the given header. 
     * Return null if the header not exists in the Map
     * 
     * @param header the headername for which the regex should be retrieven. 
     * @return regex the regex for the given header
     */
    public String getHeaderValueRegex(String header) {
        return (String) headerFilters.get(header);
    }

    /**
     * Return the compiled Pattern for the given header. 
     * Return null if the header not exists in the Map
     * 
     * @param header the header for which the pattern should be returned
     * @return pattern the Pattern which was compiled for the given header
     * @throws SpoolManagementException get thrown if an invalid regex is used with the given header
     */
    public Pattern getHeaderValueRegexCompiled(String header) throws SpoolManagementException {
        Perl5Compiler compiler = new Perl5Compiler();

        // try to reuse cached pattern
        if (headerFiltersCompiled.get(header) != null) return (Pattern)headerFiltersCompiled.get(header); 

        String headerValueRegex = getHeaderValueRegex(header);
        if (headerValueRegex == null) return null;
        try {
            Pattern pattern = compiler.compile(headerValueRegex, Perl5Compiler.READ_ONLY_MASK);
            headerFiltersCompiled.put(header, pattern); // cache
            return pattern;
        } catch (MalformedPatternException e) {
            throw new SpoolManagementException("failed to compile regular expression", e);
        }
    }
    
}
