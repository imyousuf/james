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



package org.apache.james.smtpserver.core.filter.fastfail;

import java.util.HashMap;
import java.util.Map;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.james.util.VirtualUserTableUtil;
import org.apache.mailet.MailAddress;

/**
 * This handler should be called before ValidRcptHandler if the XMLVirtualUserTable is used.
 * It only set some state. It not replace the recipient.
 */
public class XMLVirtualUserTableHandler extends AbstractVirtualUserTableHandler implements Configurable{

    Map mappings = new HashMap();
    
    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration arg0) throws ConfigurationException {
        Configuration[] mapping = arg0.getChildren("mapping");
    
        if (mapping != null) {
            StringBuffer map = new StringBuffer();
            
            for (int i = 0; i < mapping.length; i++) {
                if (i != 0)  {
                    map.append(", ");
                }
                map.append(mapping[i]);
            }
            mappings = VirtualUserTableUtil.getXMLMappings(map.toString());
        }       
    }
    
    /**
     * @see org.apache.james.smtpserver.core.filter.fastfail.AbstractVirtualUserTableHandler#mapRecipients(MailAddress)
     */
    protected String mapRecipients(MailAddress recipient) {
        String user = recipient.getUser().toLowerCase();
        String domain = recipient.getHost().toLowerCase();
  
        return VirtualUserTableUtil.getTargetString(user, domain, mappings);
    }

}
