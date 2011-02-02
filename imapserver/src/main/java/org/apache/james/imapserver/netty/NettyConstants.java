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
package org.apache.james.imapserver.netty;

/**
 * Just some constants which are used with the Netty implementation
 * 
 *
 */
public interface NettyConstants {
    public final static String ZLIB_DECODER = "zlibDecoder";
    public final static String ZLIB_ENCODER = "zlibEncoder";
    public final static String SSL_HANDLER = "sslHandler";
    public final static String REQUEST_DECODER = "requestDecoder";
    public final static String FRAMER = "framer";
    public final static String TIMEOUT_HANDLER = "timeoutHandler";
    public final static String CORE_HANDLER = "coreHandler";
    public final static String GROUP_HANDLER = "groupHandler";
    public final static String CONNECTION_LIMIT_HANDLER = "connectionLimitHandler";
    public final static String CONNECTION_LIMIT_PER_IP_HANDLER = "connectionPerIpLimitHandler";
    public final static String CONNECTION_COUNT_HANDLER= "connectionCountHandler";
}
