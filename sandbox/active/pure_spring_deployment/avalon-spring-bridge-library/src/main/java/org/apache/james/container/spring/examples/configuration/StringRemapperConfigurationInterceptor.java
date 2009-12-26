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
package org.apache.james.container.spring.examples.configuration;

import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.MutableConfiguration;
import org.apache.commons.lang.StringUtils;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * re-maps arbitrary string values. handles pure literal Strings, not Regular Expressions
 */
public class StringRemapperConfigurationInterceptor extends TraversingConfigurationInterceptor {
    
    private final Map<String,String> mappings = new LinkedHashMap<String,String>();

    /**
     * @param mappings - Map<String, String>, key being the string to be replaced, value being the replacement
     */
    public void setMappings(Map<String,String> mappings) {
        this.mappings.putAll(mappings);
    }

    protected void process(MutableConfiguration mutableConfiguration) {

        processAttributes(mutableConfiguration);
        processValue(mutableConfiguration);
    }

    private void processAttributes(MutableConfiguration mutableConfiguration) {
        String[] attributeNames = mutableConfiguration.getAttributeNames();
        for (int i = 0; i < attributeNames.length; i++) {
            String attributeName = attributeNames[i];
            String attributeValue = null;
            try {
                attributeValue = mutableConfiguration.getAttribute(attributeName);
            } catch (ConfigurationException e) {
                continue; // if value is not accessible, skip it
            }

            Iterator<String> iterator = mappings.keySet().iterator();
            while (iterator.hasNext()) {
                String find = iterator.next();
                String replacement = mappings.get(find);
                String replaced = StringUtils.replace(attributeValue, find, replacement);
                mutableConfiguration.setAttribute(attributeName, replaced);
            }
        }
    }

    private void processValue(MutableConfiguration mutableConfiguration) {
        String value = null;
        try {
            value = mutableConfiguration.getValue();
        } catch (ConfigurationException e) {
            return; // if value is inaccessible, we better skip it
        }
        if (value == null) return;

        Iterator<String> iterator = mappings.keySet().iterator();
        while (iterator.hasNext()) {
            String find =  iterator.next();
            String replacement = (String) mappings.get(find);
            String replaced = StringUtils.replace(value, find, replacement);
            mutableConfiguration.setValue(replaced);
        }
    }
}