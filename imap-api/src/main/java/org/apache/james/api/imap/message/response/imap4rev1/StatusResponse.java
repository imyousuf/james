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

package org.apache.james.api.imap.message.response.imap4rev1;

import java.util.Collection;
import java.util.Collections;

import javax.mail.Flags;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.display.HumanReadableTextKey;
import org.apache.james.api.imap.message.MessageFlags;
import org.apache.james.api.imap.message.response.ImapResponseMessage;

/**
 * <p>
 * Represents an <code>RFC2060</code> status response.
 * The five specified status server responses (<code>OK<code>.
 * <code>NO</code>, <code>BAD</code>, <code>PREAUTH</code>
 * and <code>BYE</code>) are modeled by this single interface.
 * They are differentiated by {@link #getServerResponseType()}
 * </p>
 */
public interface StatusResponse extends ImapResponseMessage {
    
    /**
     * Gets the server response type of this status message.
     * @return
     */
    public Type getServerResponseType();
    
    /**
     * Gets the tag.
     * @return if tagged response, the tag. Otherwise null.
     */
    public String getTag();
    
    /**
     * Gets the command.
     * @return if tagged response, the command. Otherwise null
     */
    public ImapCommand getCommand();
    
    /**
     * Gets the key to the human readable text to be displayed.
     * Required.
     * @return key for the text message to be displayed, not null
     */
    public HumanReadableTextKey getTextKey();
    
    /**
     * Gets the response code.
     * Optional.
     * @return <code>ResponseCode</code>, 
     * or null if there is no response code
     */
    public ResponseCode getResponseCode();
    
    /**
     * Enumerates types of RC2060 status response 
     */
    public static final class Type {
        /** RFC2060 <code>OK</code> server response */
        public static final Type OK = new Type("OK");
        /** RFC2060 <code>OK</code> server response */
        public static final Type NO = new Type("NO");
        /** RFC2060 <code>BAD</code> server response */
        public static final Type BAD = new Type("BAD")
        /** RFC2060 <code>PREAUTH</code> server response */;
        public static final Type PREAUTH = new Type("PREAUTH");
        /** RFC2060 <code>BYE</code> server response */
        public static final Type BYE = new Type("BYE");
        
        private final String code;

        private Type(final String code) {
            super();
            this.code = code;
        }

        public int hashCode() {
            final int PRIME = 31;
            int result = 1;
            result = PRIME * result + ((code == null) ? 0 : code.hashCode());
            return result;
        }
        
        public final String getCode() {
            return code;
        }

        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final ResponseCode other = (ResponseCode) obj;
            if (code == null) {
                if (other.code != null)
                    return false;
            } else if (!code.equals(other.code))
                return false;
            return true;
        }
        
        public String toString() {
            return code;
        }
    }
    
    /**
     * Enumerates response codes.
     */
    public static final class ResponseCode {
  
        /** RFC2060 <code>ALERT</code> response code */
        private static final ResponseCode ALERT = new ResponseCode("ALERT");
        /** RFC2060 <code>PARSE</code> response code */
        private static final ResponseCode PARSE = new ResponseCode("PARSE");
        /** RFC2060 <code>READ_ONLY</code> response code */
        private static final ResponseCode READ_ONLY = new ResponseCode("READ-ONLY");
        /** RFC2060 <code>READ_WRITE</code> response code */
        private static final ResponseCode READ_WRITE = new ResponseCode("READ-WRITE");
        /** RFC2060 <code>TRYCREATE</code> response code */
        private static final ResponseCode TRYCREATE = new ResponseCode("TRYCREATE");
       
        /**
         * Creates a RFC2060 <code>ALERT</code> response code.
         * @return <code>ResponseCode</code>, not null
         */
        public static final ResponseCode alert() {
            return ALERT;
        }
        
        /**
         * Creates a RFC2060 <code>BADCHARSET</code> response code.
         * @param charsetNames <code>Collection<String></code> containing charset names
         * @return <code>ResponseCode</code>, not null
         */
        public static final ResponseCode badCharset(Collection charsetNames) {
            return new ResponseCode("BADCHARSET", charsetNames);
        }
        
        /**
         * Creates a RFC2060 <code>PARSE</code> response code.
         * @return <code>ResponseCode</code>, not null
         */
        public static final ResponseCode parse() {
            return PARSE;
        }
        
        /**
         * Creates a RFC2060 <code>PERMENANTFLAGS</code> response code.
         * @param flagNames <code>Collection<String></code> containing flag names
         * @return <code>ResponseCode</code>, not null
         */
        public static final ResponseCode permanentFlags(Flags flags) {
            return new ResponseCode("PERMANENTFLAGS", MessageFlags.names(flags));
        }
        
        /**
         * Creates a RFC2060 <code>READ-ONLY</code> response code.
         * @return <code>ResponseCode</code>, not null
         */
        public static final ResponseCode readOnly() {
            return READ_ONLY;
        }
        
        /**
         * Creates a RFC2060 <code>READ-WRITE</code> response code.
         * @return <code>ResponseCode</code>, not null
         */
        public static final ResponseCode readWrite() {
            return READ_WRITE;
        }
        
        /**
         * Creates a RFC2060 <code>TRYCREATE</code> response code.
         * @return <code>ResponseCode</code>, not null
         */
        public static final ResponseCode tryCreate() {
            return TRYCREATE;
        }
        
        /**
         * Creates a RFC2060 <code>UIDVALIDITY</code> response code.
         * @param uid positive non-zero integer
         * @return <code>ResponseCode</code>, not null
         */
        public static final ResponseCode uidValidity(long uid) {
            return new ResponseCode("UIDVALIDITY", uid);
        }
        
        /**
         * Creates a RFC2060 <code>UNSEEN</code> response code.
         * @param numberUnseen positive non-zero integer
         * @return <code>ResponseCode</code>, not null
         */
        public static final ResponseCode unseen(int numberUnseen) {
            return new ResponseCode("UNSEEN", numberUnseen);
        }
        
        /**
         * Creates a RFC2060 <code>UIDNEXT</code> response code.
         * @param uid positive non-zero integer
         * @return <code>ResponseCode</code>, not null
         */
        public static final ResponseCode uidNext(int uid) {
            return new ResponseCode("UIDNEXT", uid);
        }
        
        /**
         * Creates an extension response code.
         * Names that do not begin with 'X' will have 'X' prepended
         * @param name extension code, not null
         * @return <code>ResponseCode</code>, not null
         */
        public static ResponseCode createExtension(String name) {
            StringBuffer buffer = new StringBuffer(name.length() + 2);
            if (!name.startsWith("X")) {
                buffer.append('X');
            }
            buffer.append(name);
            final ResponseCode result = new ResponseCode(buffer.toString());
            return result;
        }
        
        private final String code;
        private final Collection parameters;
        private final long number;

        private ResponseCode(final String code) {
            this(code, Collections.EMPTY_LIST, 0);
        }
        
        private ResponseCode(final String code, final long number) {
            this(code, Collections.EMPTY_LIST, number);
        }
        
        private ResponseCode(final String code, final Collection parameters) {
            this(code, parameters, 0);
        }
        
        private ResponseCode(final String code, final Collection parameters, final long number) {
            super();
            this.code = code;
            this.parameters = parameters;
            this.number = number;
        }

        public final String getCode() {
            return code;
        }
        
        /**
         * Gets number for this response.
         * @return the number, 
         * or zero if no number has been set
         */
        public final long getNumber() {
            return number;
        }

        /**
         * Gets parameters for this code.
         * @return the parameters <code>Collection</code>
         * of <code>String</code> parameters, not null
         */
        public final Collection getParameters() {
            return parameters;
        }        
        
        public int hashCode() {
            final int PRIME = 31;
            int result = 1;
            result = PRIME * result + ((code == null) ? 0 : code.hashCode());
            result = PRIME * result + (int) (number ^ (number >>> 32));
            result = PRIME * result + ((parameters == null) ? 0 : parameters.hashCode());
            return result;
        }

        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final ResponseCode other = (ResponseCode) obj;
            if (code == null) {
                if (other.code != null)
                    return false;
            } else if (!code.equals(other.code))
                return false;
            if (number != other.number)
                return false;
            if (parameters == null) {
                if (other.parameters != null)
                    return false;
            } else if (!parameters.equals(other.parameters))
                return false;
            return true;
        }

        public String toString() {
            return code;
        }
    }
}
