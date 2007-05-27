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
package org.apache.james.api.imap.message;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


public class FetchData
{
    private boolean flags;
    private boolean uid;
    private boolean internalDate;
    private boolean size;
    private boolean envelope;
    private boolean body;
    private boolean bodyStructure;
    
    private boolean setSeen = false;
    
    private Set bodyElements = new HashSet();
    
    public Collection getBodyElements() {
        return bodyElements;
    }

    public boolean isBody() {
        return body;
    }

    public void setBody(boolean body) {
        this.body = body;
    }

    public boolean isBodyStructure() {
        return bodyStructure;
    }

    public void setBodyStructure(boolean bodyStructure) {
        this.bodyStructure = bodyStructure;
    }

    public boolean isEnvelope() {
        return envelope;
    }

    public void setEnvelope(boolean envelope) {
        this.envelope = envelope;
    }

    public boolean isFlags() {
        return flags;
    }

    public void setFlags(boolean flags) {
        this.flags = flags;
    }

    public boolean isInternalDate() {
        return internalDate;
    }

    public void setInternalDate(boolean internalDate) {
        this.internalDate = internalDate;
    }

    public boolean isSize() {
        return size;
    }

    public void setSize(boolean size) {
        this.size = size;
    }

    public boolean isUid() {
        return uid;
    }

    public void setUid(boolean uid) {
        this.uid = uid;
    }

    public boolean isSetSeen() {
        return setSeen;
    }

    public void add( BodyFetchElement element, boolean peek )
    {
        if (!peek) {
            setSeen = true;
        }
        bodyElements.add(element);
    }
}
