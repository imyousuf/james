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

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.mail.internet.ParseException;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.services.DNSServer;
import org.apache.james.services.DomainList;
import org.apache.james.services.VirtualUserTable;
import org.apache.james.services.VirtualUserTableManagement;
import org.apache.james.util.DomainListUtil;
import org.apache.james.util.VirtualUserTableUtil;
import org.apache.mailet.MailAddress;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Perl5Compiler;

/**
 * 
 */
public abstract class AbstractVirtualUserTable extends AbstractLogEnabled
    implements VirtualUserTable, VirtualUserTableManagement, DomainList, Serviceable {
    
    private boolean autoDetect = true;
    private boolean autoDetectIP = true;
    private DNSServer dns;

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(org.apache.avalon.framework.service.ServiceManager)
     */
    public void service(ServiceManager arg0) throws ServiceException {
        dns = (DNSServer)arg0.lookup(DNSServer.ROLE); 
    }
    
    
    /**
     * @see org.apache.james.services.VirtualUserTable#getMapping(org.apache.mailet.MailAddress)
     */
    public Collection getMappings(String user,String domain) throws ErrorMappingException {

        String targetString = mapAddress(user, domain);
        
        // Only non-null mappings are translated
        if (targetString != null) {
            Collection mappings = new ArrayList();
            if (targetString.startsWith(VirtualUserTable.ERROR_PREFIX)) {
                throw new ErrorMappingException(targetString.substring(VirtualUserTable.ERROR_PREFIX.length()));

            } else {
                Iterator map= VirtualUserTableUtil.getMappings(targetString).iterator();

                while (map.hasNext()) {
                    String target;
                    String targetAddress = map.next().toString();

                    if (targetAddress.startsWith(VirtualUserTable.REGEX_PREFIX)) {
                        try {
                            targetAddress = VirtualUserTableUtil.regexMap(new MailAddress(user,domain), targetAddress);
                        } catch (MalformedPatternException e) {
                            getLogger().error("Exception during regexMap processing: ", e);
                        } catch (ParseException e) {
                            // should never happen
                            getLogger().error("Exception during regexMap processing: ", e);
                        } 

                        if (targetAddress == null) continue;
                    }
                    
                    /* The VirtualUserTable not know anything about the defaultDomain. The defaultDomain should be added by the service which use 
                     * the VirtualUserTable
                     * 
                    if (targetAddress.indexOf('@') < 0) {
                         target = targetAddress + "@localhost";
                    } else {
                        target = targetAddress;
                    }
                    */
                    
                    target = targetAddress;
            
                    // add mapping
                    mappings.add(target);

                    StringBuffer buf = new StringBuffer().append("Valid virtual user mapping ")
                                                         .append(user).append("@").append(domain)
                                                         .append(" to ").append(targetAddress);
                    getLogger().debug(buf.toString());

                 }
            }
            return mappings;
        }
        return null;
    }
    
    /**
     * @see org.apache.james.services.VirtualUserTableManagement#addRegexMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public synchronized boolean addRegexMapping(String user, String domain, String regex) throws InvalidMappingException {     
        try {
            new Perl5Compiler().compile(regex);
        } catch (MalformedPatternException e) {
            throw new InvalidMappingException("Invalid regex: " + regex);
        }
        
        if (checkMapping(user,domain,regex) == true) {
            getLogger().info("Add regex mapping => " + regex + " for user: " + user + " domain: " + domain);
            return addMappingInternal(user, domain, VirtualUserTable.REGEX_PREFIX + regex);
        } else {
            return false;
        }
    }

    
    /**
     * @throws InvalidMappingException 
     * @see org.apache.james.services.VirtualUserTableManagement#removeRegexMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public synchronized boolean removeRegexMapping(String user, String domain, String regex) throws InvalidMappingException {
        getLogger().info("Remove regex mapping => " + regex + " for user: " + user + " domain: " + domain);
        return removeMappingInternal(user,domain,VirtualUserTable.REGEX_PREFIX + regex);
    }
    
    /**
     * @see org.apache.james.services.VirtualUserTableManagement#addAddressMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public synchronized boolean addAddressMapping(String user, String domain, String address) throws InvalidMappingException {
        if (address.indexOf('@') < 0) {
            address =  address + "@localhost";
        } 
        try {
            new MailAddress(address);
        } catch (ParseException e) {
            throw new InvalidMappingException("Invalid emailAddress: " + address);
        }
        if (checkMapping(user,domain,address) == true) {          
            getLogger().info("Add address mapping => " + address + " for user: " + user + " domain: " + domain);
            return addMappingInternal(user, domain, address);
        } else {
            return false;
        }   
    }
    
    /**
     * @see org.apache.james.services.VirtualUserTableManagement#removeAddressMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public synchronized boolean removeAddressMapping(String user, String domain, String address) throws InvalidMappingException {
        if (address.indexOf('@') < 0) {
            address =  address + "@localhost";
        } 
        getLogger().info("Remove address mapping => " + address + " for user: " + user + " domain: " + domain);
        return removeMappingInternal(user,domain,address);
    }
    
    /**
     * @see org.apache.james.services.VirtualUserTableManagement#addErrorMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public synchronized boolean addErrorMapping(String user, String domain, String error) throws InvalidMappingException {   
        if (checkMapping(user,domain,error) == true) {          
            getLogger().info("Add error mapping => " + error + " for user: " + user + " domain: " + domain);
            return addMappingInternal(user,domain, VirtualUserTable.ERROR_PREFIX + error);
        } else {
            return false;
        } 
    }
    
    /**
     * @see org.apache.james.services.VirtualUserTableManagement#removeErrorMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public synchronized boolean removeErrorMapping(String user, String domain, String error) throws InvalidMappingException {
        getLogger().info("Remove error mapping => " + error + " for user: " + user + " domain: " + domain);     
        return removeMappingInternal(user,domain,VirtualUserTable.ERROR_PREFIX + error);
    }


    /**
     * @see org.apache.james.services.VirtualUserTableManagement#addMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean addMapping(String user, String domain, String mapping) throws InvalidMappingException {
        String map = mapping.toLowerCase();
        
        if (map.startsWith(VirtualUserTable.ERROR_PREFIX)) {
            return addErrorMapping(user,domain,map.substring(VirtualUserTable.ERROR_PREFIX.length()));
        } else if (map.startsWith(VirtualUserTable.REGEX_PREFIX)) {
            return addRegexMapping(user,domain,map.substring(VirtualUserTable.REGEX_PREFIX.length()));
        } else {
            return addAddressMapping(user,domain,map);
        }
    }
    
    /**
     * @see org.apache.james.services.VirtualUserTableManagement#removeMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean removeMapping(String user, String domain, String mapping) throws InvalidMappingException {
        String map = mapping.toLowerCase();
    
        if (map.startsWith(VirtualUserTable.ERROR_PREFIX)) {
            return removeErrorMapping(user,domain,map.substring(VirtualUserTable.ERROR_PREFIX.length()));
        } else if (map.startsWith(VirtualUserTable.REGEX_PREFIX)) {
            return removeRegexMapping(user,domain,map.substring(VirtualUserTable.REGEX_PREFIX.length()));
        } else {
            return removeAddressMapping(user,domain,map);
        }
    }
    
    /**
     * @see org.apache.james.services.VirtualUserTableManagement#getAllMappings()
     */
    public Map getAllMappings() {
        int count = 0;
        Map mappings = getAllMappingsInternal();
    
        if (mappings != null) {
            count = mappings.size();
        }
        getLogger().debug("Retrieve all mappings. Mapping count: " + count);
        return mappings;
    }
    
    
   private boolean checkMapping(String user,String domain, String mapping) {
       Collection mappings = getUserDomainMappings(user,domain);
       if (mappings != null && mappings.contains(mapping)) {
           return false;
       } else {
           return true;
       }
   }

 
    /**
     * @see org.apache.james.services.DomainList#getDomains()
     */
    public List getDomains() {
        List domains = getDomainsInternal();
        if (domains != null) {
            
            String hostName = null;
            try {
                hostName = dns.getHostName(dns.getLocalHost());
            } catch  (UnknownHostException ue) {
                hostName = "localhost";
            }
            
            getLogger().info("Local host is: " + hostName);
            
            if (autoDetect == true && (!hostName.equals("localhost"))) {
                domains.add(hostName.toLowerCase(Locale.US));
            }

            
            if (autoDetectIP == true) {
                domains.addAll(DomainListUtil.getDomainsIP(domains,dns,getLogger()));
            }
       
            if (getLogger().isInfoEnabled()) {
                for (Iterator i = domains.iterator(); i.hasNext(); ) {
                    getLogger().info("Handling mail for: " + i.next());
                }
            }  
            return domains;
        } else {
            return null;
        }
    }
    
    /**
     * @see org.apache.james.services.VirtualUserTableManagement#getUserDomainMappings(java.lang.String, java.lang.String)
     */
    public Collection getUserDomainMappings(String user, String domain) {
        return getUserDomainMappingsInternal(user, domain);
    }
    
    /**
     * @see org.apache.james.services.DomainList#setAutoDetect(boolean)
     */
    public synchronized void setAutoDetect(boolean autoDetect) {
        getLogger().info("Set autodetect to: " + autoDetect);
        this.autoDetect = autoDetect;
    }
    
    /**
     * @see org.apache.james.services.DomainList#setAutoDetectIP(boolean)
     */
    public synchronized void setAutoDetectIP(boolean autoDetectIP) {
        getLogger().info("Set autodetectIP to: " + autoDetectIP);
        this.autoDetectIP = autoDetectIP;
    }

    /**
     * Override to map virtual recipients to real recipients, both local and non-local.
     * Each key in the provided map corresponds to a potential virtual recipient, stored as
     * a <code>MailAddress</code> object.
     * 
     * Translate virtual recipients to real recipients by mapping a string containing the
     * address of the real recipient as a value to a key. Leave the value <code>null<code>
     * if no mapping should be performed. Multiple recipients may be specified by delineating
     * the mapped string with commas, semi-colons or colons.
     * 
     * @param recipient the mapping of virtual to real recipients, as 
     *    <code>MailAddress</code>es to <code>String</code>s.
     */
    protected abstract String mapAddress(String user, String domain);
    
    /**
     * Add new mapping
     * 
     * @param user the user
     * @param domain the domain
     * @param mapping the mapping
     * @return true if successfully
     * @throws InvalidMappingException 
     */
    protected abstract boolean  addMappingInternal(String user, String domain, String mapping) throws InvalidMappingException;
    
    /**
     * Remove mapping 
     * 
     * @param user the user
     * @param domain the domain
     * @param mapping the mapping 
     * @return true if successfully
     * @throws InvalidMappingException 
     */
    protected abstract boolean  removeMappingInternal(String user, String domain, String mapping) throws InvalidMappingException;

    /**
     * Return List of all domains for which email should accepted
     * 
     * @return domains  the domains
     */
    protected abstract List getDomainsInternal();
    
    /**
     * Return Collection of all mappings for the given username and domain
     * 
     * @param user the user
     * @param domain the domain
     * @return Collection which hold the mappings
     */
    protected abstract Collection getUserDomainMappingsInternal(String user, String domain);

    /**
     * Return a Map which holds all Mappings
     * 
     * @return Map
     */
    protected abstract Map getAllMappingsInternal();
}
