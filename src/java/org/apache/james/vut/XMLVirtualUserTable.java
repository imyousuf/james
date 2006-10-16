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



package org.apache.james.vut;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.james.util.VirtualUserTableUtil;

public class XMLVirtualUserTable extends AbstractVirtualUserTable implements Configurable {
    /**
     * Holds the configured mappings
     */
    private Map mappings = new HashMap();
    
    
    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(org.apache.avalon.framework.configuration.Configuration)
     */
    public void configure(Configuration arg0) throws ConfigurationException {
        Configuration mapConf = arg0.getChild("mapping");
        if (mapConf != null) {
            mappings = VirtualUserTableUtil.getXMLMappings(mapConf.getValue());
        } else {
            throw new ConfigurationException("No mapping configured");
        }
    
    }
    
    /**
     * Not implemented
     */
    public boolean addMappingInternal(String user, String domain, String mapping) {
        // Not supported
        return false;
    }

    /**
     * @see org.apache.james.vut.AbstractVirtualUserTable#mapAddress(java.lang.String, java.lang.String)
     */
    protected String mapAddress(String user, String domain) {
        return VirtualUserTableUtil.getTargetString(user, domain, mappings);
    }

    /**
     * Not implemented
     */
    public boolean removeMappingInternal(String user, String domain, String mapping) {
        // Not supported
        return false;
    }

    /**
     * Not implemented
     */
    public Collection getUserDomainMappings(String user, String domain) {
    // Not supported
    return null;
    }

}
