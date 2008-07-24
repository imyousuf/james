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

import org.apache.avalon.framework.service.ServiceManager;
import org.apache.james.core.MailImpl;
import org.apache.james.dnsserver.TemporaryResolutionException;
import org.apache.james.services.DNSServer;
import org.apache.mailet.HostAddress;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Provider;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.Provider.Type;
import javax.mail.internet.MimeMessage;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

/**
 * Email lifecycle 
 * - in mailet.service it is accepted and sent to outgoing spool (an outgoing.store for each recipient).
 * - another thread pool, in mailtet.run it is accepted from the outgoing spool and sent to the "deliver" method.
 * - "deliver" calls transport.connect and transport.sendMessage from javaMail
 * - on delivery error a call to "failMessage" is done. failMessage will call mailetContext.sendMail or mailetContext.bounce to send a delivery failure receipt.
 * - Back to "run" results in outgoing.remove (permanent result) or outgoing.store (temporary failure)
 */
public class Tester {
    private RemoteDeliveryTestable remoteDelivery;
    private TesterMailetConfig mailetConfig;
    
    private HashMap procMails = new HashMap(); 
    private HashMap procMailsByName = new HashMap();
    
    private TransportRule genericRule = null;
    private Hashtable serverRules = new Hashtable();
    private TestStatus testStatus = new TestStatus();

    private Hashtable hostAddresses = new Hashtable();
    private Hashtable domainsAssociated = new Hashtable();
    private DNSServer dnsServer = new DNSServer() {

        public Collection findMXRecords(String hostname)
                throws TemporaryResolutionException {
            throw new UnsupportedOperationException("DNSServer stub");
        }

        public Collection findTXTRecords(String hostname) {
            throw new UnsupportedOperationException("DNSServer stub");
        }

        public InetAddress[] getAllByName(String host)
                throws UnknownHostException {
            throw new UnsupportedOperationException("DNSServer stub");
        }

        public InetAddress getByName(String host)
                throws UnknownHostException {
            throw new UnsupportedOperationException("DNSServer stub");
        }

        public String getHostName(InetAddress addr) {
            throw new UnsupportedOperationException("DNSServer stub");
        }

        public InetAddress getLocalHost() throws UnknownHostException {
            throw new UnsupportedOperationException("DNSServer stub");
        }

        public Iterator getSMTPHostAddresses(final String domainName)
                throws TemporaryResolutionException {
            return onMailetContextGetSMTPHostAddresses(domainName);
        }
        
    };
    
    private static Tester instance;
    
    /**
     * Get the last static instance.
     * Works only in a single Tester environment.
     */
    public static Tester getInstance() {
        return instance;
    }

    public Tester(RemoteDeliveryTestable remoteDelivery) {
        this.remoteDelivery = remoteDelivery;
        remoteDelivery.setRemoteDeliveryTester(this);
        
        //if (Tester.instance == null) 
        Tester.instance = this;
    }
        
    public void init(ServiceManager serviceManager, Properties mailetConfigProperties) throws MessagingException {
        mailetConfig = new TesterMailetConfig(this, mailetConfigProperties, serviceManager);
        remoteDelivery.init(mailetConfig);
        // do this after the init because init would override it.
        remoteDelivery.setDNSServer(dnsServer);
        
        if (mailetConfig.getWrappedSpoolRepository() != null) log("DEBUG", "Init WrappedSpoolRepository OK");
        else {
            log("WARN", "Init WrappedSpoolRepository ERROR");
            //throw new IllegalStateException();
        }
        
        Session session = obtainSession(new Properties());
        Transport t = session.getTransport("smtp");
        if (t instanceof SMTPTransport) log("DEBUG", "Init WrappedSMTPTransport OK");
        else {
            log("ERROR", "Init WrappedSMTPTransport ERROR");
            throw new IllegalStateException();
        }
    }
    
    public Session obtainSession(Properties props) {
        /**
         * Javamail1.4 allows for providers to be specified
         */
        
        props.put("mail.smtp.class", SMTPTransport.class.getName());
        props.put("Tester", this);
        Session s = Session.getInstance(props);
        // Session s = Session.getDefaultInstance(props);
        try {
            if (!((s.getTransport("smtp")) instanceof SMTPTransport))
                s.setProvider(new Provider(Type.TRANSPORT, "smtp", SMTPTransport.class.getName(), "test", "0"));
        } catch (NoSuchProviderException e) {
          // Let's do it twice, don't remember why.
          try {
                        s.setProvider(new Provider(Type.TRANSPORT, "smtp", SMTPTransport.class.getName(), "test", "0"));
                    } catch (NoSuchProviderException e1) {
                        e1.printStackTrace();
                    }
            e.printStackTrace();
        }
        return s;
    }

    protected void log(String type, String message) {
        System.out.println("<RDT:" + type + "> " + message);
    }
    
    public ProcMail.Listing service(String name, String fromMail, String toMail, String body) throws MessagingException {
        return service(name, fromMail, new String[] {toMail}, body, null);
    }
        
    public ProcMail.Listing service(String name, String fromMail, String toMail, String body, TransportRule rule) throws MessagingException {
        return service(name, fromMail, new String[] {toMail}, body, rule);
    }

    public ProcMail.Listing service(String name, String fromMail, String[] toMail, String body) throws MessagingException {
        return service(name, fromMail, toMail, body, null);
    }
        
    public ProcMail.Listing service(String name, String fromMail, String[] toMail, String body, TransportRule rule) throws MessagingException {
        MimeMessage mm = new MimeMessage(Session.getInstance(new Properties()));
        mm.setSubject(name);
        mm.setText(body);
//      MimeMessageInputStreamSource mmis = new MimeMessageInputStreamSource("test", new SharedByteArrayInputStream((body).getBytes()));
        Collection recipients = new ArrayList();
        for (int i = 0; i < toMail.length; i++) recipients.add(new MailAddress(toMail[i]));
//      MailImpl mail = new MailImpl(name, new MailAddress(fromMail), recipients, new MimeMessageWrapper(mmis));
        MailImpl mail = new MailImpl(name, new MailAddress(fromMail), recipients, mm);
        return service(mail, rule);
    }
    
    public ProcMail.Listing service(Mail mail) throws MessagingException {
        return service(mail, null);
    }

    public ProcMail.Listing service(Mail mail, TransportRule rule) throws MessagingException {
        ProcMail.Listing listing = new ProcMail.Listing();
        Iterator recipients = mail.getRecipients().iterator();
        while (recipients.hasNext()) {
            MailAddress address = (MailAddress)recipients.next();
            ProcMail pmail = new ProcMail(this, mail, address);
            pmail.setTransportRule(rule);
            storeProcMail(pmail);
            listing.add(pmail);
        }
        remoteDelivery.service(mail);
        return listing;
    }

    public TestStatus getTestStatus() {
        return testStatus;
    }

    public Collection getProcMails() {
        return procMails.values();
    }
    
    public ProcMail getProcMail(Mail mail, MailAddress recipient) {
        return getProcMailByKey(ProcMail.getKey(mail, recipient));
    }
    
    public ProcMail getProcMail(Message message, Address address) {
        return getProcMailByKey(ProcMail.getKey(message, address));
    }
        
    public ProcMail getProcMailByKey(String key) {
        return (ProcMail) procMails.get(key);
    }
    
    public ProcMail getProcMail(String name) {
        return (ProcMail) procMailsByName.get(name);
    }

    public void storeProcMail(ProcMail pmail) {
        procMails.put(pmail.getKey(), pmail);
        procMailsByName.put(pmail.getMailName(), pmail);
        log("DEBUG", "Stored procmail: "+pmail.getKey());
    }
    
    private String getServerName(URLName urlname) {
        return urlname.toString();
    }
    
    public void setGenericRule(TransportRule rule) {
        genericRule = rule;
    }

    public void addDomainServer(String domain, String url) {
        if (!hostAddresses.containsKey(domain)) hostAddresses.put(domain, new Vector());
        ((Vector)hostAddresses.get(domain)).add(new HostAddress(domain, url));
        domainsAssociated.put(url, domain);
    }

    public void addServerRule(String url, TransportRule rule) {
        serverRules.put(getServerName(new URLName(url)), rule);
    }

    public void addDomainServer(String domain, String url, TransportRule rule) {
        addDomainServer(domain, url);
        addServerRule(url, rule);
    }
    
    public String getDomainAssociated(String server) {
        return (String) domainsAssociated.get(server);
    }

    public Iterator onMailetContextGetSMTPHostAddresses(String domainName) {
        return ((List)hostAddresses.get(domainName)).iterator();
    }
    
    public void onTransportConnect(SMTPTransport tester) throws MessagingException {
        String server = getServerName(tester.getURLName());
        log("TRANSPORT", "Connection to " + server);
        
        Exception ex = null;
        try {
            if (serverRules.containsKey(server)) ((TransportRule)serverRules.get(server)).onConnect(testStatus, server);
            if (genericRule != null) genericRule.onConnect(testStatus, server);
        } catch (MessagingException e) {
            ex = e;
        } catch (RuntimeException e) {
            ex = e;
        }

        testStatus.incTransportConnectionCount(1);
        testStatus.incTransportServerConnectionCount(server, 1);

        if (ex != null) {
            if (ex instanceof MessagingException) throw (MessagingException)ex;
            if (ex instanceof RuntimeException) throw (RuntimeException)ex;
        }
    }

    public void onTransportSendMessage(SMTPTransport tester, Message message, Address[] recipients) throws MessagingException, SendFailedException {
        String server = getServerName(tester.getURLName());
        ProcMail.Listing listing = new ProcMail.Listing();
        Vector rules = new Vector();
        for (int i = 0; i < recipients.length; i++) {
            ProcMail pmail = getProcMail(message, recipients[i]);
            log("TRANSPORT.SEND", "Preparing send " + pmail.getKey() + " to " + server);
            
            if (pmail == null) throw new IllegalStateException("Unknow mail for " + recipients[i]);
            listing.add(pmail);
            if (pmail.getTransportRule() != null && !rules.contains(pmail.getTransportRule())) rules.add(pmail.getTransportRule());
        }
        
        Exception ex = null;
        try {
            
            log("TRANSPORT.SEND", "Sending to " + server + ", applying " + rules.size() + " mail rules");
            for (int i = 0; i < rules.size(); i++) ((TransportRule) rules.get(i)).onSendMessage(testStatus, server, listing); 
            
            if (serverRules.containsKey(server)) ((TransportRule)serverRules.get(server)).onSendMessage(testStatus, server, listing);
            if (genericRule != null) genericRule.onSendMessage(testStatus, server, listing);
            
        } catch (SendFailedException e) {
            ex = e;
        } catch (MessagingException e) {
            ex = e;
        } catch (RuntimeException e) {
            ex = e;
        }
        
        if (ex != null) log("TRANSPORT.SEND", "Got Exception " + dumpException(ex));
        
        // If we got a sendFailed then the specific recipient could be OK
        Address[] validAddresses = ex instanceof SendFailedException ? ((SendFailedException)ex).getValidSentAddresses() : null;
        for (int i = 0; i < listing.size(); i++) {
            ProcMail pmail = listing.get(i);
            boolean valid = false;
            if (validAddresses != null) 
                for (int j = 0; !valid && j < validAddresses.length; j++)
                    valid = pmail.getRecipient().toString().equals(validAddresses[j].toString());
            
            pmail.sent(server, !valid ? ex : null);
//          System.out.println("--------------------\r\n");
//          System.out.println("0. " + server + " " + pmail + " " + pmail.getKey() + " " + pmail.getSendCount() + valid + "\r\n");
//          System.out.println("--------------------\r\n");

            testStatus.incTransportSendCount(1);
            testStatus.incTransportServerSendCount(server, 1);
            if (ex == null || valid) {
                testStatus.incTransportSendSuccessfullCount(1);
                testStatus.incTransportServerSendSuccessfullCount(server, 1);
            } else {
                testStatus.incTransportSendFailedCount(1);
                testStatus.incTransportServerSendFailedCount(server, 1);
            }
        }

        if (ex != null) {
            if (ex instanceof SendFailedException) throw (SendFailedException)ex;
            if (ex instanceof MessagingException) throw (MessagingException)ex;
            if (ex instanceof RuntimeException) throw (RuntimeException)ex;
        }

    }

    private String dumpException(Exception ex) {
        if (!(ex instanceof MessagingException)) return ex.toString();
        
        StringBuffer res = new StringBuffer();
        String exs = ex.getMessage();
        int p = exs.indexOf("nested exception is:");
        if (p >= 0) exs = exs.substring(0, p - 3);
        res.append(exs);
        //res.append(ex.toString());
        
        while (ex != null && (ex instanceof MessagingException)) {
            res.append(" > ");
            if (ex instanceof SMTPSendFailedException) res.append("smtpsfe" + ((SMTPSendFailedException)ex).getReturnCode()); else
            if (ex instanceof SMTPAddressFailedException) res.append("smtpafe" + ((SMTPAddressFailedException)ex).getReturnCode()); else
            if (ex instanceof SMTPAddressSucceededException) res.append("smtpase" + ((SMTPAddressSucceededException)ex).getReturnCode()); else
            if (ex instanceof SendFailedException) res.append("sfe"); else
            if (ex instanceof NullPointerException) res.append("null"); else 
            res.append("me");
            ex = ((MessagingException)ex).getNextException();
        }
        
        return res.toString();
    }

    public void onTransportClose(SMTPTransport tester) throws MessagingException {
        String server = getServerName(tester.getURLName());
        log("TRANSPORT", "Closing connection to " + server);
        
        Exception ex = null;
        try {
            if (serverRules.containsKey(server)) ((TransportRule)serverRules.get(server)).onClose(testStatus, server);
            if (genericRule != null) genericRule.onClose(testStatus, server);
        } catch (MessagingException e) {
            ex = e;
        } catch (RuntimeException e) {
            ex = e;
        }

        testStatus.incTransportCloseCount(1);
        testStatus.incTransportServerCloseCount(server, 1);

        if (ex != null) {
            if (ex instanceof MessagingException) throw (MessagingException)ex;
            if (ex instanceof RuntimeException) throw (RuntimeException)ex;
        }
    }

    public boolean OnTransportSupportsExtension(SMTPTransport tester, String arg0) {
        String server = getServerName(tester.getURLName());
        log("TRANSPORT", "Asking if " + server + " supports " + arg0);
        
        boolean value = false;
        if (serverRules.containsKey(server)) value = ((TransportRule)serverRules.get(server)).onSupportsExtension(testStatus, server, arg0, value);
        if (genericRule != null) value = genericRule.onSupportsExtension(testStatus, server, arg0, value);
        
        return value;
    }
    
    public void onMailetContextBounce(Mail mail, String message) {
        throw new UnsupportedOperationException("Not supported old bounce method");
    }

    public void onMailetContextSendMail(Mail mail) {
        Iterator recipients = mail.getRecipients().iterator();
        while (recipients.hasNext()) {
            MailAddress address = (MailAddress)recipients.next();
            ProcMail pmail = getProcMail(mail, address);
            if (pmail != null) onMailetContextSendMail(pmail, mail);
            else throw new IllegalStateException("No ProcMail on " + address);
        }
    }
    
    public void onMailetContextSendMail(ProcMail pmail, Mail mail) {
        pmail.bounced(mail);
        testStatus.incBouncedCount(1);
    }

    protected void onOutgoingStore(Mail mail) {
//  acceptedProcMailList.set(null);
//  
//  Iterator recipients = mail.getRecipients().iterator();
//  while (recipients.hasNext()) {
//      MailAddress address = (MailAddress)recipients.next();
//      ProcMail pmail = getProcMail(mail, address);
//      if (pmail == null) {
//          // (1) Called as the first step. The email is accepted and spooled.
//          pmail = new ProcMail(this, mail, address);
//          storeProcMail(pmail);
//          onOutgoingStoreStart(pmail, mail);
//      } else {
//          // (5b) And again as the last step when we have temporary errors
//          onOutgoingStoreEnd(pmail, mail);
//      }
//  }
}

protected void onOutgoingAccept(Mail mail) {
//  Vector l = new Vector();
//  acceptedProcMailList.set(l);
//  
//  // (2) Called as the second step. The email is being loaded for sending.
//  Iterator recipients = mail.getRecipients().iterator();
//  while (recipients.hasNext()) {
//      MailAddress address = (MailAddress)recipients.next();
//      ProcMail pmail = getProcMail(mail, address);
//      if (pmail != null) {
//          onOutgoingAccept(pmail, mail);
//          l.add(new Object[] {pmail, mail});
//      }
//      else throw new IllegalStateException("No ProcMail on " + address);
//  }
}

protected void onOutgoingRemove(Mail mail) {
//  // (5a) Called as the last method if the mail has a permanent result (perm fail or success) 
//  Iterator recipients = mail.getRecipients().iterator();
//  while (recipients.hasNext()) {
//      MailAddress address = (MailAddress)recipients.next();
//      ProcMail pmail = getProcMail(mail, address);
//      if (pmail != null) onOutgoingRemove(pmail, mail);
//      else throw new IllegalStateException("No ProcMail on " + address);
//  }
}

protected void onOutgoingRemove(String key) {
//  // (5a) Called as the last method if the mail has a permanent result (perm fail or success)
//  
//  // Look for the same key between the last accepted.
//  Vector l = (Vector) acceptedProcMailList.get();
//  if (l != null) 
//      for (int i = 0; i < l.size(); i++) {
//          ProcMail pmail = (ProcMail)((Object[])l.get(i))[0];
//          Mail mail = (Mail)((Object[])l.get(i))[1];
//          if (!pmail.getMailName().equals(key)) throw new IllegalStateException("Mail not recognized."); 
//          onOutgoingRemove(pmail, mail);
//      }
}

    /**
     * Runtime status of test.
     * <p>Contains test statistics.
     */
    public class TestStatus {
        private int transportConnectionCount = 0;
        private int transportSendCount = 0;
        private int transportSendSuccessfullCount = 0;
        private int transportSendFailedCount = 0;
        private int transportCloseCount = 0;
        private Map transportServerConnectionCount = new HashMap();
        private Map transportServerSendCount = new HashMap();
        private Map transportServerSendSuccessfullCount = new HashMap();
        private Map transportServerSendFailedCount = new HashMap();
        private Map transportServerCloseCount = new HashMap();
        private int bouncedCount = 0;
        
        public int getIntMapValue(Map map, String key) {
            return map.containsKey(key) ? ((Integer) map.get(key)).intValue() : 0;
        }
        public void setIntMapValue(Map map, String key, int v) {
            map.put(key, new Integer(v));
        }
        public void incIntMapValue(Map map, String key, int v) {
            setIntMapValue(map, key, getIntMapValue(map, key) + v);
        }
        
        public int getBouncedCount() {
            return bouncedCount;
        }
        public int incBouncedCount(int v) {
            return (bouncedCount += v);
        }
        public void setBouncedCount(int bouncedCount) {
            this.bouncedCount = bouncedCount;
        }
        public int getTransportCloseCount() {
            return transportCloseCount;
        }
        public int incTransportCloseCount(int v) {
            return (transportCloseCount += v);
        }
        public void setTransportCloseCount(int transportCloseCount) {
            this.transportCloseCount = transportCloseCount;
        }
        public int getTransportConnectionCount() {
            return transportConnectionCount;
        }
        public int incTransportConnectionCount(int v) {
            return (transportConnectionCount += v);
        }
        public void setTransportConnectionCount(int transportConnectionCount) {
            this.transportConnectionCount = transportConnectionCount;
        }
        public int getTransportSendCount() {
            return transportSendCount;
        }
        public int incTransportSendCount(int v) {
            return (transportSendCount += v);
        }
        public void setTransportSendCount(int transportSendCount) {
            this.transportSendCount = transportSendCount;
        }
        public int getTransportSendSuccessfullCount() {
            return transportSendSuccessfullCount;
        }
        public int incTransportSendSuccessfullCount(int v) {
            return (transportSendSuccessfullCount += v);
        }
        public void setTransportSendSuccessfullCount(int transportSendSuccessfullCount) {
            this.transportSendSuccessfullCount = transportSendSuccessfullCount;
        }
        public int getTransportSendFailedCount() {
            return transportSendFailedCount;
        }
        public int incTransportSendFailedCount(int v) {
            return (transportSendFailedCount += v);
        }
        public void setTransportSendFailedCount(int transportSendFailedCount) {
            this.transportSendFailedCount = transportSendFailedCount;
        }
        public int getTransportServerCloseCount(String server) {
            return getIntMapValue(transportServerCloseCount, server);
        }
        public void incTransportServerCloseCount(String server, int v) {
            incIntMapValue(transportServerCloseCount, server, v);
        }
        public int getTransportServerSendCount(String server) {
            return getIntMapValue(transportServerSendCount, server);
        }
        public void incTransportServerSendCount(String server, int v) {
            incIntMapValue(transportServerSendCount, server, v);
        }
        public int getTransportServerSendSuccessfullCount(String server) {
            return getIntMapValue(transportServerSendSuccessfullCount, server);
        }
        public void incTransportServerSendSuccessfullCount(String server, int v) {
            incIntMapValue(transportServerSendSuccessfullCount, server, v);
        }
        public int getTransportServerSendFailedCount(String server) {
            return getIntMapValue(transportServerSendFailedCount, server);
        }
        public void incTransportServerSendFailedCount(String server, int v) {
            incIntMapValue(transportServerSendFailedCount, server, v);
        }
        public int getTransportServerConnectionCount(String server) {
            return getIntMapValue(transportServerConnectionCount, server);
        }
        public void incTransportServerConnectionCount(String server, int v) {
            incIntMapValue(transportServerConnectionCount, server, v);
        }
        
    }
}
