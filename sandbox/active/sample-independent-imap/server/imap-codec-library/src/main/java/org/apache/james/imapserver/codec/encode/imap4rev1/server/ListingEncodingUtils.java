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

package org.apache.james.imapserver.codec.encode.imap4rev1.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.james.api.imap.ImapConstants;
import org.apache.james.imap.message.response.imap4rev1.server.AbstractListingResponse;
import org.apache.james.imapserver.codec.encode.ImapResponseComposer;

public class ListingEncodingUtils {
    
    public static void encodeListingResponse(final String responseTypeName, final ImapResponseComposer composer, final AbstractListingResponse response) throws IOException {
        final List attributes = getNameAttributes(response);
        
        final String name = response.getName();
        final String hierarchyDelimiter = response.getHierarchyDelimiter();
        
        composer.listResponse(responseTypeName, attributes, hierarchyDelimiter, name);
    }
    
    private static List getNameAttributes(final AbstractListingResponse response) {
        final List attributes;
        if (response.isNameAttributed()) {
            attributes = new ArrayList();
            if (response.isNoInferiors()) {
                attributes.add(ImapConstants.NAME_ATTRIBUTE_NOINFERIORS);
            }
            if (response.isNoSelect()) {
                attributes.add(ImapConstants.NAME_ATTRIBUTE_NOSELECT);
            }
            if (response.isMarked()) {
                attributes.add(ImapConstants.NAME_ATTRIBUTE_MARKED);
            }
            if (response.isUnmarked()) {
                attributes.add(ImapConstants.NAME_ATTRIBUTE_UNMARKED);
            }
        } else {
            attributes = null;
        }
        return attributes;
    }

}
