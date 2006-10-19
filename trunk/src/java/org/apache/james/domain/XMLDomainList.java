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



package org.apache.james.domain;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;

/**
 * Mimic the old behavoir of JAMES
 */
public class XMLDomainList extends AbstractDomainList implements Configurable {
    private List serverNames;

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(org.apache.avalon.framework.configuration.Configuration)
     */
    public void configure(Configuration arg0) throws ConfigurationException {
        Configuration conf = arg0.getChild("servernames");
        
        if (conf != null) {
            serverNames = new ArrayList();
        
            String hostName = null;
            try {
                hostName = getDNSServer().getHostName(InetAddress.getLocalHost());
            } catch  (UnknownHostException ue) {
                hostName = "localhost";
            }

            getLogger().info("Local host is: " + hostName);
    
            if (conf.getAttributeAsBoolean("autodetect") && (!hostName.equals("localhost"))) {
                serverNames.add(hostName.toLowerCase(Locale.US));
            }

            Configuration[] serverNameConfs = conf.getChildren( "servername" );
            for ( int i = 0; i < serverNameConfs.length; i++ ) {
                serverNames.add( serverNameConfs[i].getValue().toLowerCase(Locale.US));

                if (conf.getAttributeAsBoolean("autodetectIP", true)) {
                    serverNames.addAll(getDomainsIP(serverNames));
                }
            }
            if (serverNames.isEmpty()) {
                throw new ConfigurationException( "Fatal configuration error: no servernames specified!");
            }
        } 
    }
    
    /**
     * @see org.apache.james.domain.AbstractDomainList#getInternalDomainList()
     */
    protected List getInternalDomainList() {
        return serverNames;
    }

    /**
     * @see org.apache.james.services.DomainList#containsDomain(java.lang.String)
     */
    public boolean containsDomain(String domains) {
        return serverNames.contains(domains);
    }
}
