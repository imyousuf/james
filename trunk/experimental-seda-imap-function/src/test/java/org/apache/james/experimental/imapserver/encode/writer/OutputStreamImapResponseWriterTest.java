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

package org.apache.james.experimental.imapserver.encode.writer;


public class OutputStreamImapResponseWriterTest extends AbstractTestOutputStreamImapResponseWriter {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCloseParen() throws Exception {
        writer.closeParen();
        checkExpected(")");
    }

    public void testOpenParen() throws Exception {
        writer.openParen();
        checkExpected(" (");
    }

    public void testOpenParenMessageCloseParen() throws Exception {
        writer.openParen();
        writer.message("Hello");
        writer.closeParen();
        checkExpected(" (Hello)");
    }
    

    public void testOpenParenQuoteCloseParen() throws Exception {
        writer.openParen();
        writer.quote("Hello");
        writer.closeParen();
        checkExpected(" (\"Hello\")");
    }
    
    public void testOpenParenOpenParenMessageCloseParenCloseParen() throws Exception {
        writer.openParen();
        writer.openParen();
        writer.quote("Hello");
        writer.closeParen();
        writer.closeParen();
        checkExpected(" ((\"Hello\"))");
    }
    
    public void testOpenParenCloseParenMessage() throws Exception {
        writer.openParen();
        writer.closeParen();
        writer.message("Hello");
        checkExpected(" () Hello");
    }
    
}
