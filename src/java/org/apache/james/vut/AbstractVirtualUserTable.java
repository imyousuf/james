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
import java.util.StringTokenizer;

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
            if (targetString.startsWith("error:")) {
                throw new ErrorMappingException(targetString.substring("error:".length()));

            } else {
                Iterator map= VirtualUserTableUtil.getMappings(targetString).iterator();

                while (map.hasNext()) {
                    String target;
                    String targetAddress = map.next().toString();

                    if (targetAddress.startsWith("regex:")) {
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
            
                    if (targetAddress.indexOf('@') < 0) {
                         target = targetAddress + "@localhost";
                    } else {
                        target = targetAddress;
                    }
            
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
        getLogger().info("Add regex mapping => " + regex + " for user: " + user + " domain: " + domain);
        try {
            new Perl5Compiler().compile(regex);
        } catch (MalformedPatternException e) {
            throw new InvalidMappingException("Invalid regex: " + regex);
        }
        return addMappingInternal(user, domain, "regex:" + regex);
    }

    
    /**
     * @throws InvalidMappingException 
     * @see org.apache.james.services.VirtualUserTableManagement#removeRegexMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public synchronized boolean removeRegexMapping(String user, String domain, String regex) throws InvalidMappingException {
        getLogger().info("Add regex mapping => " + regex + " for user: " + user + " domain: " + domain);
        return removeMappingInternal(user,domain,"regex:" + regex);
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
        getLogger().info("Add address mapping => " + address + " for user: " + user + " domain: " + domain);
        return addMappingInternal(user, domain, address);
    }
    
    /**
     * @throws InvalidMappingException 
     * @see org.apache.james.services.VirtualUserTableManagement#removeAddressMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public synchronized boolean removeAddressMapping(String user, String domain, String address) throws InvalidMappingException {

        if (address.indexOf('@') < 0) {
            address =  address + "@localhost";
        } 
        getLogger().info("Add address mapping => " + address + " for user: " + user + " domain: " + domain);
        return removeMappingInternal(user,domain,address);
    }
    
    /**
     * @throws InvalidMappingException 
     * @see org.apache.james.services.VirtualUserTableManagement#addErrorMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public synchronized boolean addErrorMapping(String user, String domain, String error) throws InvalidMappingException {   
        getLogger().info("Add error mapping => " + error + " for user: " + user + " domain: " + domain);
        
        return addMappingInternal(user,domain, "error:" + error);
    }
    
    /**
     * @throws InvalidMappingException 
     * @see org.apache.james.services.VirtualUserTableManagement#removeErrorMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public synchronized boolean removeErrorMapping(String user, String domain, String error) throws InvalidMappingException {
        getLogger().info("Add error mapping => " + error + " for user: " + user + " domain: " + domain);     
    
        return removeMappingInternal(user,domain,"error:" + error);
    }


    /**
     * Convert a raw mapping String to a Collection
     * 
     * @param rawMapping the mapping Strin
     * @return map a collection which holds all mappings
     */
    protected ArrayList mappingToCollection(String rawMapping) {
        ArrayList map = new ArrayList();
        StringTokenizer tokenizer = new StringTokenizer(rawMapping,
        VirtualUserTableUtil.getSeparator(rawMapping));

        while (tokenizer.hasMoreTokens()) {
            String raw = tokenizer.nextToken().trim();
            map.add(raw);
        }
        return map;
   }
    

    /**
     * Convert a Collection which holds mappings to a raw mapping String
     * 
     * @param map the Collection
     * @return mapping the mapping String
     */
    protected String CollectionToMapping(Collection map) {
        StringBuffer mapping = new StringBuffer();
    
        Iterator mappings = map.iterator();
    
        while (mappings.hasNext()) {
            mapping.append(mappings.next());
        
            if (mappings.hasNext()) {
                mapping.append(";");
            }
        }  
        return mapping.toString();  
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
}
