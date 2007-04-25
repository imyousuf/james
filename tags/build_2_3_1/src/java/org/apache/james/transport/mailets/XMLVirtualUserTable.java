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

package org.apache.james.transport.mailets;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import javax.mail.MessagingException;

import org.apache.mailet.MailAddress;

/**
 * Implements a Virtual User Table to translate virtual users
 * to real users. This implementation has the same functionality
 * as <code>JDBCVirtualUserTable</code>, but is configured in the
 * JAMES configuration and is thus probably most suitable for smaller
 * and less dynamic mapping requirements.
 * 
 * The configuration is specified in the form:
 * 
 * &lt;mailet match="All" class="XMLVirtualUserTable"&gt;
 *   &lt;mapping&gt;virtualuser@xxx=realuser[@yyy][;anotherrealuser[@zzz]]&lt;/mapping&gt;
 *   &lt;mapping&gt;virtualuser2@*=realuser2[@yyy][;anotherrealuser2[@zzz]]&lt;/mapping&gt;
 *   ...
 * &lt;/mailet&gt;
 * 
 * As many &lt;mapping&gt; elements can be added as necessary. As indicated,
 * wildcards are supported, and multiple recipients can be specified with a 
 * semicolon-separated list. The target domain does not need to be specified if 
 * the real user is local to the server.
 * 
 * Matching is done in the following order:
 * 1. user@domain    - explicit mapping for user@domain
 * 2. user@*         - catchall mapping for user anywhere
 * 3. *@domain       - catchall mapping for anyone at domain
 * 4. null           - no valid mapping
 */
public class XMLVirtualUserTable extends AbstractVirtualUserTable
{
  /**
   * Holds the configured mappings
   */
  private Map mappings = new HashMap();

  /**
   * Initialize the mailet
   */
  public void init() throws MessagingException {
      String mapping = getInitParameter("mapping");
      
      if(mapping != null) {
          StringTokenizer tokenizer = new StringTokenizer(mapping, ",");
          while(tokenizer.hasMoreTokens()) {
            String mappingItem = tokenizer.nextToken();
            int index = mappingItem.indexOf('=');
            String virtual = mappingItem.substring(0, index).trim().toLowerCase();
            String real = mappingItem.substring(index + 1).trim().toLowerCase();
            mappings.put(virtual, real);
          }
      }
  }

  /**
   * Map any virtual recipients to real recipients using the configured mapping.
   * 
   * @param recipientsMap the mapping of virtual to real recipients
   */
  protected void mapRecipients(Map recipientsMap) throws MessagingException {
      Collection recipients = recipientsMap.keySet();  
        
      for (Iterator i = recipients.iterator(); i.hasNext(); ) {
          MailAddress source = (MailAddress)i.next();
          String user = source.getUser().toLowerCase();
          String domain = source.getHost().toLowerCase();
    
          String targetString = getTargetString(user, domain);
          
          if (targetString != null) {
              recipientsMap.put(source, targetString);
          }
      }
  }

  /**
   * Returns the real recipient given a virtual username and domain.
   * 
   * @param user the virtual user
   * @param domain the virtual domain
   * @return the real recipient address, or <code>null</code> if no mapping exists
   */
  private String getTargetString(String user, String domain) {
      StringBuffer buf;
      String target;
      
      //Look for exact (user@domain) match
      buf = new StringBuffer().append(user).append("@").append(domain);
      target = (String)mappings.get(buf.toString());
      if (target != null) {
          return target;
      }
      
      //Look for user@* match
      buf = new StringBuffer().append(user).append("@*");
      target = (String)mappings.get(buf.toString());
      if (target != null) {
          return target;
      }
      
      //Look for *@domain match
      buf = new StringBuffer().append("*@").append(domain);
      target = (String)mappings.get(buf.toString());
      if (target != null) {
          return target;
      }
      
      return null;
  }
  
  public String getMailetInfo() {
      return "XML Virtual User Table mailet";
  }
}
