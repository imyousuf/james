package org.apache.james.samples.mailets;

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 

import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Flags.Flag;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.NewsAddress;
import javax.mail.internet.MimeMessage.RecipientType;

import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.Mailet;
import org.apache.mailet.MailetConfig;
import org.apache.mailet.MailetContext;

/**
 * Mailet just prints out the details of a message. 
 * Sometimes Useful for debugging.
 */
public class InstrumentationMailet implements Mailet {

    private MailetConfig config;
    
    public void destroy() {

    }

    public String getMailetInfo() {
        return "Example mailet";
    }

    public MailetConfig getMailetConfig() {
        return config;
    }

    public void init(MailetConfig config) throws MessagingException {
        this.config = config;
    }

    public void service(Mail mail) throws MessagingException {
        MailetContext context = config.getMailetContext();
        context.log("######## MAIL STARTS");
        context.log("");
        
        MimeMessage message = mail.getMessage();
        
        context.log("Mail named: " + mail.getName());
        
        for (Iterator it=mail.getAttributeNames(); it.hasNext();) {
            String attributeName = (String) it.next();
            context.log("Attribute " + attributeName);
        }
        context.log("Message size: " + mail.getMessageSize());
        context.log("Last updated: " + mail.getLastUpdated());
        context.log("Remote Address: " + mail.getRemoteAddr());
        context.log("Remote Host: " + mail.getRemoteHost());
        context.log("State: " + mail.getState());
        context.log("Sender host: " + mail.getSender().getHost());
        context.log("Sender user: " + mail.getSender().getUser());
        Collection recipients = mail.getRecipients();
        for (Iterator it = recipients.iterator(); it.hasNext();)
        {
            MailAddress address = (MailAddress) it.next();
            context.log("Recipient: " + address.getUser() + "@" + address.getHost());
        }
        
        context.log("Subject: " + message.getSubject());
        context.log("MessageID: " + message.getMessageID());
        context.log("Received: " + message.getReceivedDate());
        context.log("Sent: " + message.getSentDate());
        
        Enumeration allHeadersLines = message.getAllHeaderLines();
        while(allHeadersLines.hasMoreElements()) {
            String header = (String) allHeadersLines.nextElement();
            context.log("Header Line:= " + header);
        }
        
        
        Enumeration allHeadersEnumeration = message.getAllHeaders();
        while(allHeadersEnumeration.hasMoreElements()) {
            Header header = (Header) allHeadersEnumeration.nextElement();
            context.log("Header: " + header.getName() + "=" + header.getValue());
        }
        
        Address[] to = message.getRecipients(RecipientType.TO);
        printAddresses(to, "TO: ");
        Address[] cc = message.getRecipients(RecipientType.CC);
        printAddresses(cc, "CC: ");     
        Address[] bcc = message.getRecipients(RecipientType.BCC);
        printAddresses(bcc, "BCC: ");   

        
        Flags flags = message.getFlags();
        Flag[] systemFlags = flags.getSystemFlags();
        for (int i=0;i<systemFlags.length;i++) {
            context.log("System Flag:" + systemFlags[i]);
        }
        String[] userFlags = flags.getUserFlags();
        for (int i=0;i<userFlags.length;i++) {
            context.log("User flag: " + userFlags[i]);
        }
        
        String mimeType = message.getContentType();
        context.log("Mime type: " + mimeType);
        if (mimeType == "text/plain") {
            try {
                Object content = message.getContent();
                context.log("Content: " + content);
            } catch (IOException e) {
                e.printStackTrace();
            }
            
        }
        
        context.log("");
        context.log("######## MAIL ENDS");
    }
    
    private final void printAddresses(Address[] addresses, String prefix) {
        MailetContext context = config.getMailetContext();
        for (int i=0;i<addresses.length;i++) {
            if (addresses[i] instanceof InternetAddress) {
                InternetAddress address = (InternetAddress) addresses[i];
                context.log(prefix + address.getPersonal() + "@" + address.getAddress());
            } else if (addresses[i] instanceof NewsAddress) {
                NewsAddress address = (NewsAddress) addresses[i];
                context.log(prefix + address.getNewsgroup() + "@" + address.getHost());
            }
        }
    }
}
