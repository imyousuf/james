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

package org.apache.james.pop3server;

import org.apache.james.protocols.api.StartTlsResponse;

/**
 * Special sub-type of {@link POP3Response} which will trigger the start of TLS after the response was written to the client
 * 
 *
 */
public class StartTlsPop3Response extends POP3Response implements StartTlsResponse{

    public StartTlsPop3Response(String code, CharSequence description) {
        super(code, description);
    }

    public StartTlsPop3Response(String code) {
        super(code);
    }

}
