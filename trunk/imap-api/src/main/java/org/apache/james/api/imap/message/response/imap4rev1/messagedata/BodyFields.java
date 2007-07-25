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

public interface BodyFields {
    
    public static final class Encoding {
        public static final Encoding ENC_7BIT = new Encoding("7BIT");
        public static final Encoding ENC_8BIT = new Encoding("8BIT");
        public static final Encoding ENC_BINARY = new Encoding("BINARY");
        public static final Encoding ENC_BASE64 = new Encoding("BASE64");
        public static final Encoding ENC_QUOTED_PRINTABLE = new Encoding("QUOTED_PRINTABLE");
        
        private final CharSequence encoding;
        public Encoding(final CharSequence encoding) {
            this.encoding = encoding;
        }
        public final CharSequence getEncoding() {
            return encoding;
        }

        public int hashCode() {
            final int PRIME = 31;
            int result = 1;
            result = PRIME * result + ((encoding == null) ? 0 : encoding.hashCode());
            return result;
        }

        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final Encoding other = (Encoding) obj;
            if (encoding == null) {
                if (other.encoding != null)
                    return false;
            } else if (!encoding.equals(other.encoding))
                return false;
            return true;
        }
    }
    
    public BodyFieldParam[] getContentTypeParameters();
    public CharSequence getContentId();
    public CharSequence getContentDescription();
    public Encoding getContentEncoding();
    public long getBodySize();
}
