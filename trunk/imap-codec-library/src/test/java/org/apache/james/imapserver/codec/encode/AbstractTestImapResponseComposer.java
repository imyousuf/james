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

package org.apache.james.imapserver.codec.encode;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public abstract class AbstractTestImapResponseComposer extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testQuotedDelimiter() throws Exception {
        checkListResponseEncode("* LSUB () \"\\\"\" \"#news\"\r\n", "LSUB", null, "\"", "#news");
        checkListResponseEncode("* LIST () \"\\\"\" \"#INBOX\"\r\n", "LIST", null, "\"", "#INBOX");
        checkListResponseEncode("* LSUB () \"\\\\\" \"#news\"\r\n", "LSUB", null, "\\", "#news");
        checkListResponseEncode("* LIST () \"\\\\\" \"#INBOX\"\r\n", "LIST", null, "\\", "#INBOX");
    }
    
    public void testNilDelimiter() throws Exception {
        checkListResponseEncode("* LSUB () NIL \"#news\"\r\n", "LSUB", null, null, "#news");
        checkListResponseEncode("* LIST () NIL \"#INBOX\"\r\n", "LIST", null, null, "#INBOX");
    }
    
    public void testSimple() throws Exception {
        checkListResponseEncode("* LSUB () \".\" \"#news\"\r\n", "LSUB", null, ".", "#news");
        checkListResponseEncode("* LIST () \".\" \"#INBOX\"\r\n", "LIST", null, ".", "#INBOX");
        checkListResponseEncode("* LSUB () \".\" \"#news.sub\"\r\n", "LSUB", null, ".", "#news.sub");
        checkListResponseEncode("* LIST () \".\" \"#INBOX.sub\"\r\n", "LIST", null, ".", "#INBOX.sub");
    }

    public void testSpecialNames() throws Exception {
        checkListResponseEncode("* LSUB () \"\\\\\" \"#news\\\\sub\\\\directory\"\r\n", "LSUB", null, "\\", "#news\\sub\\directory");
        checkListResponseEncode("* LIST () \"\\\\\" \"#INBOX\\\\sub\\\\directory\"\r\n", "LIST", null, "\\", "#INBOX\\sub\\directory");
        checkListResponseEncode("* LSUB () \".\" \"#news.sub directory.what\"\r\n", "LSUB", null, ".", "#news.sub directory.what");
        checkListResponseEncode("* LIST () \".\" \"#INBOX.sub directory.what\"\r\n", "LIST", null, ".", "#INBOX.sub directory.what");
        checkListResponseEncode("* LSUB () \".\" \"#news.\\\"sub directory\\\".what\"\r\n", "LSUB", null, ".", "#news.\"sub directory\".what");
        checkListResponseEncode("* LIST () \".\" \"#INBOX.\\\"sub directory\\\".what\"\r\n", "LIST", null, ".", "#INBOX.\"sub directory\".what");
    }
    
    public void testAttributes() throws Exception {
        List attributes = new ArrayList();
        attributes.add("\\one");
        attributes.add("\\two");
        attributes.add("\\three");
        attributes.add("\\four");
        checkListResponseEncode("* LSUB (\\one \\two \\three \\four) \".\" \"#news\"\r\n", "LSUB", attributes, ".", "#news");
        checkListResponseEncode("* LIST (\\one \\two \\three \\four) \".\" \"#INBOX\"\r\n", "LIST", attributes, ".", "#INBOX");
    }
    
    private void checkListResponseEncode(String expected, String typeName, List attributes, 
            String hierarchyDelimiter, String name) throws Exception {
        StringBuffer buffer = new StringBuffer();
        byte[] output = encodeListResponse(typeName, attributes, hierarchyDelimiter, name);
        for (int i=0;i<output.length;i++) {
            buffer.append((char) output[i]);
        }
        assertEquals(expected, buffer.toString());
        clear();
    }
    
    protected abstract byte[] encodeListResponse(String typeName, List attributes, 
            String hierarchyDelimiter, String name) throws Exception;
    
    protected abstract void clear() throws Exception;
}
