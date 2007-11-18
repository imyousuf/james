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
package org.apache.james.imap.message.response.imap4rev1;

import org.apache.james.api.imap.message.response.ImapResponseMessage;

/**
 * @deprecated data should be not be encoded in the processor
 */
public class LegacyFetchResponse implements ImapResponseMessage {

        // TODO: this is not an efficient solution
        // TODO: would be better to lazy load and stream on output
        // TODO: this is just a transitional solution
        private final int number;
        private final String data;
        public LegacyFetchResponse(final int number, final String data) {
            super();
            this.number = number;
            this.data = data;
        }
        
        public final String getData() {
            return data;
        }
        
        public final int getNumber() {
            return number;
        }        
}
