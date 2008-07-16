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

package org.apache.james.imapserver.codec.encode.imap4rev1;

import javax.mail.Flags;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.imap.message.response.imap4rev1.FetchResponse;
import org.apache.james.imap.message.response.imap4rev1.FetchResponse.Envelope.Address;
import org.apache.james.imapserver.codec.encode.ImapEncoder;
import org.apache.james.imapserver.codec.encode.ImapResponseComposer;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

public class FetchResponseEncoderEnvelopeTest extends MockObjectTestCase {

    private static final String ADDRESS_ONE_HOST = "HOST";
    private static final String ADDRESS_ONE_MAILBOX = "MAILBOX";
    private static final String ADDRESS_ONE_DOMAIN_LIST = "DOMAIN LIST";
    private static final String ADDRESS_ONE_NAME = "NAME";
    private static final String ADDRESS_TWO_HOST = "2HOST";
    private static final String ADDRESS_TWO_MAILBOX = "2MAILBOX";
    private static final String ADDRESS_TWO_DOMAIN_LIST = "2DOMAIN LIST";
    private static final String ADDRESS_TWO_NAME = "2NAME";
    
    private static final int MSN = 100;
    Flags flags;
    ImapResponseComposer composer;
    Mock mockComposer;
    Mock mockNextEncoder;
    FetchResponseEncoder encoder;
    Mock mockCommand;
    FetchResponse message;
    Mock envelope;
    Address[] bcc;
    Address[] cc;
    String date;
    Address[] from;
    String inReplyTo;
    String messageId;
    Address[] replyTo;
    Address[] sender;
    String subject;                  
    Address[] to;
    
    protected void setUp() throws Exception {
        super.setUp();
        envelope = mock(FetchResponse.Envelope.class);
        
        bcc = null;
        cc = null;
        date = null;
        from = null;
        inReplyTo = null;
        messageId = null;
        replyTo = null;
        sender = null;
        subject = null;                  
        to = null;
    
        
        message = new FetchResponse(MSN, null, null, null, null, 
                (FetchResponse.Envelope) envelope.proxy(), null, null, null);
        mockComposer = mock(ImapResponseComposer.class);
        composer = (ImapResponseComposer) mockComposer.proxy();
        mockNextEncoder = mock(ImapEncoder.class);
        encoder = new FetchResponseEncoder((ImapEncoder) mockNextEncoder.proxy());
        mockCommand = mock(ImapCommand.class);
        flags = new Flags(Flags.Flag.DELETED);
    }
    
    private Address[] mockOneAddress()
    {
        Address[] one = {mockAddress(ADDRESS_ONE_NAME, ADDRESS_ONE_DOMAIN_LIST, ADDRESS_ONE_MAILBOX, ADDRESS_ONE_HOST)};
        return one;
    }
    
    private Address[] mockManyAddresses()
    {
        Address[] many = {mockAddress(ADDRESS_ONE_NAME, ADDRESS_ONE_DOMAIN_LIST, ADDRESS_ONE_MAILBOX, ADDRESS_ONE_HOST),
                mockAddress(ADDRESS_TWO_NAME, ADDRESS_TWO_DOMAIN_LIST, ADDRESS_TWO_MAILBOX, ADDRESS_TWO_HOST)};
        return many;
    }
    
    private Address mockAddress(final String name, final String domainList, final String mailbox, final String host)
    {
        Mock address = mock(Address.class);
        address.expects(once()).method("getPersonalName").will(returnValue(name));
        address.expects(once()).method("getAtDomainList").will(returnValue(domainList));
        address.expects(once()).method("getMailboxName").will(returnValue(mailbox));
        address.expects(once()).method("getHostName").will(returnValue(host));
        return (Address) address.proxy();
    }

    private void envelopExpects() {
        envelope.expects(once()).method("getBcc").will(returnValue(bcc)).id("bcc");
        envelope.expects(once()).method("getCc").will(returnValue(cc)).id("cc");
        envelope.expects(once()).method("getDate").will(returnValue(date)).id("date");
        envelope.expects(once()).method("getFrom").will(returnValue(from)).id("from");
        envelope.expects(once()).method("getInReplyTo").will(returnValue(inReplyTo)).id("inreplyto");
        envelope.expects(once()).method("getMessageId").will(returnValue(messageId)).id("messageid");
        envelope.expects(once()).method("getReplyTo").will(returnValue(replyTo)).id("replyto");
        envelope.expects(once()).method("getSender").will(returnValue(sender)).id("sender");
        envelope.expects(once()).method("getSubject").will(returnValue(subject)).id("subject");
        envelope.expects(once()).method("getTo").will(returnValue(to)).id("to");
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testShouldNilAllNullProperties() throws Exception {
        envelopExpects();
        mockComposer.expects(once()).method("openFetchResponse").with(eq((long)MSN)).id("open");
        mockComposer.expects(once()).method("startEnvelope").with(NULL, NULL).after("open").id("start");
        mockComposer.expects(exactly(6)).method("nil").after("start").id("last");
        mockComposer.expects(once()).method("endEnvelope").with(NULL, NULL).after("last").id("end");
        mockComposer.expects(once()).method("closeFetchResponse").after("end");
        
        encoder.doEncode(message, composer);
    }
    
    public void testShouldComposeDate() throws Exception {
        date = "a date";
        envelopExpects();
        mockComposer.expects(once()).method("openFetchResponse").with(eq((long)MSN)).id("open");
        mockComposer.expects(once()).method("startEnvelope").with(eq(date), NULL).after("open").id("start");
        mockComposer.expects(exactly(6)).method("nil").after("start").id("last");
        mockComposer.expects(once()).method("endEnvelope").with(NULL, NULL).after("last").id("end");
        mockComposer.expects(once()).method("closeFetchResponse").after("end");
        
        encoder.doEncode(message, composer);
    }
    
    public void testShouldComposeSubject() throws Exception {
        subject = "some subject";
        envelopExpects();
        mockComposer.expects(once()).method("openFetchResponse").with(eq((long)MSN)).id("open");
        mockComposer.expects(once()).method("startEnvelope").with(NULL, eq(subject)).after("open").id("start");
        mockComposer.expects(exactly(6)).method("nil").after("start").id("last");
        mockComposer.expects(once()).method("endEnvelope").with(NULL, NULL).after("last").id("end");
        mockComposer.expects(once()).method("closeFetchResponse").after("end");
        
        encoder.doEncode(message, composer);
    }
    
    public void testShouldComposeInReplyTo() throws Exception {
        inReplyTo = "some reply to";
        envelopExpects();
        mockComposer.expects(once()).method("openFetchResponse").with(eq((long)MSN)).id("open");
        mockComposer.expects(once()).method("startEnvelope").with(NULL, NULL).after("open").id("start");
        mockComposer.expects(exactly(6)).method("nil").after("start").id("last");
        mockComposer.expects(once()).method("endEnvelope").with(eq(inReplyTo), NULL).after("last").id("end");
        mockComposer.expects(once()).method("closeFetchResponse").after("end");
        
        encoder.doEncode(message, composer);
    }
    
    public void testShouldComposeMessageId() throws Exception {
        messageId = "some message id";
        envelopExpects();
        mockComposer.expects(once()).method("openFetchResponse").with(eq((long)MSN)).id("open");
        mockComposer.expects(once()).method("startEnvelope").with(NULL, NULL).after("open").id("start");
        mockComposer.expects(exactly(6)).method("nil").after("start").id("last");
        mockComposer.expects(once()).method("endEnvelope").with(NULL, eq(messageId)).after("last").id("end");
        mockComposer.expects(once()).method("closeFetchResponse").after("end");
        
        encoder.doEncode(message, composer);
    }
    
    public void testShouldComposeOneFromAddress() throws Exception {
        from = mockOneAddress();
        envelopExpects();
        mockComposer.expects(once()).method("openFetchResponse").with(eq((long)MSN)).id("open");
        mockComposer.expects(once()).method("startEnvelope").with(NULL, NULL).after("open").id("start");
        mockComposer.expects(once()).method("startAddresses").after("start").id("start-from");
        mockComposer.expects(once()).method("address").with(eq(ADDRESS_ONE_NAME), eq(ADDRESS_ONE_DOMAIN_LIST), eq(ADDRESS_ONE_MAILBOX), eq(ADDRESS_ONE_HOST)).after("start-from").id("address-one");
        mockComposer.expects(once()).method("endAddresses").after("address-one").id("from");
        mockComposer.expects(exactly(5)).method("nil").after("from").id("last");
        mockComposer.expects(once()).method("endEnvelope").with(NULL, eq(messageId)).after("last").id("end");
        mockComposer.expects(once()).method("closeFetchResponse").after("end");
        
        encoder.doEncode(message, composer);
    }
    
    public void testShouldComposeManyFromAddress() throws Exception {
        from = mockManyAddresses();
        envelopExpects();
        mockComposer.expects(once()).method("openFetchResponse").with(eq((long)MSN)).id("open");
        mockComposer.expects(once()).method("startEnvelope").with(NULL, NULL).after("open").id("start");
        mockComposer.expects(once()).method("startAddresses").after("start").id("start-from");
        mockComposer.expects(once()).method("address").with(eq(ADDRESS_ONE_NAME), eq(ADDRESS_ONE_DOMAIN_LIST), eq(ADDRESS_ONE_MAILBOX), eq(ADDRESS_ONE_HOST)).after("start-from").id("address-one");
        mockComposer.expects(once()).method("address").with(eq(ADDRESS_TWO_NAME), eq(ADDRESS_TWO_DOMAIN_LIST), eq(ADDRESS_TWO_MAILBOX), eq(ADDRESS_TWO_HOST)).after("address-one").id("address-two");
        mockComposer.expects(once()).method("endAddresses").after("address-two").id("from");
        mockComposer.expects(exactly(5)).method("nil").after("from").id("last");
        mockComposer.expects(once()).method("endEnvelope").with(NULL, eq(messageId)).after("last").id("end");
        mockComposer.expects(once()).method("closeFetchResponse").after("end");
        
        encoder.doEncode(message, composer);
    }

    public void testShouldComposeOneSenderAddress() throws Exception {
        sender = mockOneAddress();
        envelopExpects();
        mockComposer.expects(once()).method("openFetchResponse").with(eq((long)MSN)).id("open");
        mockComposer.expects(once()).method("startEnvelope").with(NULL, NULL).after("open").id("start");
        mockComposer.expects(exactly(1)).method("nil").after("start").id("before");
        mockComposer.expects(once()).method("startAddresses").after("before").id("start-addresses");
        mockComposer.expects(once()).method("address").with(eq(ADDRESS_ONE_NAME), eq(ADDRESS_ONE_DOMAIN_LIST), eq(ADDRESS_ONE_MAILBOX), eq(ADDRESS_ONE_HOST)).after("start-addresses").id("address-one");
        mockComposer.expects(once()).method("endAddresses").after("address-one").id("end-addresses");
        mockComposer.expects(exactly(4)).method("nil").after("end-addresses").id("last");
        mockComposer.expects(once()).method("endEnvelope").with(NULL, eq(messageId)).after("last").id("end");
        mockComposer.expects(once()).method("closeFetchResponse").after("end");
        
        encoder.doEncode(message, composer);
    }
    
    public void testShouldComposeManySenderAddress() throws Exception {
        sender = mockManyAddresses();
        envelopExpects();
        mockComposer.expects(once()).method("openFetchResponse").with(eq((long)MSN)).id("open");
        mockComposer.expects(once()).method("startEnvelope").with(NULL, NULL).after("open").id("start");
        mockComposer.expects(exactly(1)).method("nil").after("start").id("before");
        mockComposer.expects(once()).method("startAddresses").after("before").id("start-addresses");
        mockComposer.expects(once()).method("address").with(eq(ADDRESS_ONE_NAME), eq(ADDRESS_ONE_DOMAIN_LIST), eq(ADDRESS_ONE_MAILBOX), eq(ADDRESS_ONE_HOST)).after("start-addresses").id("address-one");
        mockComposer.expects(once()).method("address").with(eq(ADDRESS_TWO_NAME), eq(ADDRESS_TWO_DOMAIN_LIST), eq(ADDRESS_TWO_MAILBOX), eq(ADDRESS_TWO_HOST)).after("address-one").id("address-two");
        mockComposer.expects(once()).method("endAddresses").after("address-two").id("end-addresses");
        mockComposer.expects(exactly(4)).method("nil").after("end-addresses").id("last");
        mockComposer.expects(once()).method("endEnvelope").with(NULL, eq(messageId)).after("last").id("end");
        mockComposer.expects(once()).method("closeFetchResponse").after("end");
        
        encoder.doEncode(message, composer);
    }
    

    public void testShouldComposeOneReplyToAddress() throws Exception {
        replyTo = mockOneAddress();
        envelopExpects();
        mockComposer.expects(once()).method("openFetchResponse").with(eq((long)MSN)).id("open");
        mockComposer.expects(once()).method("startEnvelope").with(NULL, NULL).after("open").id("start");
        mockComposer.expects(exactly(2)).method("nil").after("start").id("before");
        mockComposer.expects(once()).method("startAddresses").after("before").id("start-addresses");
        mockComposer.expects(once()).method("address").with(eq(ADDRESS_ONE_NAME), eq(ADDRESS_ONE_DOMAIN_LIST), eq(ADDRESS_ONE_MAILBOX), eq(ADDRESS_ONE_HOST)).after("start-addresses").id("address-one");
        mockComposer.expects(once()).method("endAddresses").after("address-one").id("end-addresses");
        mockComposer.expects(exactly(3)).method("nil").after("end-addresses").id("last");
        mockComposer.expects(once()).method("endEnvelope").with(NULL, eq(messageId)).after("last").id("end");
        mockComposer.expects(once()).method("closeFetchResponse").after("end");
        
        encoder.doEncode(message, composer);
    }
    
    public void testShouldComposeManyReplyToAddress() throws Exception {
        replyTo = mockManyAddresses();
        envelopExpects();
        mockComposer.expects(once()).method("openFetchResponse").with(eq((long)MSN)).id("open");
        mockComposer.expects(once()).method("startEnvelope").with(NULL, NULL).after("open").id("start");
        mockComposer.expects(exactly(2)).method("nil").after("start").id("before");
        mockComposer.expects(once()).method("startAddresses").after("before").id("start-addresses");
        mockComposer.expects(once()).method("address").with(eq(ADDRESS_ONE_NAME), eq(ADDRESS_ONE_DOMAIN_LIST), eq(ADDRESS_ONE_MAILBOX), eq(ADDRESS_ONE_HOST)).after("start-addresses").id("address-one");
        mockComposer.expects(once()).method("address").with(eq(ADDRESS_TWO_NAME), eq(ADDRESS_TWO_DOMAIN_LIST), eq(ADDRESS_TWO_MAILBOX), eq(ADDRESS_TWO_HOST)).after("address-one").id("address-two");
        mockComposer.expects(once()).method("endAddresses").after("address-two").id("end-addresses");
        mockComposer.expects(exactly(3)).method("nil").after("end-addresses").id("last");
        mockComposer.expects(once()).method("endEnvelope").with(NULL, eq(messageId)).after("last").id("end");
        mockComposer.expects(once()).method("closeFetchResponse").after("end");
        
        encoder.doEncode(message, composer);
    }

    public void testShouldComposeOneToAddress() throws Exception {
        to = mockOneAddress();
        envelopExpects();
        mockComposer.expects(once()).method("openFetchResponse").with(eq((long)MSN)).id("open");
        mockComposer.expects(once()).method("startEnvelope").with(NULL, NULL).after("open").id("start");
        mockComposer.expects(exactly(3)).method("nil").after("start").id("before");
        mockComposer.expects(once()).method("startAddresses").after("before").id("start-addresses");
        mockComposer.expects(once()).method("address").with(eq(ADDRESS_ONE_NAME), eq(ADDRESS_ONE_DOMAIN_LIST), eq(ADDRESS_ONE_MAILBOX), eq(ADDRESS_ONE_HOST)).after("start-addresses").id("address-one");
        mockComposer.expects(once()).method("endAddresses").after("address-one").id("end-addresses");
        mockComposer.expects(exactly(2)).method("nil").after("end-addresses").id("last");
        mockComposer.expects(once()).method("endEnvelope").with(NULL, eq(messageId)).after("last").id("end");
        mockComposer.expects(once()).method("closeFetchResponse").after("end");
        
        encoder.doEncode(message, composer);
    }
    
    public void testShouldComposeManyToAddress() throws Exception {
        to = mockManyAddresses();
        envelopExpects();
        mockComposer.expects(once()).method("openFetchResponse").with(eq((long)MSN)).id("open");
        mockComposer.expects(once()).method("startEnvelope").with(NULL, NULL).after("open").id("start");
        mockComposer.expects(exactly(3)).method("nil").after("start").id("before");
        mockComposer.expects(once()).method("startAddresses").after("before").id("start-addresses");
        mockComposer.expects(once()).method("address").with(eq(ADDRESS_ONE_NAME), eq(ADDRESS_ONE_DOMAIN_LIST), eq(ADDRESS_ONE_MAILBOX), eq(ADDRESS_ONE_HOST)).after("start-addresses").id("address-one");
        mockComposer.expects(once()).method("address").with(eq(ADDRESS_TWO_NAME), eq(ADDRESS_TWO_DOMAIN_LIST), eq(ADDRESS_TWO_MAILBOX), eq(ADDRESS_TWO_HOST)).after("address-one").id("address-two");
        mockComposer.expects(once()).method("endAddresses").after("address-two").id("end-addresses");
        mockComposer.expects(exactly(2)).method("nil").after("end-addresses").id("last");
        mockComposer.expects(once()).method("endEnvelope").with(NULL, eq(messageId)).after("last").id("end");
        mockComposer.expects(once()).method("closeFetchResponse").after("end");
        
        encoder.doEncode(message, composer);
    }


    public void testShouldComposeOneCcAddress() throws Exception {
        cc = mockOneAddress();
        envelopExpects();
        mockComposer.expects(once()).method("openFetchResponse").with(eq((long)MSN)).id("open");
        mockComposer.expects(once()).method("startEnvelope").with(NULL, NULL).after("open").id("start");
        mockComposer.expects(exactly(4)).method("nil").after("start").id("before");
        mockComposer.expects(once()).method("startAddresses").after("before").id("start-addresses");
        mockComposer.expects(once()).method("address").with(eq(ADDRESS_ONE_NAME), eq(ADDRESS_ONE_DOMAIN_LIST), eq(ADDRESS_ONE_MAILBOX), eq(ADDRESS_ONE_HOST)).after("start-addresses").id("address-one");
        mockComposer.expects(once()).method("endAddresses").after("address-one").id("end-addresses");
        mockComposer.expects(exactly(1)).method("nil").after("end-addresses").id("last");
        mockComposer.expects(once()).method("endEnvelope").with(NULL, eq(messageId)).after("last").id("end");
        mockComposer.expects(once()).method("closeFetchResponse").after("end");
        
        encoder.doEncode(message, composer);
    }
    
    public void testShouldComposeManyCcAddress() throws Exception {
        cc = mockManyAddresses();
        envelopExpects();
        mockComposer.expects(once()).method("openFetchResponse").with(eq((long)MSN)).id("open");
        mockComposer.expects(once()).method("startEnvelope").with(NULL, NULL).after("open").id("start");
        mockComposer.expects(exactly(4)).method("nil").after("start").id("before");
        mockComposer.expects(once()).method("startAddresses").after("before").id("start-addresses");
        mockComposer.expects(once()).method("address").with(eq(ADDRESS_ONE_NAME), eq(ADDRESS_ONE_DOMAIN_LIST), eq(ADDRESS_ONE_MAILBOX), eq(ADDRESS_ONE_HOST)).after("start-addresses").id("address-one");
        mockComposer.expects(once()).method("address").with(eq(ADDRESS_TWO_NAME), eq(ADDRESS_TWO_DOMAIN_LIST), eq(ADDRESS_TWO_MAILBOX), eq(ADDRESS_TWO_HOST)).after("address-one").id("address-two");
        mockComposer.expects(once()).method("endAddresses").after("address-two").id("end-addresses");
        mockComposer.expects(exactly(1)).method("nil").after("end-addresses").id("last");
        mockComposer.expects(once()).method("endEnvelope").with(NULL, eq(messageId)).after("last").id("end");
        mockComposer.expects(once()).method("closeFetchResponse").after("end");
        
        encoder.doEncode(message, composer);
    }
    
    public void testShouldComposeOneBccAddress() throws Exception {
        bcc = mockOneAddress();
        envelopExpects();
        mockComposer.expects(once()).method("openFetchResponse").with(eq((long)MSN)).id("open");
        mockComposer.expects(once()).method("startEnvelope").with(NULL, NULL).after("open").id("start");
        mockComposer.expects(exactly(5)).method("nil").after("start").id("before");
        mockComposer.expects(once()).method("startAddresses").after("before").id("start-addresses");
        mockComposer.expects(once()).method("address").with(eq(ADDRESS_ONE_NAME), eq(ADDRESS_ONE_DOMAIN_LIST), eq(ADDRESS_ONE_MAILBOX), eq(ADDRESS_ONE_HOST)).after("start-addresses").id("address-one");
        mockComposer.expects(once()).method("endAddresses").after("address-one").id("end-addresses");
        mockComposer.expects(once()).method("endEnvelope").with(NULL, eq(messageId)).after("end-addresses").id("end");
        mockComposer.expects(once()).method("closeFetchResponse").after("end");
        
        encoder.doEncode(message, composer);
    }
    
    public void testShouldComposeManyBccAddress() throws Exception {
        bcc = mockManyAddresses();
        envelopExpects();
        mockComposer.expects(once()).method("openFetchResponse").with(eq((long)MSN)).id("open");
        mockComposer.expects(once()).method("startEnvelope").with(NULL, NULL).after("open").id("start");
        mockComposer.expects(exactly(5)).method("nil").after("start").id("before");
        mockComposer.expects(once()).method("startAddresses").after("before").id("start-addresses");
        mockComposer.expects(once()).method("address").with(eq(ADDRESS_ONE_NAME), eq(ADDRESS_ONE_DOMAIN_LIST), eq(ADDRESS_ONE_MAILBOX), eq(ADDRESS_ONE_HOST)).after("start-addresses").id("address-one");
        mockComposer.expects(once()).method("address").with(eq(ADDRESS_TWO_NAME), eq(ADDRESS_TWO_DOMAIN_LIST), eq(ADDRESS_TWO_MAILBOX), eq(ADDRESS_TWO_HOST)).after("address-one").id("address-two");
        mockComposer.expects(once()).method("endAddresses").after("address-two").id("end-addresses");
        mockComposer.expects(once()).method("endEnvelope").with(NULL, eq(messageId)).after("end-addresses").id("end");
        mockComposer.expects(once()).method("closeFetchResponse").after("end");
        
        encoder.doEncode(message, composer);
    }
}
