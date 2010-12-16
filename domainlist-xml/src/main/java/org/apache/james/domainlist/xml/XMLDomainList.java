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



package org.apache.james.domainlist.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.domainlist.lib.AbstractDomainList;
import org.apache.james.lifecycle.api.Configurable;


/**
 * Mimic the old behavoir of JAMES
 */
public class XMLDomainList extends AbstractDomainList implements Configurable{
    
    private List<String> domainNames = new ArrayList<String>();
    
    private boolean managementDisabled = false;
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.lifecycle.Configurable#configure(org.apache.commons.configuration.HierarchicalConfiguration)
     */
    @SuppressWarnings("unchecked")
    public void configure(HierarchicalConfiguration config) throws ConfigurationException {
        super.configure(config);
        List<String> serverNameConfs = config.getList( "domainnames.domainname" );
        for ( int i = 0; i < serverNameConfs.size(); i++ ) {
            addDomainInternal( serverNameConfs.get(i));
        }
        
    }
   
   
    
    
    /**
     * @see org.apache.james.domainlist.lib.AbstractDomainList#getDomainListInternal()
     */
    protected List<String> getDomainListInternal() {
        // TODO: Remove temporary fix!
        // This is set to true to get sure now new domain can get added or removed
        // after the domains were retrieved by James.java. See is a workaround!
        managementDisabled = true;
        return new ArrayList<String>(domainNames);
    }

    /**
     * @see org.apache.james.domainlist.api.DomainList#containsDomain(java.lang.String)
     */
    public boolean containsDomain(String domains) {
        return domainNames.contains(domains);
    }

    /**
     * The added domains will only added in memory!
     * 
     * @see org.apache.james.domainlist.lib.AbstractDomainList#addDomainInternal(java.lang.String)
     */
    protected boolean addDomainInternal(String domain) {
        // TODO: Remove later. Temporary fix to get sure no domains can be added to the XMLDomainList
        if (managementDisabled) throw new UnsupportedOperationException("Management not supported");
        
        String newDomain = domain.toLowerCase(Locale.US);
        if (containsDomain(newDomain) == false) {
            domainNames.add(newDomain);
            return true;
        } else {
            return false;
        }
    }

    /**
     * @see org.apache.james.domainlist.lib.AbstractDomainList#removeDomainInternal(java.lang.String)
     */
    protected boolean removeDomainInternal(String domain) {
        // TODO: Remove later. Temporary fix to get sure no domains can be added to the XMLDomainList
        if (managementDisabled) throw new UnsupportedOperationException("Management not supported");
       
        return domainNames.remove(domain.toLowerCase(Locale.US));
    }
}
