package org.apache.james.transport;

import java.util.Collection;
import java.util.Iterator;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.avalon.framework.logger.Logger;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailetContext;

/**
 * Wrapper for a MailetContext that simply override the used logger.
 */
final class MailetContextWrapper implements MailetContext {
    
    /** the mailetContext */
    private MailetContext mailetContext;
    /** the logger */
    private Logger logger;

    /**
     * Create a mailetContext wrapper that use a different logger for the log
     * operations
     * 
     * @param mailetContext the mailet context to be wrapped
     * @param logger the logger to be used instead of the parent one. 
     */
    public MailetContextWrapper(MailetContext mailetContext, Logger logger) {
        this.mailetContext = mailetContext;
        this.logger = logger;
    }

    /**
     * @see org.apache.mailet.MailetContext#bounce(org.apache.mailet.Mail, java.lang.String)
     */
    public void bounce(Mail mail, String message) throws MessagingException {
        mailetContext.bounce(mail, message);
    }

    /**
     * @see org.apache.mailet.MailetContext#bounce(org.apache.mailet.Mail, java.lang.String, org.apache.mailet.MailAddress)
     */
    public void bounce(Mail mail, String message, MailAddress bouncer) throws MessagingException {
        mailetContext.bounce(mail, message, bouncer);
    }

    /**
     * @see org.apache.mailet.MailetContext#getAttribute(java.lang.String)
     */
    public Object getAttribute(String name) {
        return mailetContext.getAttribute(name);
    }

    /**
     * @see org.apache.mailet.MailetContext#getAttributeNames()
     */
    @SuppressWarnings("unchecked")
    public Iterator getAttributeNames() {
        return mailetContext.getAttributeNames();
    }

    /**
     * @see org.apache.mailet.MailetContext#getMailServers(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public Collection getMailServers(String host) {
        return mailetContext.getMailServers(host);
    }

    /**
     * @see org.apache.mailet.MailetContext#getMajorVersion()
     */
    public int getMajorVersion() {
        return mailetContext.getMajorVersion();
    }

    /**
     * @see org.apache.mailet.MailetContext#getMinorVersion()
     */
    public int getMinorVersion() {
        return mailetContext.getMinorVersion();
    }

    /**
     * @see org.apache.mailet.MailetContext#getPostmaster()
     */
    public MailAddress getPostmaster() {
        return mailetContext.getPostmaster();
    }

    /**
     * @see org.apache.mailet.MailetContext#getSMTPHostAddresses(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public Iterator getSMTPHostAddresses(String domainName) {
        return mailetContext.getSMTPHostAddresses(domainName);
    }

    /**
     * @see org.apache.mailet.MailetContext#getServerInfo()
     */
    public String getServerInfo() {
        return mailetContext.getServerInfo();
    }

    /**
     * @see org.apache.mailet.MailetContext#isLocalEmail(org.apache.mailet.MailAddress)
     */
    public boolean isLocalEmail(MailAddress mailAddress) {
        return mailetContext.isLocalEmail(mailAddress);
    }

    /**
     * @see org.apache.mailet.MailetContext#isLocalServer(java.lang.String)
     */
    public boolean isLocalServer(String serverName) {
        return mailetContext.isLocalServer(serverName);
    }

    /**
     * @see org.apache.mailet.MailetContext#isLocalUser(java.lang.String)
     */
    @SuppressWarnings("deprecation")
    public boolean isLocalUser(String userAccount) {
        return mailetContext.isLocalUser(userAccount);
    }

    /**
     * @see org.apache.mailet.MailetContext#log(java.lang.String)
     */
    public void log(String message) {
        logger.info(message);
    }

    /**
     * @see org.apache.mailet.MailetContext#log(java.lang.String, java.lang.Throwable)
     */
    public void log(String message, Throwable t) {
        logger.info(message, t);
    }

    /**
     * @see org.apache.mailet.MailetContext#removeAttribute(java.lang.String)
     */
    public void removeAttribute(String name) {
        mailetContext.removeAttribute(name);
    }

    /**
     * @see org.apache.mailet.MailetContext#sendMail(javax.mail.internet.MimeMessage)
     */
    public void sendMail(MimeMessage msg) throws MessagingException {
        mailetContext.sendMail(msg);
    }

    /**
     * @see org.apache.mailet.MailetContext#sendMail(org.apache.mailet.MailAddress, java.util.Collection, javax.mail.internet.MimeMessage)
     */
    @SuppressWarnings("unchecked")
    public void sendMail(MailAddress sender, Collection recipients, MimeMessage msg) throws MessagingException {
        mailetContext.sendMail(sender, recipients, msg);
    }

    /**
     * @see org.apache.mailet.MailetContext#sendMail(org.apache.mailet.MailAddress, java.util.Collection, javax.mail.internet.MimeMessage, java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public void sendMail(MailAddress sender, Collection recipients, MimeMessage msg, String state) throws MessagingException {
        mailetContext.sendMail(sender, recipients, msg, state);
    }

    /**
     * @see org.apache.mailet.MailetContext#sendMail(org.apache.mailet.Mail)
     */
    public void sendMail(Mail mail) throws MessagingException {
        mailetContext.sendMail(mail);
    }

    /**
     * @see org.apache.mailet.MailetContext#setAttribute(java.lang.String, java.lang.Object)
     */
    public void setAttribute(String name, Object object) {
        mailetContext.setAttribute(name, object);
    }

    /**
     * @see org.apache.mailet.MailetContext#storeMail(MailAddress, MailAddress, MimeMessage)
     */
    @SuppressWarnings("deprecation")
    public void storeMail(MailAddress sender, MailAddress recipient, MimeMessage msg) throws MessagingException {
        mailetContext.storeMail(sender, recipient, msg);
    }
}