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

package org.apache.james.api.imap.message.response.imap4rev1.messagedata;

public interface BodyFieldExtension {
    
    /**
     * The RFC 1864 MD5 value of the body.
     * @return md5 sum, otherwise null
     */
    public CharSequence getMd5();
    
    /**
     * Disposition type part of RFC2060 body_fld_dsp.
     * @return disposition type, otherwise null
     */
    public CharSequence getDispostion();
    
    /**
     * Gets disposition attribute/value pairs.
     * @return disposition parameters, otherwise null
     */
    public BodyFieldParam[] getDispositionParams();
    
    /**
     * RFC 1766 language tags.
     * @return one or more langauge tags, otherwise null
     */
    public CharSequence[] getLanguage();
    
    /**
     * 
     * @return <code>BodyExtension</code>, otherwise null
     */
    public BodyExtension getBodyExtension();
}
