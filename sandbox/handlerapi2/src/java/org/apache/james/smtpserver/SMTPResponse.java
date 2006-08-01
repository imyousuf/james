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

package org.apache.james.smtpserver;

import java.util.ArrayList;
import java.util.Collection;

/**
 * The SMTPResponse which should be returned to the client socked
 */
public class SMTPResponse
{
    private Collection resp = new ArrayList();

    /**
     * Store the responseString which should be returned to the client socked
     * 
     * @param response The RepsponseString which should be returned to the client socked
     */
    public void store(String response) {
    resp.clear();
    resp.add(response);
    }
    
    public void append(String response) {
    resp.add(response);
    }
    
    /**
     * Get the reponseString which should be returend to the client socked
     * 
     * @return response The responseString which should be returned to the client socked
     */
    public Collection retrieve() {
        return resp;
    }

    /**
     * Reset the SMTPResponse
     *
     */
    public void clear() {
    resp.clear();
    }
}
