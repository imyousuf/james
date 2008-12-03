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

package org.apache.james.imapserver.sieve;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.jsieve.mailet.ResourceLocator;

/**
 * To maintain backwards compatibility with existing
 * installations, this uses the old file based scheme.
 * TODO: replace with <code>FileSystem</code> based implementation.
 */
public class ResourceLocatorImpl implements ResourceLocator {

    public InputStream get(String uri) throws IOException {
        // This is a toy implementation
        String username = uri.substring(3, uri.indexOf('@'));
        String sieveFileName = "../apps/james/var/sieve/"+username+".sieve";
        return new FileInputStream(sieveFileName);
    }

}
