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

package org.apache.james.transport.remotedeliverytester;

import org.apache.james.test.mock.avalon.MockServiceManager;
import org.apache.james.test.mock.avalon.MockStore;
import org.apache.james.test.mock.james.InMemorySpoolRepository;
import org.apache.james.transport.remotedeliverytester.Tester.TestStatus;
import org.apache.mailet.Mail;

import javax.mail.MessagingException;
import javax.mail.SendFailedException;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

public abstract class AbstractRemoteDeliveryTest extends TestCase {
    private int doTest = 0;
    
    private MockServiceManager serviceManager;
    private InMemorySpoolRepository outgoingSpool;
    
    public abstract RemoteDeliveryTestable getDeliverer();
    public abstract Properties getParameters();
    
    public void test1() throws Exception {
        if (doTest == 0 || doTest == 1) doTest1(getDeliverer(), getParameters());
    }

    public void test2_0() throws Exception {
        if (doTest == 0 || doTest == 2) doTest2_0(getDeliverer(), getParameters());
    }

    public void test2() throws Exception {
        if (doTest == 0 || doTest == 2) doTest2(getDeliverer(), getParameters());
    }

    public void test3() throws Exception {
        if (doTest == 0 || doTest == 3) doTest3(getDeliverer(), getParameters());
    }

    public void test4() throws Exception {
        if (doTest == 0 || doTest == 4) doTest4(getDeliverer(), getParameters());
    }

    public void test5() throws Exception {
        if (doTest == 0 || doTest == 5) doTest5(getDeliverer(), getParameters());
    }

    public void test6() throws Exception {
        if (doTest == 0 || doTest == 6) doTest6(getDeliverer(), getParameters());
    }

    public void test7() throws Exception {
        if (doTest == 0 || doTest == 7) doTest7a(getDeliverer(), getParameters());
        if (doTest == 0 || doTest == 7) doTest7b(getDeliverer(), getParameters());
    }

    public void test8() throws Exception {
        // SOLO REMOTEDELIVERY E REMOTEDELIVERYVOID
        if (doTest == 0 || doTest == 8) doTest8(getDeliverer(), getParameters());
    }

    public void test9() throws Exception {
        if (doTest == 0 || doTest == 9) doTest9(getDeliverer(), getParameters());
    }

    public void test10() throws Exception {
        if (doTest == 0 || doTest == 10) doTest10(getDeliverer(), getParameters());
    }

    public void testMulti() throws Exception {
        if (doTest == 0 || doTest == -1) doTestMulti(getDeliverer(), getParameters());
    }
    
    protected void initEnvironment() {
        // Generate mock environment
        serviceManager = new MockServiceManager();
        MockStore mockStore = new MockStore();
        outgoingSpool = new InMemorySpoolRepository();
        // new AvalonSpoolRepository();
        mockStore.add("outgoing", outgoingSpool);
        serviceManager.put("org.apache.avalon.cornerstone.services.store.Store", mockStore);
    }
    
    protected Properties getStandardParameters() {
        Properties parameters = new Properties();
        parameters.put("delayTime", "500 msec, 500 msec, 500 msec"); // msec, sec, minute, hour
        parameters.put("maxRetries", "3");
        parameters.put("deliveryThreads", "1");
        parameters.put("debug", "true");
        parameters.put("sendpartial", "false");
        parameters.put("bounceProcessor", "bounce");
        // parameters.put("outgoing", "file://var/mail/outgoing_test/");
        return parameters;
    }

    protected String[][] addServers(Tester tester, String[][] servers,
            boolean sendValid) {
        for (int i = 0; i < servers.length; i++)
            // for (int j = 0; j < servers[1 + i].length; j++) tester.addDomainServer(servers[0][i], servers[1 + i][j], new TransportRule.NameExpression(sendValid));
            for (int j = 1; j < servers[i].length; j++)
                tester.addDomainServer(servers[i][0], servers[i][j],
                        new TransportRule.NameExpression(sendValid));

        return servers;
    }

    protected int waitEmptySpool(int maxWait) {
        if (maxWait == 0) maxWait = -1;
        while (outgoingSpool.size() > 0 && (maxWait > 0 || maxWait == -1)) {
            synchronized (this) {
                try {
                    wait(1000);
                } catch (InterruptedException e) {}
            }
            if (maxWait != -1) maxWait -= 1000;
        }

        
        if (outgoingSpool.size() > 0) {
            Iterator i = outgoingSpool.list();
            while (i.hasNext()) {
                String key = (String) i.next();
                Mail m = null;
                try {
                    m = outgoingSpool.retrieve(key);
                    System.err.println("Still in outgoing: "+key+" S:"+m.getState()+" E:"+m.getErrorMessage()+" F:"+m.getSender()+" T:"+m.getRecipients()+" M:"+m.getMessage().getContent());
                } catch (MessagingException e) {
                    e.printStackTrace();
                    System.err.println("Still in outgoing: "+key+" NULL");
                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("Still in outgoing: "+key+" IOException");
                }
            }
        
        }

        return outgoingSpool.size();
    }

    /**
     * 
     * @param status
     * @param sends
     *            Email send attempts number. if < 0 will take the minimum number.
     * @param maxConnection
     *            Max attempts to connect the server. If < 0 defaults to the minimum.
     */
    protected void assertWhole(Tester.TestStatus status, int sends, int maxConnection) {
        if (sends >= 0)
            assertEquals(sends, status.getTransportSendCount());
        else
            assertTrue(status.getTransportSendCount() >= -sends);
        if (maxConnection >= 0) {
            assertTrue(maxConnection >= status.getTransportConnectionCount());
            assertTrue(maxConnection >= status.getTransportCloseCount());
        } else {
            assertTrue(-maxConnection <= status.getTransportConnectionCount());
            assertTrue(-maxConnection <= status.getTransportCloseCount());
        }
        assertEquals(status.getTransportConnectionCount(), status
                .getTransportCloseCount());
    }

    /**
     * 
     * @param status
     * @param server
     * @param sends
     *            Email send attempts number. if < 0 will take the minimum number.
     * @param maxConnection
     *            Max attempts to connect the server. If < 0 defaults to the minimum.
     */
    protected void assertServer(Tester.TestStatus status, String server, int sends, int maxConnection) {
        if (sends >= 0)
            assertEquals(sends, status.getTransportServerSendCount(server));
        else
            assertTrue(status.getTransportServerSendCount(server) >= -sends);
        if (maxConnection >= 0) {
            assertTrue(maxConnection >= status.getTransportServerConnectionCount(server));
            assertTrue(maxConnection >= status.getTransportServerCloseCount(server));
        } else {
            assertTrue(-maxConnection <= status.getTransportServerConnectionCount(server));
            assertTrue(-maxConnection <= status.getTransportServerCloseCount(server));
        }
        assertEquals(status.getTransportServerConnectionCount(server), status.getTransportServerCloseCount(server));
    }
    
    /**
     * Assert procmail result.
     * 
     * @param pmail
     * @param state
     * @param sends
     *            Email send attempts number. if < 0 will take the minimum number.
     * @param minBounce
     * @param lastServer
     */
    protected void assertProcMail(ProcMail pmail, int state, int sends, int minBounce, String lastServer) {
        if (sends >= 0) assertEquals(sends, pmail.getSendCount());
        else assertTrue(pmail.getSendCount() >= -sends);
        assertTrue(pmail.getBounceCount() >= minBounce);
        assertEquals(state, pmail.getState());
        assertEquals(lastServer, pmail.getSendCount() > 0 ? pmail.getSendServer(pmail.getSendCount() - 1) : null);
        assertEquals(0, pmail.getErrorFlags());
    }

    protected void doTest1(RemoteDeliveryTestable rd, Properties params) throws Exception {
        initEnvironment();
        Tester tester = new Tester(rd);
        tester.init(serviceManager, params);
        
        // test initialization
        tester.addDomainServer("test.it", "smtp://mail.test.it:25");
        
        ProcMail.Listing mails = tester.service("mail", "from@test.it", "to@test.it", "Subject: test\r\nContent-Transfer-Encoding: plain\r\n\r\nbody");

        // Wait for empty spool
        assertEquals(0, waitEmptySpool(5000));
        
        // checks
        assertWhole(tester.getTestStatus(), 1, 1);
        assertServer(tester.getTestStatus(), "smtp://mail.test.it:25", 1, 1);
        assertEquals(1, tester.getProcMails().size());
        assertProcMail(mails.get("to@test.it"), ProcMail.STATE_SENT, 1, 0, "smtp://mail.test.it:25");
    }

    protected void doTest2_0(RemoteDeliveryTestable rd, Properties params) throws Exception {
        initEnvironment();
        Tester tester = new Tester(rd);
        tester.init(serviceManager, params);
        
        // test initialization
        tester.addDomainServer("test.it", "smtp://mail.test.it:25", new TransportRule.Default() {
            public void onConnect(Tester.TestStatus status, String server) throws MessagingException {
                // Manda una connessione al connect, solo la prima connessione
                if (status.getTransportServerConnectionCount(server) == 0) throw new MessagingException("Connect");
            }
        });
        
        ProcMail.Listing mails = tester.service("mail", "from@test.it", new String[] {"to@test.it", "other@test.it"}, "Subject: test\r\nContent-Transfer-Encoding: plain\r\n\r\nbody", new TransportRule.Default() {
            public void onSendMessage(TestStatus status, String server, ProcMail.Listing pmails) throws MessagingException, SendFailedException {
                // Manda una connessione al send, solo al primo send
                if (status.getTransportServerSendCount(server) == 0) throw new MessagingException("Send");
            }
        });

        // Wait for empty spool
        assertEquals(0, waitEmptySpool(5000));
        
        // checks
        assertWhole(tester.getTestStatus(), 4, 3);
        assertServer(tester.getTestStatus(), "smtp://mail.test.it:25", 4, 3);
        assertEquals(2, tester.getProcMails().size());
        assertProcMail(mails.get("to@test.it"), ProcMail.STATE_SENT, 2, 0, "smtp://mail.test.it:25");
        assertProcMail(mails.get("other@test.it"), ProcMail.STATE_SENT, 2, 0, "smtp://mail.test.it:25");
    }
    
    protected void doTest2(RemoteDeliveryTestable rd, Properties params) throws Exception {
        initEnvironment();
        Tester tester = new Tester(rd);
        tester.init(serviceManager, params);
        
        String[][] servers = addServers(tester, new String[][] {
                { "test.it", "smtp://mail-me-1-ok.*-me-1-ok.test.it:25" }
        }, true);
        

        ProcMail.Listing mails = tester.service("mail", "from@test.it", new String[] {"to@test.it", "other@test.it"}, "Subject: test\r\nContent-Transfer-Encoding: plain\r\n\r\nbody");

        // Wait for empty spool
        assertEquals(0, waitEmptySpool(5000));
        
        // Checks
        assertWhole(tester.getTestStatus(), 4, 3);
        assertServer(tester.getTestStatus(), servers[0][1], 4, 3);
        assertEquals(2, tester.getProcMails().size());
        assertProcMail(mails.get("to@test.it"), ProcMail.STATE_SENT, 2, 0, servers[0][1]);
        assertProcMail(mails.get("other@test.it"), ProcMail.STATE_SENT, 2, 0, servers[0][1]);
    }
    
    /**
     * Permanent error fo 1/2 addresses.
     * 
     * @param rd
     * @throws Exception
     */
    protected void doTest3(RemoteDeliveryTestable rd, Properties params) throws Exception {
        initEnvironment();
        Tester tester = new Tester(rd);
        tester.init(serviceManager, params);
        
        String[][] servers = addServers(tester, new String[][] {
                { "test.it", "smtp://s1-ok.a*-smtpafe400.test.it:25" }
        }, true);
        
        ProcMail.Listing mails = tester.service("mail", "from@test.it", new String[] {"a@test.it", "b@test.it"}, "Subject: test\r\nContent-Transfer-Encoding: plain\r\n\r\nbody");

        // Wait for empty spool
        assertEquals(0, waitEmptySpool(5000));
        
        // Checks
        assertWhole(tester.getTestStatus(), 2, 1);
        assertServer(tester.getTestStatus(), servers[0][1], 2, 1);
        assertEquals(2, tester.getProcMails().size());
        assertProcMail(mails.get("a@test.it"), ProcMail.STATE_SENT_ERROR, 1, 1, servers[0][1]);
        assertProcMail(mails.get("b@test.it"), ProcMail.STATE_SENT, 1, 0, servers[0][1]);
    }

    /**
     * Temporary error for 1/2 addresses.
     * 
     * @param rd
     * @throws Exception
     */
    protected void doTest4(RemoteDeliveryTestable rd, Properties params) throws Exception {
        initEnvironment();
        Tester tester = new Tester(rd);
        tester.init(serviceManager, params);
        
        String[][] servers = addServers(tester, new String[][] {
                { "test.it", "smtp://s1-ok.a*-smtpafe400V.test.it:25" }
        }, true);
        
        ProcMail.Listing mails = tester.service("mail", "from@test.it", new String[] {"a@test.it", "b@test.it"}, "Subject: test\r\nContent-Transfer-Encoding: plain\r\n\r\nbody");

        // Wait for empty spool
        assertEquals(0, waitEmptySpool(10000));
        
        // Checks
        assertWhole(tester.getTestStatus(), 5, 4);
        assertServer(tester.getTestStatus(), servers[0][1], 5, 4);
        assertEquals(2, tester.getProcMails().size());
        assertProcMail(mails.get("a@test.it"), ProcMail.STATE_SENT_ERROR, 4, 1, servers[0][1]);
        assertProcMail(mails.get("b@test.it"), ProcMail.STATE_SENT, 1, 0, servers[0][1]);
    }

    /**
     * 1 Temporary error + 1 Permanent error
     * 
     * @param rd
     * @throws Exception
     */
    protected void doTest5(RemoteDeliveryTestable rd, Properties params) throws Exception {
        initEnvironment();
        Tester tester = new Tester(rd);
        tester.init(serviceManager, params);
        
        String[][] servers = addServers(tester, new String[][] {
                { "test.it", "smtp://s1-ok.a*-smtpafe411V.b*-smtpafe500.test.it:25" }
        }, true);
        
        ProcMail.Listing mails = tester.service("mail", "from@test.it", new String[] {"a@test.it", "b@test.it"}, "Subject: test\r\nContent-Transfer-Encoding: plain\r\n\r\nbody");

        // Wait for empty spool
        assertEquals(0, waitEmptySpool(10000));
        
        // Checks
        try {
            assertWhole(tester.getTestStatus(), 5, 4);
            assertServer(tester.getTestStatus(), servers[0][1], 5, 4);
            assertEquals(2, tester.getProcMails().size());
            assertProcMail(mails.get("a@test.it"), ProcMail.STATE_SENT_ERROR, 4, 1, servers[0][1]);
            assertProcMail(mails.get("b@test.it"), ProcMail.STATE_SENT_ERROR, 1, 1, servers[0][1]);
        } catch (AssertionFailedError e) {
            // TEMPORARILY add a dump stack on failure to 
            // see if we have a deadlock (unlikely) or simply the
            // notification is not working properly. (see JAMES-850)
            
            // Use reflection to invoke java 1.5 stack functions.
            try {
                Method m = Thread.class.getMethod("getAllStackTraces", null);
                Map stackDump = (Map) m.invoke(null, null);
                Set threads = stackDump.keySet();
                for (Iterator i = threads.iterator(); i.hasNext(); ) {
                    Thread th = (Thread) i.next();
                    System.out.println("Thread "+th);
                    StackTraceElement[] stack = (StackTraceElement[]) stackDump.get(th);
                    for (int k = 0; k < stack.length; k++) {
                        System.out.println("STE: "+stack[k]);
                    }
                }
            } catch (Exception reflectException) {
            }
            throw e;
        }
    }

    /**
     * Mixed
     * 
     * @param rd
     * @throws Exception
     */
    protected void doTest6(RemoteDeliveryTestable rd, Properties params) throws Exception {
        initEnvironment();
        Tester tester = new Tester(rd);
        tester.init(serviceManager, params);
        
        String[][] servers = addServers(tester, new String[][] {
                { "test.it", "smtp://s1-ok.a*-smtpafe400V.b*-smtpafe500.test.it:25" },
                { "test2.it", "smtp://s1-ok.a*-smtpafe400V.b*-smtpafe500.test2.it:25" }
        }, true);
        
        ProcMail.Listing mails1 = tester.service("mail1", "from1@test.it", new String[] {"a@test.it", "b@test.it"}, "Subject: test1\r\nContent-Transfer-Encoding: plain\r\n\r\nbody");
        ProcMail.Listing mails2 = tester.service("mail2", "from2@test.it", new String[] {"c@test2.it", "b@test2.it"}, "Subject: test2\r\nContent-Transfer-Encoding: plain\r\n\r\nbody");
        ProcMail.Listing mails3 = tester.service("mail3", "from3@test.it", new String[] {"a@test.it", "b@test.it"}, "Subject: test3\r\nContent-Transfer-Encoding: plain\r\n\r\nbody");
        ProcMail.Listing mails4 = tester.service("mail4", "from4@test.it", new String[] {"c@test2.it", "b@test2.it"}, "Subject: test4\r\nContent-Transfer-Encoding: plain\r\n\r\nbody");

        // Wait for empty spool
        assertEquals(0, waitEmptySpool(5000));
        
        // Checks
        assertWhole(tester.getTestStatus(), 14, 10);
        assertServer(tester.getTestStatus(), servers[0][1], 10, 8);
        assertServer(tester.getTestStatus(), servers[1][1], 4, 2);
        assertEquals(8, tester.getProcMails().size());
        assertProcMail(mails1.get("a@test.it"), ProcMail.STATE_SENT_ERROR, 4, 1, servers[0][1]);
        assertProcMail(mails1.get("b@test.it"), ProcMail.STATE_SENT_ERROR, 1, 1, servers[0][1]);
        assertProcMail(mails2.get("b@test2.it"), ProcMail.STATE_SENT_ERROR, 1, 1, servers[1][1]);
        assertProcMail(mails2.get("c@test2.it"), ProcMail.STATE_SENT, 1, 0, servers[1][1]);
        assertProcMail(mails3.get("a@test.it"), ProcMail.STATE_SENT_ERROR, 4, 1, servers[0][1]);
        assertProcMail(mails3.get("b@test.it"), ProcMail.STATE_SENT_ERROR, 1, 1, servers[0][1]);
        assertProcMail(mails4.get("b@test2.it"), ProcMail.STATE_SENT_ERROR, 1, 1, servers[1][1]);
        assertProcMail(mails4.get("c@test2.it"), ProcMail.STATE_SENT, 1, 0, servers[1][1]);
    }

    /**
     * NPE during send
     * 
     * @param rd
     * @throws Exception
     */
    protected void doTest7a(RemoteDeliveryTestable rd, Properties params) throws Exception {
        initEnvironment();
        Tester tester = new Tester(rd);
        tester.init(serviceManager, params);
        
        String[][] servers = addServers(tester, new String[][] {
                { "test.it", "smtp://s1-ok.a*-null.test.it:25" },
        }, true);
        
        ProcMail.Listing mails1 = tester.service("M0", "from0@test.it", new String[] {"a@test.it", "b@test.it"}, "Subject: test1\r\nContent-Transfer-Encoding: plain\r\n\r\nbody");

        // Wait for empty spool
        //assertEquals(0, waitEmptySpool(5000));
        assertEquals(0, waitEmptySpool(0));
        
        // Checks
        assertWhole(tester.getTestStatus(), 2, 1);
        assertServer(tester.getTestStatus(), servers[0][1], 2, 1);
        assertEquals(2, tester.getProcMails().size());
        assertProcMail(mails1.get("a@test.it"), ProcMail.STATE_SENT_ERROR, 1, 1, servers[0][1]);
        assertProcMail(mails1.get("b@test.it"), ProcMail.STATE_SENT_ERROR, 1, 1, servers[0][1]);
    }
    
    protected void doTest7b(RemoteDeliveryTestable rd, Properties params) throws Exception {
            initEnvironment();
            Tester tester = new Tester(rd);
            tester.init(serviceManager, params);
            
            String[][] servers = addServers(tester, new String[][] {
                    { "test.it", "smtp://s1-null.**-ok.test.it:25" },
            }, true);
            
            ProcMail.Listing mails1 = tester.service("M0", "from0@test.it", new String[] {"a@test.it", "b@test.it"}, "Subject: test1\r\nContent-Transfer-Encoding: plain\r\n\r\nbody");

            // Wait for empty spool
            assertEquals(0, waitEmptySpool(5000));
            
            // Controlli
            assertWhole(tester.getTestStatus(), 0, 1);
            assertServer(tester.getTestStatus(), servers[0][1], 0, 1);
            assertEquals(2, tester.getProcMails().size());
            assertProcMail(mails1.get("a@test.it"), ProcMail.STATE_IDLE, 0, 1, null);
            assertProcMail(mails1.get("b@test.it"), ProcMail.STATE_IDLE, 0, 1, null);
        }

    /**
     * Multiple mx servers for a single domain. One failing with a 400V on the first reception.
     *
     * This test expect the RemoteDelivery to check the next server on a temporary error on the first
     * server. Other remote delivery implementations could use different strategies.
     */
    protected void doTest8(RemoteDeliveryTestable rd, Properties params) throws Exception {
        initEnvironment();
        Tester tester = new Tester(rd);
        tester.init(serviceManager, params);
        
        String[][] servers = addServers(tester, new String[][] {
            // a.it: all addresses ok.
            { "a.it", "smtp://s1-ok.a.it:25", "smtp://s2-ok.a.it:25" },
            
            // b.it: one server always reply smtpafe400V, the other works.
            { "b.it", "smtp://s1-ok.**-smtpafe400V.b.it:25", "smtp://s2-ok.b.it:25" },
        }, true);
        
        ProcMail.Listing mails = tester.service("M0", "o@test.it", new String[] {"a@a.it", "b@b.it"}, "Subject: test0\r\nContent-Transfer-Encoding: plain\r\n\r\nbody");
        //ProcMail.Listing mails = tester.service("M0", "o@test.it", new String[] {"b@b.it"}, "Subject: test0\r\nContent-Transfer-Encoding: plain\r\n\r\nbody");
        // Wait for empty spool
        assertEquals(0, waitEmptySpool(5000));
        
        // Checks
        assertWhole(tester.getTestStatus(), -2, -2); // Almeno 2 connessioni deve averle fatte
        assertServer(tester.getTestStatus(), servers[0][1], 1, 1);
        assertServer(tester.getTestStatus(), servers[1][1], -1, -1);
        assertEquals(2, tester.getProcMails().size());
        assertProcMail(mails.get("a@a.it"), ProcMail.STATE_SENT, 1, 0, servers[0][1]);
        assertProcMail(mails.get("b@b.it"), ProcMail.STATE_SENT, -1, 0, servers[1][2]);
    }

    /**
     * Multiple MX server for a domain. 
     */
    protected void doTest9(RemoteDeliveryTestable rd, Properties params) throws Exception {
        initEnvironment();
        Tester tester = new Tester(rd);
        tester.init(serviceManager, params);
        
        String[][] servers = addServers(tester, new String[][] {
            // a.it: all addresses ok.
            { "a.it", "smtp://s1-ok.a.it:25", "smtp://s2-ok.a.it:25" },
            
            // b.it: one server always reply smtpsfe500V, the other works.
            { "b.it", "smtp://s1-me.b.it:25", "smtp://s2-ok.b.it:25" },
        }, true);
        
        ProcMail.Listing mails = tester.service("M0", "o@test.it", new String[] {"a@a.it", "b@b.it"}, "Subject: test0\r\nContent-Transfer-Encoding: plain\r\n\r\nbody");
        //ProcMail.Listing mails = tester.service("M0", "o@test.it", new String[] {"b@b.it"}, "Subject: test0\r\nContent-Transfer-Encoding: plain\r\n\r\nbody");
        // Wait for empty spool
        assertEquals(0, waitEmptySpool(5000));
        
        // Checks
        assertWhole(tester.getTestStatus(), -2, -3); // Almeno 2 connessioni deve averle fatte
        assertServer(tester.getTestStatus(), servers[0][1], 1, 1);
        assertServer(tester.getTestStatus(), servers[1][1], 0, 1);
        assertServer(tester.getTestStatus(), servers[1][2], 1, 1);
        assertEquals(2, tester.getProcMails().size());
        assertProcMail(mails.get("a@a.it"), ProcMail.STATE_SENT, 1, 0, servers[0][1]);
        assertProcMail(mails.get("b@b.it"), ProcMail.STATE_SENT, 1, 0, servers[1][2]);
    }

    /**
     * IO Exception 
     */
    protected void doTest10(RemoteDeliveryTestable rd, Properties params) throws Exception {
   // creazione tester
        initEnvironment();
        Tester tester = new Tester(rd);
        tester.init(serviceManager, params);
        
        String[][] servers = addServers(tester, new String[][] {
            // i.it: ioexception (during connect or send for "a", depending on the server) 
            { "i.it", "smtp://s1-io.i.it:25",  "smtp://s2-ok.a*-io.i.it:25"},
        }, true);
        
        //ProcMail.Listing mails = tester.service("M0", "o@test.it", new String[] {"a@i.it", "b@i.it"}, "Subject: test0\r\nContent-Transfer-Encoding: plain\r\n\r\nbody");
        ProcMail.Listing mails = tester.service("M0", "o@test.it", new String[] {"b@i.it"}, "Subject: test0\r\nContent-Transfer-Encoding: plain\r\n\r\nbody");

        // Wait for empty spool
        assertEquals(0, waitEmptySpool(5000));
        
        // Checks
        assertWhole(tester.getTestStatus(), 1, 2); 
        assertServer(tester.getTestStatus(), servers[0][1], 0, 1);
        assertServer(tester.getTestStatus(), servers[0][2], 1, 1);
        assertEquals(1, tester.getProcMails().size());
        //assertProcMail(mails.get("a@i.it"), ProcMail.STATE_SENT_ERROR, 1, 0, servers[0][1]);
        assertProcMail(mails.get("b@i.it"), ProcMail.STATE_SENT, -1, 0, servers[0][2]);
    }

    protected String[][] getTestMultiServers() {
        return new String[][] {
            // a.it: ok for every address
            { "a.it", "smtp://s1-ok.a.it:25", "smtp://s2-ok.a.it:25" },
            
            // b.it: one server throws ME on every call, the other works.
            //{ "b.it", "smtp://s1-ok.**-smtpafe400V.b.it:25", "smtp://s2-ok.b.it:25" },  <- Questo su MailSender non lo puo' fare
            { "b.it", "smtp://s1-me.b.it:25", "smtp://s2-ok.b.it:25" },
            
            // c.it: both servers replies smtpafe400V.
            { "c.it", "smtp://s1-ok.**-smtpafe400V.c.it:25", "smtp://s2-ok.**-smtpafe400V.c.it:25" },

            // d.it: Messaging exception once every 2 attempts.
            { "d.it", "smtp://s1-me-1-ok-1-rpt.*-me-1-ok-1-rpt.d.it:25" }, 
            
            // e.it: addresses starting with "a" are rejected with a smtpafe400
            { "e.it", "smtp://s1-ok.a*-smtpafe400.e.it:25" },
            
            // f.it: addresses starting with "a" are rejected with a smtpafe400V from the first server
            //       the second server do the same with "b" addresses.
            { "f.it", "smtp://s1-ok.a*-smtpafe400V.f.it:25", "smtp://s2-ok.b*-smtpafe400V.f.it:25" },
            
            // g.it: "a" not accepted (temporary), "b" not accepted (permanent), "c" half temporary exceptions
            { "g.it", "smtp://s1-ok.a*-smtpafe411V.b*-smtpafe500.test.c*-smtpafe411V-1-ok-1-rpt.g.it:25" },
            
            // h.it: null pointer exception (during connect for one server and during send for the other) 
            { "h.it", "smtp://s1-null.h.it:25",  "smtp://s2-ok.**-null.h.it:25"},

            // i.it: ioexception during connect on one server, the other works. 
            { "i.it", "smtp://s1-io.i.it:25",  "smtp://s2-ok.i.it:25"},
        };
    }
    
    protected String[][] getTestMultiEmails() {
        return new String[][] {
            { "a.it", "a@a.it", "1", "b@a.it", "1", "c@a.it", "1"},
            { "b.it", "a@b.it", "1", "b@b.it", "1", "c@b.it", "1"},
            { "c.it", "a@c.it", "0", "b@c.it", "0", "c@c.it", "0"},
            { "d.it", "a@d.it", "1", "b@d.it", "1", "c@d.it", "1"},
            { "e.it", "a@e.it", "0", "b@e.it", "1", "c@e.it", "1"},
            { "f.it", "a@f.it", "1", "b@f.it", "1", "c@f.it", "1"},
            { "g.it", "a@g.it", "0", "b@g.it", "0", "c@g.it", "1"},
            { "h.it", "a@h.it", "0", "b@h.it", "0", "c@h.it", "0"},
            { "i.it", "a@i.it", "1", "b@i.it", "1", "c@i.it", "1"},
        };
    }
    
    protected void doTestMulti(RemoteDeliveryTestable rd, Properties params) throws Exception {
        // Number of attempts
        int loopCount = 20;
        // Wait time between attempts
        int loopWait = 0;
        // Max wait betwen attempts (for the randomization)
        int loopWaitRandom = 100;
        // Max number of recipients per mail
        int maxRecipientsPerEmail = 3;
        // Probability for the recipients to be in the same domain
        int probRecipientsSameDomain = 30;

        // creazione tester
        initEnvironment();
        Tester tester = new Tester(rd);
        tester.init(serviceManager, params);
        
        // String[][] servers = 
        addServers(tester, getTestMultiServers(), true);
        String[][] emails = getTestMultiEmails();
        
        Random rnd = new Random();
        ProcMail.Listing[] results = new ProcMail.Listing[loopCount];
        for (int i = 0; i < loopCount; i++) {
            // Calcolo recipients
            ArrayList rcpts = new ArrayList();
            int rcptn = rnd.nextInt(maxRecipientsPerEmail) + 1;
            if (rnd.nextInt(100) < probRecipientsSameDomain) {
                // same domain
                int dom = rnd.nextInt(emails.length);
                // for the domain we don't have more email, takes other domains emails
                if (rcptn >= (emails[dom].length - 1) / 2) for (int j = 1; j < emails[dom].length; j += 2) rcpts.add(emails[dom][j]);
                else {
                    boolean[] got = new boolean[(emails[dom].length - 1) / 2];
                    int done = 0;
                    while (done < rcptn) {
                        int t = rnd.nextInt((emails[dom].length - 1) / 2);
                        if (!got[t]) {
                            rcpts.add(emails[dom][t * 2 + 1]);
                            got[t] = true;
                            done++;
                        }
                    }
                }
            } else {
                // multiple domains
                boolean got[][] = new boolean[emails.length][];
                for (int j = 0; j < emails.length; j++) got[j] = new boolean[1 + (emails[j].length - 1) / 2];
                int done = 0;
                int gotdepth = 0;
                while (done < rcptn && gotdepth < got.length) {
                    int dom;
                    do {dom = rnd.nextInt(got.length);} while (got[dom][0]);
                    int t;
                    do {t = rnd.nextInt(got[dom].length - 1);} while (got[dom][t + 1]);
                    rcpts.add(emails[dom][t * 2 + 1]);
                    got[dom][t + 1] = true;
                    for (int j = 1; j < got[dom].length && got[dom][j]; j++) if (j == got[dom].length - 1) got[dom][0] = true;
                    done ++;
                }
            }
            
            System.out.println("------");
            for (int j = 0; j < rcpts.size(); j++) System.out.println(rcpts.get(j)); 
            
            results[i] = tester.service("M" + i, i + "@test.it", (String[]) rcpts.toArray(new String[0]), "Subject: test" + i + "\r\nContent-Transfer-Encoding: plain\r\n\r\nbody");
            synchronized(this) {
                if (loopWait > 0) wait(loopWait);
                if (loopWaitRandom > 0) wait(rnd.nextInt(loopWaitRandom)+1);
            }
        }
        
        // Wait for empty spool
        assertEquals(0, waitEmptySpool(30000));
        
        Hashtable emailsRes = new Hashtable();
        for (int i = 0; i < emails.length; i ++) for (int j = 1; j < emails[i].length; j += 2) emailsRes.put(emails[i][j], emails[i][j + 1]);
        
        boolean error = false;
        for (int i = 0; i < results.length; i++) {
            System.out.println("Call#" + i);
            for (int j = 0; j < results[i].size(); j++) {
                ProcMail pmail = results[i].get(j);
                System.out.print(pmail.getKey() + " status:" + (pmail.getState() == ProcMail.STATE_SENT ? "SENT" : pmail.getState() == ProcMail.STATE_SENT_ERROR ? "ERR" : "" + pmail.getState() ) + " sends:" + pmail.getSendCount());
                String res = (String) emailsRes.get(pmail.getRecipient().toString());
                if (pmail.getState() == ProcMail.STATE_IDLE) pmail.setState(ProcMail.STATE_SENT_ERROR);
                if (pmail.getState() != (res.equals("0") ? ProcMail.STATE_SENT_ERROR: ProcMail.STATE_SENT)) {
                    System.out.print(" <<< ERROR");
                    error = true;
                }
                System.out.print("\n");
            }
        }
        assertFalse(error);
    }

}
