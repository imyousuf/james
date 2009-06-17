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


package org.apache.james.test.mock.avalon;

import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.service.ServiceException;

import java.util.HashMap;
import java.util.Map;

public class MockStore implements Store {

    Map m_storedObjectMap = new HashMap();

    public void add(Object key, Object obj) {
        m_storedObjectMap.put(key, obj);
    }
    
    public Object select(Object object) throws ServiceException {
        Object result = get(object);
        return result;
    }

    private Object get(Object object) {
        return m_storedObjectMap.get(extractKeyObject(object));
    }

    private Object extractKeyObject(Object object) {
        if (object instanceof Configuration) {
            Configuration repConf = (Configuration) object;
            try {
                String attribute = repConf.getAttribute("destinationURL");
                String[] strings = attribute.split("/");
                if (strings.length > 0) {
                    object = strings[strings.length-1];
                }
            } catch (ConfigurationException e) {
                throw new RuntimeException("test configuration setup failed");
            }
            
        }
        return object;
    }

    public boolean isSelectable(Object object) {
        return get(object) != null;
    }

    public void release(Object object) {
        //trivial implementation
    }
}
