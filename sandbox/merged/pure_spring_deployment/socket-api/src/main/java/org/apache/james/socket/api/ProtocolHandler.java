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

package org.apache.james.socket.api;

import java.io.IOException;


/**
 * Handles protocol interactions.
 */
public interface ProtocolHandler {

    /**
     * Handle the protocol
     * @param context not null
     * 
     * @throws IOException get thrown if an IO error is detected
     */
    public abstract void handleProtocol(ProtocolContext context) throws IOException;

    /**
     * Resets the handler data to a basic state.
     */
    public abstract void resetHandler();

    /**
     * Called when a fatal failure occurs during processing.
     * Provides a last ditch chance to send a message to the client.
     * @param e exception
     * @param context not null
     */
    public abstract void fatalFailure(RuntimeException e, ProtocolContext context);

}