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

package org.apache.james.test.functional;

public class CreateScript {

    public static final void main(String[] args) throws Exception {
        ScriptBuilder builder = ScriptBuilder.open("localhost", 143);
        notHeaderFetches(builder);
    }
    
    public static void notHeaderFetches(ScriptBuilder builder) throws Exception {
        builder.login();
        builder.create();
        builder.select();
        builder.append();
        builder.setFile("wild-example.mail");
        builder.append();
        builder.setFile("multipart-alt.mail");
        builder.append();
        builder.setFile("multipart-mixed.mail");
        builder.append();
        builder.setFile("multipart-mixed-complex.mail");
        builder.append();
        builder.setFile("rfc822-hello-world.mail");
        builder.append();
        builder.setFile("rfc822-sender.mail");
        builder.append();
        builder.setFile("rfc822.mail");
        builder.append();
        builder.setFile("rfc822-multiple-addresses.mail");
        builder.append();
        builder.select();
        builder.getFetch().bodyPeekCompleteMessage();
        builder.fetchAllMessages();
        builder.resetFetch();
        builder.getFetch().bodyPeekNotHeaders(ScriptBuilder.Fetch.SELECT_HEADERS);
        builder.fetchAllMessages();
        builder.select();
        builder.quit();
    }
    
    public static void simpleCombinedFetches(ScriptBuilder builder) throws Exception {
        builder.login();
        builder.create();
        builder.select();
        builder.append();
        builder.setFile("wild-example.mail");
        builder.append();
        builder.setFile("multipart-alt.mail");
        builder.append();
        builder.setFile("multipart-mixed.mail");
        builder.append();
        builder.setFile("multipart-mixed-complex.mail");
        builder.append();
        builder.setFile("rfc822-hello-world.mail");
        builder.append();
        builder.setFile("rfc822-sender.mail");
        builder.append();
        builder.setFile("rfc822.mail");
        builder.append();
        builder.setFile("rfc822-multiple-addresses.mail");
        builder.append();
        builder.select();
        builder.getFetch().bodyPeekCompleteMessage();
        builder.fetchAllMessages();
        builder.resetFetch();
        builder.getFetch().bodyPeekHeaders(ScriptBuilder.Fetch.COMPREHENSIVE_HEADERS);
        builder.fetchAllMessages();
        builder.select();
        builder.quit();
    }
    
    public static void recent(ScriptBuilder builder) throws Exception {
        builder.login();
        builder.create();
        builder.select();
        builder.append();
        builder.select();
        builder.fetchFlags();
        builder.fetchSection("");
        builder.fetchFlags();
        builder.quit();
    }

    public static void multipartMixedMessagesPeek(ScriptBuilder builder) throws Exception {
        builder.setPeek(true);
        multipartMixedMessages(builder);
    }
    
    public static void multipartMixedMessages(ScriptBuilder builder) throws Exception {
        builder.login();
        builder.create();
        builder.select();
        builder.setFile("multipart-mixed-complex.mail");
        builder.append();
        builder.select();
        builder.fetchSection("");
        builder.fetchSection("TEXT");
        builder.fetchSection("HEADER");
        builder.fetchSection("1");
        builder.fetchSection("2");
        builder.fetchSection("3");
        builder.fetchSection("3.HEADER");
        builder.fetchSection("3.TEXT");
        builder.fetchSection("3.1");
        builder.fetchSection("3.2");
        builder.fetchSection("4");
        builder.fetchSection("4.1");
        builder.fetchSection("4.1.MIME");
        builder.fetchSection("4.2");
        builder.fetchSection("4.2.HEADER");
        builder.fetchSection("4.2.TEXT");
        builder.fetchSection("4.2.1");
        builder.fetchSection("4.2.2");
        builder.fetchSection("4.2.2.1");
        builder.fetchSection("4.2.2.2");
        builder.select();
        builder.quit();
    }
    
    public static void multipartAlternativePeek(ScriptBuilder builder) throws Exception {
        builder.setPeek(true);
        multipartAlternative(builder);
    }
    
    public static void multipartAlternative(ScriptBuilder builder) throws Exception {
        builder.login();
        builder.create();
        builder.select();
        builder.setFile("multipart-alt.mail");
        builder.append();
        builder.select();
        builder.fetchSection("");
        builder.fetchSection("TEXT");
        builder.fetchSection("HEADER");
        builder.fetchSection("1");
        builder.fetchSection("2");
        builder.fetchSection("3");
        builder.select();
        builder.quit();
    }
    
    public static void multipartMixedPeek(ScriptBuilder builder) throws Exception {
        builder.setPeek(true);
        multipartMixed(builder);
    }
    
    public static void multipartMixed(ScriptBuilder builder) throws Exception {
        builder.login();
        builder.create();
        builder.select();
        builder.setFile("multipart-mixed.mail");
        builder.append();
        builder.select();
        builder.fetchSection("");
        builder.fetchSection("TEXT");
        builder.fetchSection("HEADER");
        builder.fetchSection("1");
        builder.fetchSection("2");
        builder.fetchSection("3");
        builder.fetchSection("4");
        builder.select();
        builder.quit();
    }
    
}
