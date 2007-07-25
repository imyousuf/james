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

public interface BodyVisitor {
    
    /**
     * RFC2060 body_type_basic for MIME type <code>AUDIO</code>.
     * @param mediaSubType RFC2045 MIME media sub type
     * @param bodyFields <code>BodyFields</code>, not null
     */
    public void basicMessageAudio(CharSequence mediaSubType, BodyFields bodyFields, BodyFieldExtension extension);
    
    /**
     * RFC2060 body_type_basic for MIME type <code>MESSAGE</code>.
     * @param mediaSubType RFC2045 MIME media sub type, not <code>RFC822<code>
     * @param bodyFields <code>BodyFields</code>, not null
     */
    public void basicMessageBody(CharSequence mediaSubType, BodyFields bodyFields, BodyFieldExtension extension);
    
    /**
     * RFC2060 body_type_basic for MIME type <code>VIDEO</code>.
     * @param mediaSubType RFC2045 MIME media sub type
     * @param bodyFields <code>BodyFields</code>, not null
     */
    public void basicVideoBody(CharSequence mediaSubType, BodyFields bodyFields, BodyFieldExtension extension);
    
    /**
     * RFC2060 body_type_basic for MIME type <code>IMAGE</code>.
     * @param mediaSubType RFC2045 MIME media sub type
     * @param bodyFields <code>BodyFields</code>, not null
     */
    public void basicImageBody(CharSequence mediaSubType, BodyFields bodyFields, BodyFieldExtension extension);
    
    /**
     * RFC2060 body_type_basic for MIME type <code>APPLICATION</code>.
     * @param mediaSubType RFC2045 MIME media sub type
     * @param bodyFields <code>BodyFields</code>, not null
     */
    public void basicApplicationBody(CharSequence mediaSubType, BodyFields bodyFields, BodyFieldExtension extension);
    
    /**
     * RFC2060 body_type_basic for extension MIME media types.
     * @param mediaType extension MIME media type, not <code>APPLICATION</code>,
     * <code>IMAGE</code>, <code>VIDEO</code>, <code>AUDIO</code>, <code>MESSAGE</code>
     * @param mediaSubType RFC2045 MIME media sub type
     * @param bodyFields <code>BodyFields</code>, not null
     */
    public void basicOtherBody(CharSequence mediaType, CharSequence mediaSubType, BodyFields bodyFields, BodyFieldExtension extension);
    
    public void textBody(CharSequence mediaSubType, BodyFields bodyFields, int bodyFieldLines, BodyFieldExtension extension);
    
    public void startMessageBody(BodyFields bodyFields, Envelope envelope);
    public void endMessageBody(int bodyFieldLines, BodyFieldExtension extension);
    
    public void startMultipartBody(); 
    public void endMultipartBody(CharSequence mediaSubType, MultipartBodyFieldExtension extension);
}
