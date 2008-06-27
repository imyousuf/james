/*
 * Copyright (c) 1998 Stefano Mazzocchi, Pierpaolo Fumagalli.  All rights reserved.
 */
 
package org.apache.servlet.mail;

import java.io.*;
import java.util.*;

/**
 * This class defines an internet mail address.
 * 
 * @author Stefano Mazzocchi <stefano@apache.org>
 * @author Pierpaolo Fumagalli <p.fumagalli@fumagalli.org>
 * @version pre-draft 1.0 (submitted for review)
 */
public class MailAddress implements Serializable {

    /** The mail address description */ 
    private String description;
    
    /** The mailbox name */
    private String mailbox;
    
    /** The host name*/
    private String host;
    
    /**
     * Creates this object from a string with the format:
     * <pre>description &lt;mailbox@host&gt;</pre>
     */
    public MailAddress (String header) {
        String description = null;
        String address = null;
        
        try {
            StringTokenizer t = new StringTokenizer(header, "<>");
            if (t.countTokens() == 1) {
                address = t.nextToken();
            } else {
                description = t.nextToken();
                address = t.nextToken();
            }
        } finally {
            this.init(description, address);
        }
    }

    /**
     * Creates this object from a description and the mail address with the format:
     * <pre>user@host</pre>
     */
    public MailAddress (String description, String address) {
        this.init(description, address);
    }

    /**
     * Creates this object from description, mailbox and host.
     */
    public MailAddress (String description, String mailbox, String host) {
        this.init(description, mailbox, host);
    }

    /**
     * Creates this object from a description and the mail address with the format:
     * <pre>user@host</pre>
     */
    private void init (String description, String address) {
        String mailbox = null;
        String host = null;
        
        try {
            StringTokenizer t = new StringTokenizer(address, "@");
            mailbox = t.nextToken();
            host = t.nextToken();
        } finally {
            this.init(description, mailbox, host);
        }            
    }

    /**
     * Creates this object from description, mailbox and host.
     */
    private void init (String description, String mailbox, String host) {
        this.description = (description != null) ? description.trim() : ""; 
        this.mailbox = (mailbox != null) ? mailbox.trim() : ""; 
        this.host = (host != null) ? host.trim() : ""; 
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    public void setMailbox(String mailbox) {
        this.mailbox = mailbox;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public String getDescription() {
        return this.description;
    }
    
    public String getMailbox() {
        return this.mailbox;
    }

    public String getHost() {
        return this.host;
    }

    /**
     * Returns the address with the format:
     * <pre>mailbox@host</pre>
     */
    public String getAddress() {
        return this.mailbox + "@" + this.host;
    }

    /**
     * Returns the fully qualified address with the format:
     * <pre>description &lt;mailbox@host&gt;</pre>
     */
    public String toString() {
        return this.description + " <" + this.mailbox + "@" + this.host + ">";
    }
}