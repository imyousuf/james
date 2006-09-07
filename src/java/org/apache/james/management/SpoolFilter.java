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

    public SpoolFilter(String state, String header, String headerValueRegex) {
        this.state = state;
        if (header != null) headerFilters.put(header, headerValueRegex);
    }

    public SpoolFilter(String state) {
        this.state = state;
    }

    public SpoolFilter(String header, String headerValueRegex) {
        this.state = null;
        if (header != null) headerFilters.put(header, headerValueRegex);
    }

    /**
     * @param state
     * @param headerFilters Map<String headerName, String headerValueRegex>
     */
    public SpoolFilter(String state, Map headerFilters) {
        this.state = state;
        this.headerFilters.putAll(headerFilters);
        this.headerFilters.remove(null); // NULL is not acceptable here
    }

    public boolean doFilter() {
        return doFilterHeader() || doFilterState();
    }

    public boolean doFilterState() {
        return state != null;
    }

    public boolean doFilterHeader() {
        return headerFilters.size() > 0;
    }

    public String getState() {
        return state;
    }

    public Iterator getHeaders() {
        return headerFilters.keySet().iterator();
    }

    public String getHeaderValueRegex(String header) {
        return (String) headerFilters.get(header);
    }

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
