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

package org.apache.james.imapserver.processor.imap4rev1;

import java.util.Collection;
import java.util.HashSet;

import org.apache.james.api.user.UserMetaDataRespository;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

public class UserMetaDataIMAPSubscriberTest extends MockObjectTestCase {
    
    private static final String EXISTING_MAILBOX = "Beta";

    private static final String NEW_MAILBOX = "Epsilon";

    private static final String USER = "A User";
    
    Collection subscriptions;
    Mock metaData;
    UserMetaDataIMAPSubscriber subscriber;
    
    protected void setUp() throws Exception {
        super.setUp();
        subscriptions = new HashSet();
        subscriptions.add("Alpha");
        subscriptions.add(EXISTING_MAILBOX);
        subscriptions.add("Gamma");
        subscriptions.add("Delta");
        metaData = mock(UserMetaDataRespository.class);
        subscriber = new UserMetaDataIMAPSubscriber((UserMetaDataRespository) metaData.proxy());
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testShouldCreateNewWhenEmpty() throws Exception {
        HashSet result = new HashSet();
        result.add(NEW_MAILBOX);
        
        metaData.expects(once()).method("getAttribute")
        .with(eq(USER), eq(UserMetaDataIMAPSubscriber.META_DATA_KEY))
        .will(returnValue(null));
        
        metaData.expects(once()).method("setAttribute").with(eq(USER), eq(result),
                eq(UserMetaDataIMAPSubscriber.META_DATA_KEY));
        
        subscriber.subscribe(USER, NEW_MAILBOX);
    }
    
    public void testShouldAddToExistingSet() throws Exception {
        HashSet result = new HashSet(subscriptions);
        result.add(NEW_MAILBOX);
        expectGetSubscriptions();
        metaData.expects(once()).method("setAttribute").with(eq(USER), eq(result), 
                eq(UserMetaDataIMAPSubscriber.META_DATA_KEY));
        
        subscriber.subscribe(USER, NEW_MAILBOX);
    }

    public void testShouldNotCallWhenAlreadySubscribed() throws Exception {
        expectGetSubscriptions();
        
        subscriber.subscribe(USER, EXISTING_MAILBOX);
    }
    
    public void testSubscriptions() throws Exception {
        
        expectGetSubscriptions();
        
        final Collection results = subscriber.subscriptions(USER);
        assertEquals(subscriptions, results);
        assertNotSame("To ensure independence, a copy should be returned.", subscriptions, results);
    }

    private void expectGetSubscriptions() {
        metaData.expects(once()).method("getAttribute")
            .with(eq(USER), eq(UserMetaDataIMAPSubscriber.META_DATA_KEY))
            .will(returnValue(subscriptions));
    }

    public void testShouldUnsubscribeWhenMailboxListed() throws Exception {
        expectGetSubscriptions();
        HashSet results = new HashSet(subscriptions);
        results.remove(EXISTING_MAILBOX);
        
        metaData.expects(once()).method("setAttribute").with(eq(USER), eq(results), 
                eq(UserMetaDataIMAPSubscriber.META_DATA_KEY));
        
        subscriber.unsubscribe(USER, EXISTING_MAILBOX);
    }

    public void testShouldNotCallWhenMailboxNoSubscribed() throws Exception {
        expectGetSubscriptions();
        
        subscriber.unsubscribe(USER, NEW_MAILBOX);
    }
}
