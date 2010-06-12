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

import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

/**
 * This class records the status of a delivery
 * This is used by the remote delivery tester to assert events
 * for a given delivery
 */
public class ProcMail {
    // Generated
    public final static int STATE_IDLE = 0;
    // outgoing spool (*)
    //public final static int STATE_SPOOLED = 1;
    // Sending (*)
    //public final static int STATE_SENDING = 3;
    // Sent
    public final static int STATE_SENT = 4;
    // Sent with error
    public final static int STATE_SENT_ERROR = 5;
    // Sent succesfully or permanent failure 
    //public final static int STATE_FINAL_REMOVED = 6;
    // Temporary error
    public final static int STATE_BOUNCED = 7;
    
    // Used wrong server
    public int ERRORFLAG_WRONGSERVER = 1;
    // Bounced after a successfull send.
    public int ERRORFLAG_WRONGBOUNCE = 2;
    
    private Tester owner;
    private String key;
    private int state;
    
    private MailAddress recipient;
    private MimeMessage mailMessage;
    private MailAddress mailSender;
    private String mailName;
    
    private MailStatus startMailStatus;
    private List<Date> sendDates;
    private List<String> sendServers;
    private List<Exception> sendExceptions;
    
    private List<MailStatus> bounceStatus;
    private List<Mail> bounceMails;

    private MailStatus finalMailStatus;
    
    private Date lastEventDate;
    
    private TransportRule transportRule;
    
    public int errorFlags;
    
    public static String getKey(Message message, Address address) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            message.writeTo(out);
            String msg = out.toString();
            msg = msg.substring(msg.indexOf("\r\n\r\n") + 4);
            String key = address.toString() + "_" + message.getSubject() + "_" + Math.abs(msg.hashCode());
            return key;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getKey(Mail mail, MailAddress recipient) {
        try {
            return getKey(mail.getMessage(), recipient.toInternetAddress());
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private ProcMail(Tester owner) {
        this.owner = owner;
        state = STATE_IDLE;
        sendDates = new Vector<Date>();
        bounceStatus = new Vector<MailStatus>();
        sendExceptions = new Vector<Exception>();
        bounceMails = new Vector<Mail>();
        sendServers = new Vector<String>();
        lastEventDate = new Date();
    }

    public ProcMail(Tester owner, Mail mail, MailAddress recipient) {
        this(owner);
        try {
            
            key = getKey(mail, recipient);
            this.recipient = recipient;
            this.mailMessage = mail.getMessage();
            this.mailSender = mail.getSender();
            this.mailName = mail.getName();
            startMailStatus = new MailStatus(mail);
            
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
    
//  public void spool(Mail mail) {
//      state = STATE_SPOOLED;
//      lastEventDate = new Date();
//  }

//  public void sending(Mail mail) {
//      state = STATE_SENDING;
//      lastEventDate = new Date();
//  }
    
    public void sent(String serverName, Exception e) {
        state = e == null ? STATE_SENT : STATE_SENT_ERROR;
        sendDates.add(new Date());
        sendServers.add(serverName);
        sendExceptions.add(e);
        lastEventDate = new Date();

        // check server
        String domain = owner.getDomainAssociated(serverName);
        if (!recipient.getDomain().equals(domain)) errorFlags = errorFlags | ERRORFLAG_WRONGSERVER; 
    }
    
//  public void removed(Mail mail) {
//      state = STATE_FINAL_REMOVED;
//      finalMailStatus = new MailStatus(mail);
//      lastEventDate = new Date();
//  }

    public void bounced(Mail mail) {
        // check this bounce is expected
//      if (state != STATE_SENT_ERROR) errorFlags = errorFlags | ERRORFLAG_WRONGBOUNCE;
        
        // state = STATE_BOUNCED;
        bounceStatus.add(new MailStatus(mail));
        bounceMails.add(mail);
        lastEventDate = new Date();
    }
    
    public int getSendCount() {
        return sendDates.size();
    }
    
    public int getBounceCount() {
        return bounceStatus.size();
    }
    
    /**
     * Duration (from creation to last event) in seconds.
     * @return
     */
    public int getLength() {
        return (int)((lastEventDate.getTime() - startMailStatus.getDate().getTime()) / 1000);
    }
    
    public int getErrorFlags() {
        return errorFlags;
    }

    public void setErrorFlags(int errorFlags) {
        this.errorFlags = errorFlags;
    }

    public List<Mail> getBounceMails() {
        return bounceMails;
    }

    public void setBounceMails(List<Mail> bounceMails) {
        this.bounceMails = bounceMails;
    }
    
    public Mail getBounceMail(int idx) {
        return (Mail) bounceMails.get(idx);
    }

    public List<MailStatus> getBounceStatus() {
        return bounceStatus;
    }

    public void setBounceStatus(List<MailStatus> bounceStatus) {
        this.bounceStatus = bounceStatus;
    }
    
    public MailStatus getBounceStatus(int idx) {
        return (MailStatus) bounceStatus.get(idx);
    }

    public MailStatus getFinalMailStatus() {
        return finalMailStatus;
    }

    public void setFinalMailStatus(MailStatus finalMailStatus) {
        this.finalMailStatus = finalMailStatus;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Date getLastEventDate() {
        return lastEventDate;
    }

    public void setLastEventDate(Date lastEventDate) {
        this.lastEventDate = lastEventDate;
    }

    public MimeMessage getMailMessage() {
        return mailMessage;
    }

    public void setMailMessage(MimeMessage mailMessage) {
        this.mailMessage = mailMessage;
    }

    public String getMailName() {
        return mailName;
    }

    public void setMailName(String mailName) {
        this.mailName = mailName;
    }

    public MailAddress getMailSender() {
        return mailSender;
    }

    public void setMailSender(MailAddress mailSender) {
        this.mailSender = mailSender;
    }

    public Tester getOwner() {
        return owner;
    }

    public void setOwner(Tester owner) {
        this.owner = owner;
    }

    public MailAddress getRecipient() {
        return recipient;
    }

    public void setRecipient(MailAddress recipient) {
        this.recipient = recipient;
    }

    public List<Exception> getSendExceptions() {
        return sendExceptions;
    }

    public void setSendExceptions(List<Exception> sendExceptions) {
        this.sendExceptions = sendExceptions;
    }
    
    public Exception getSendException(int idx) {
        return (Exception) sendExceptions.get(idx);
    }

    public List<String> getSendServers() {
        return sendServers;
    }

    public void setSendServers(List<String> sendServers) {
        this.sendServers = sendServers;
    }
    
    public String getSendServer(int idx) {
        return (String) sendServers.get(idx);
    }

    public List<Date> getSendDates() {
        return sendDates;
    }

    public void setSendDates(List<Date> sendDate) {
        this.sendDates = sendDate;
    }

    public Date getSendDate(int idx) {
        return (Date) sendDates.get(idx);
    }

    public MailStatus getStartMailStatus() {
        return startMailStatus;
    }

    public void setStartMailStatus(MailStatus startMailStatus) {
        this.startMailStatus = startMailStatus;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public TransportRule getTransportRule() {
        return transportRule;
    }

    public void setTransportRule(TransportRule transportRule) {
        this.transportRule = transportRule;
    }
    
    public class MailStatus {
        public Date date;
        public String errorMessage;
        public Date lastUpdated;
        public String state;
        public MailStatus() {
            date = new Date();
        }
        public MailStatus(Mail mail) {
            date = new Date();
            errorMessage = mail.getErrorMessage();
            lastUpdated = mail.getLastUpdated();
            state = mail.getState();
        }
        public Date getDate() {
            return date;
        }
        public void setDate(Date date) {
            this.date = date;
        }
        public String getErrorMessage() {
            return errorMessage;
        }
        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
        public Date getLastUpdated() {
            return lastUpdated;
        }
        public void setLastUpdated(Date lastUpdated) {
            this.lastUpdated = lastUpdated;
        }
        public String getState() {
            return state;
        }
        public void setState(String state) {
            this.state = state;
        }
    }
    
    public static class Listing {
        HashMap<String,ProcMail> map = new HashMap<String,ProcMail>();
        List<ProcMail> list = new Vector<ProcMail>();
        
        public Listing() {
        }
        
        public void add(ProcMail mail) {
            map.put(mail.getRecipient().toString(), mail);
            list.add(mail);
        }
        
        public ProcMail get(String email) {
            return (ProcMail) map.get(email);
        }

        public ProcMail get(int idx) {
            return (ProcMail) list.get(idx);
        }
        
        public int size() {
            return list.size();
        }
    }
}
