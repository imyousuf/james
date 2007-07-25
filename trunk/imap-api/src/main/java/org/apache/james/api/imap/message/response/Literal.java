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

import java.io.IOException;

/**
 * <p>
 * <strong>Note</strong> that implementations
 * may not support multiple calls to {@link #writeAll(LiteralSink)}. 
 * See {@link #isConsumed()}.
 * </p>
 */
public interface Literal {
    
    /**
     * Is this literal consumed?
     * 
     * @return true if this literal can be written out again,
     * false otherwise
     */
    public boolean isConsumed();
    
    /**
     * Gets the total number of bytes contained in this literal.
     * @return byte count
     */
    public int getByteCount() throws IOException;
    
    /**
     * Writes all the contents of this literal into the given sink.
     * 
     * @param <code>LiteralSink</code>, not null
     */
    public void writeAll(LiteralSink sink) throws IOException;
}
