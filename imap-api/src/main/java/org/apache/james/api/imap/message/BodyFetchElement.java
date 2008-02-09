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

import org.apache.james.api.imap.ImapConstants;

public class BodyFetchElement
{

    public static final int TEXT = 0;
    public static final int MIME = 1;
    public static final int HEADER = 2;
    public static final int HEADER_FIELDS = 3;
    public static final int HEADER_NOT_FIELDS = 4;
    public static final int CONTENT = 5;
    
    private static final BodyFetchElement rfc822 = new BodyFetchElement(ImapConstants.FETCH_RFC822, CONTENT, null, null);
    private static final BodyFetchElement rfc822Header = new BodyFetchElement(ImapConstants.FETCH_RFC822_HEADER, HEADER, null, null);
    private static final BodyFetchElement rfc822Text = new BodyFetchElement(ImapConstants.FETCH_RFC822_TEXT, TEXT, null, null);

    public static final BodyFetchElement createRFC822() {
        return rfc822;
    }
     
    public static final BodyFetchElement createRFC822Header() {
        return rfc822Header;
    }
    
    public static final BodyFetchElement createRFC822Text() {
        return rfc822Text;
    }
    
    
    private final String name;
    private final int sectionType;
    private final int[] path; 
    private final Collection fieldNames;

    public BodyFetchElement( final String name, final int sectionType, 
            final int[] path, final Collection fieldNames)
    {
        this.name = name;
        this.sectionType = sectionType;
        this.fieldNames = fieldNames;
        this.path = path;
    }
    
    public String getResponseName() {
        return this.name;
    }

    /**
     * Gets field names.
     * @return <code>String</code> collection, when {@link #HEADER_FIELDS} 
     * or {@link #HEADER_NOT_FIELDS}
     * or null otherwise
     */
    public final Collection getFieldNames() {
        return fieldNames;
    }

    /**
     * Gets the MIME path.
     * @return the path, 
     * or null if the section is the base message
     */
    public final int[] getPath() {
        return path;
    }

    /**
     * Gets the type of section.
     * @return {@link #HEADER_FIELDS}, {@link #TEXT}, {@link #CONTENT}, {@link #HEADER},
     * {@link #MIME} or {@link #HEADER_NOT_FIELDS}
     */
    public final int getSectionType() {
        return sectionType;
    }
}
