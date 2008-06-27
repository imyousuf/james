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
    
    private List domainNames = null;
    
    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(org.apache.avalon.framework.configuration.Configuration)
     */
    public void configure(Configuration arg0) throws ConfigurationException {
        Configuration conf = arg0.getChild("domainnames");      
        if (conf != null) {

            Configuration[] serverNameConfs = conf.getChildren( "domainname" );
            for ( int i = 0; i < serverNameConfs.length; i++ ) {
                addDomainInternal( serverNameConfs[i].getValue());
            }
            
            Configuration autoConf = arg0.getChild("autodetect");
            if (autoConf != null) {
                setAutoDetect(autoConf.getValueAsBoolean(true));    
            }
            
            Configuration autoIPConf = arg0.getChild("autodetectIP");
            if (autoConf != null) {
                setAutoDetectIP(autoIPConf.getValueAsBoolean(true));    
            }
        } 
    }
    
    
    /**
     * @see org.apache.james.domain.AbstractDomainList#getDomainListInternal()
     */
    protected List getDomainListInternal() {
        return domainNames;
    }

    /**
     * @see org.apache.james.services.DomainList#containsDomain(java.lang.String)
     */
    public boolean containsDomain(String domains) {
        if (domainNames == null) return false;
        return domainNames.contains(domains);
    }

    /**
     * @see org.apache.james.domain.AbstractDomainList#addDomainInternal(java.lang.String)
     */
    protected boolean addDomainInternal(String domain) {
        if (domainNames == null) {
            domainNames = new ArrayList();
        }
    
        String newDomain = domain.toLowerCase(Locale.US);
        if (containsDomain(newDomain) == false) {
            domainNames.add(newDomain);
            return true;
        } else {
            return false;
        }
    }

    /**
     * @see org.apache.james.domain.AbstractDomainList#removeDomainInternal(java.lang.String)
     */
    protected boolean removeDomainInternal(String domain) {
        if (domainNames == null) return false;
        return domainNames.remove(domain.toLowerCase(Locale.US));
    }
}
