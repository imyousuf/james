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

package org.apache.james.api.imap.message.response;

/**
 * RFC2060 flags for a message. 
 * Defined in 2.3.2.
 */
public interface Flags {

    /**
     * RFC2060 <code>Seen</code> flag.
     * @return true if the <code>Seen</code> flag is set,
     * false otherwise
     */
    public boolean seen();
    
    /**
     * RFC2060 <code>Answered</code> flag.
     * @return true if the <code>Answered</code> flag is set,
     * false otherwise
     */
    public boolean answered();
    
    /**
     * RFC2060 <code>Flagged</code> flag.
     * @return true if the <code>Flagged</code> flag is set,
     * false otherwise
     */
    public boolean flagged();
    
    /**
     * RFC2060 <code>Deleted</code> flag.
     * @return true if the <code>Deleted</code> flag is set,
     * false otherwise
     */
    public boolean deleted();
    
    /**
     * RFC2060 <code>Draft</code> flag.
     * @return true if the <code>Draft</code> flag is set,
     * false otherwise
     */
    public boolean draft();
    
    /**
     * RFC2060 <code>Recent</code> flag.
     * @return true if the <code>Recent</code> flag is set,
     * false otherwise
     */
    public boolean recent();
    
    /**
     * RFC2060 flag_keywords.
     * @return <code>CharSequence</code>, 
     * or null if there are no keywords set
     */
    public CharSequence[] keywords();
}
