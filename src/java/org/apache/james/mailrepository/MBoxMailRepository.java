/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.james.mailrepository;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.core.MailImpl;
import org.apache.james.services.MailRepository;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Matcher;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.InternetAddress;
import java.util.*;
import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.lang.reflect.Array;

/**
 * Implementation of a MailRepository using UNIX mbox files.
 *
 * <p>Requires a configuration element in the .conf.xml file of the form:
 *  <br>&lt;repository destinationURL="mbox://&lt;directory&gt;"
 *  <br>            type="MAIL"
 *  <br>&lt;/directory&gt; is where the individual mbox files are read from/written to
 * <br>Type can ONLY be MAIL (SPOOL is NOT supported)
 *
 * <p>Requires a logger called MailRepository.
 *
 * <p> Implementation notes:
 * <p>
 * This class keeps an internal store of the mbox file
 * When the internal mbox file is updated (added/deleted)
 * then the file will be re-read from disk and then written back.
 * This is a bit inefficent but means that the file on disk
 * should be correct.
 * <p>
 * The mbox store is mainly meant to be used as a one-way street.
 * Storing new emails is very fast (append to file) whereas reading them (via POP3) is
 * slower (read from disk and parse).
 * Therefore this implementation is best suited to people who wish to use the mbox format
 * for taking data out of James and into something else (IMAP server or mail list displayer)
 *
 * @version CVS $Revision: 1.1.2.1 $
 */


public class MBoxMailRepository
        extends AbstractLogEnabled
            implements MailRepository, Component, Contextualizable, Composable, Configurable, Initializable {


    static final SimpleDateFormat dy = new SimpleDateFormat("EE MMM d HH:mm:ss yyyy", Locale.US);
    static final String LOCKEXT = ".lock";
    static final String WORKEXT = ".work";
    static final int LOCKSLEEPDELAY = 2000; // 2 second back off in the event of a problem with the lock file
    static final int MAXSLEEPTIMES = 100; //
    static final int USEREMOVETHREAD = 50;
    static final int REMOVALTHREADSLEEPTIME = 5000;

    /**
     * Whether 'deep debugging' is turned on.
     */
    private static final boolean DEEP_DEBUG = false;

    /**
     * The Avalon context used by the instance
     */
    protected Context context;

    /**
     * The internal list of the emails
     * The key is an adapted MD5 checksum of the mail
     */
    private Hashtable mList = null;
    /**
     * The filename to read & write the mbox from/to
     */
    private String mboxFile;

    /**
     * The message that was retrieved from the repository
     */
    private MimeMessage foundMessage = null;


    private ArrayList removalList=new ArrayList();;
    private Thread removalThread = null;

    /**
     * A callback used when a message is read from the mbox file
     */
    public interface MessageAction {
        public boolean messageAction(String messageSeparator, String bobyText, long messageStart);
    }


    /**
     * Convert a MimeMessage into raw text
     * @param mc The mime message to convert
     * @return A string representation of the mime message
     * @throws IOException
     * @throws MessagingException
     */
    private String getRawMessage(MimeMessage mc) throws IOException, MessagingException {

        ByteArrayOutputStream rawMessage = new ByteArrayOutputStream();
        mc.writeTo(rawMessage);
        return rawMessage.toString();
    }

    /**
     * Parse a text block as an email and convert it into a mime message
     * @param emailBody The headers and body of an email. This will be parsed into a mime message and stored
     */
    protected MimeMessage convertTextToMimeMessage(String emailBody) {
        //this.emailBody = emailBody;
        MimeMessage mimeMessage = null;
        // Parse the mime message as we have the full message now (in string format)
        ByteArrayInputStream mb = new ByteArrayInputStream(emailBody.getBytes());
        Properties props = System.getProperties();
        Session session = Session.getDefaultInstance(props);
        String toAddr = null;
        try {
            mimeMessage = new MimeMessage(session, mb);


        } catch (MessagingException e) {
            getLogger().error("Unable to parse mime message!", e);
        }

        if (mimeMessage == null && getLogger().isDebugEnabled()) {
            StringBuffer logBuffer =
                    new StringBuffer(128)
                    .append(this.getClass().getName())
                    .append(" Mime message is null");
            getLogger().debug(logBuffer.toString());
        }

        /*
        try {
            // Attempt to read the TO field and see if it errors
            toAddr = mimeMessage.getRecipients(javax.mail.Message.RecipientType.TO).toString();
        } catch (Exception e) {
            // It has errored, so time for plan B
            // use the from field I suppose
            try {
                mimeMessage.setRecipients(javax.mail.Message.RecipientType.TO, mimeMessage.getFrom());
                if (getLogger().isDebugEnabled()) {
                    StringBuffer logBuffer =
                            new StringBuffer(128)
                            .append(this.getClass().getName())
                            .append(" Patching To: field for message ")
                            .append(" with  From: field");
                    getLogger().debug(logBuffer.toString());
                }
            } catch (MessagingException e1) {
                getLogger().error("Unable to set to: field to from: field", e);
            }
        } */
        return mimeMessage;
    }

    /**
     * Generate a hex representation of an MD5 checksum on the emailbody
     * @param emailBody
     * @return A hex representation of the text
     * @throws NoSuchAlgorithmException
     */
    protected String generateKeyValue(String emailBody) throws NoSuchAlgorithmException {
        // MD5 the email body for a reilable (ha ha) key
        byte[] digArray = MessageDigest.getInstance("MD5").digest(emailBody.getBytes());
        StringBuffer digest = new StringBuffer();
        for (int i = 0; i < digArray.length; i++) {
            digest.append(Integer.toString(digArray[i], Character.MAX_RADIX).toUpperCase());
        }
        return digest.toString();
    }

    /**
     * Parse the mbox file.
     * @param ins The random access file to load. Note that the file may or may not start at offset 0 in the file
     * @param messAct The action to take when a message is found
     */
    private void parseMboxFile(RandomAccessFile ins, MBoxMailRepository.MessageAction messAct) {
        if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
            StringBuffer logBuffer =
                    new StringBuffer(128)
                    .append(this.getClass().getName())
                    .append("Start parsing ")
                    .append(mboxFile);

            getLogger().debug(logBuffer.toString());
        }
        try {

            Perl5Compiler sepMatchCompiler = new Perl5Compiler();
            Pattern sepMatchPattern = sepMatchCompiler.compile("^From (.*) (.*):(.*):(.*)$");
            Perl5Matcher sepMatch = new Perl5Matcher();

            int c;
            boolean inMessage = false;
            StringBuffer line = new StringBuffer();
            StringBuffer messageBuffer = new StringBuffer();
            String previousMessageSeparator = null;
            boolean foundSep = false;

            long prevMessageStart = 0;
            while ((c = ins.read()) != -1) {
                if (c == 10) {
                    foundSep = sepMatch.contains(line.toString(), sepMatchPattern);
                    if (foundSep && inMessage) {
                        if (messAct.messageAction(previousMessageSeparator, messageBuffer.toString(), prevMessageStart)) {
                            // I've got what I want so just exit
                            return;
                        }
                        previousMessageSeparator = line.toString();
                        prevMessageStart = ins.getFilePointer() - line.length();
                        messageBuffer = new StringBuffer();
                        inMessage = true;
                    }
                    // Only done at the start (first header)
                    if (foundSep && inMessage == false) {
                        previousMessageSeparator = line.toString();
                        inMessage = true;
                    }
                    if (!foundSep) {
                        messageBuffer.append(line).append((char) c);
                    }
                    line = new StringBuffer(); // Reset buffer
                } else {
                    line.append((char) c);
                }
            }
            if (messageBuffer.length() != 0) {
                // write last message
                messAct.messageAction(previousMessageSeparator, messageBuffer.toString(), prevMessageStart);
            }
            if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
                StringBuffer logBuffer =
                        new StringBuffer(128)
                        .append(this.getClass().getName())
                        .append("Start parsing ")
                        .append(mboxFile);

                getLogger().debug(logBuffer.toString());
            }
        } catch (IOException ioEx) {
            getLogger().error("Unable to write file (General I/O problem) " + mboxFile, ioEx);
        } catch (MalformedPatternException e) {
            getLogger().error("Bad regex passed " + mboxFile, e);
        }
    }

    /**
     * Find a given message
     * This method will first use selectMessage(key) to see if the key/offset combination allows us to skip
     * parts of the file and only load the message we are interested in
     *
     * @param key The key of the message to find
     */
    protected void findMessage(String key) {

        final String keyValue = key;

        // See if we can get the message by using the cache posiition first
        selectMessage(key);
        RandomAccessFile ins = null;
        try {
            ins = new RandomAccessFile(mboxFile, "r");
            this.parseMboxFile(ins, new MBoxMailRepository.MessageAction() {
                public boolean messageAction(String messageSeparator, String bodyText, long messageStart) {
                    try {
                        if (keyValue.equalsIgnoreCase(generateKeyValue(bodyText))) {
                            // Nasty hack
                            foundMessage = convertTextToMimeMessage(bodyText);
                            return false;
                        }
                    } catch (NoSuchAlgorithmException e) {
                        getLogger().error("MD5 not supported! ",e);
                    }
                    return true;
                }
            });
            ins.close();
        } catch (FileNotFoundException e) {
            getLogger().error("Unable to save(open) file (File not found) " + mboxFile, e);
        } catch (IOException e) {
            getLogger().error("Unable to write file (General I/O problem) " + mboxFile, e);
        }
    }

    /**
     * Quickly find a message by using the stored message offsets
     * @param key  The key of the message to find
     */
    private void selectMessage(final String key) {
        // Can we find the key first
        if (mList == null || !mList.containsKey(key)) {
            // Not initiailised so no point looking
            if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
                StringBuffer logBuffer =
                        new StringBuffer(128)
                        .append(this.getClass().getName())
                        .append("mList - key not found ")
                        .append(mboxFile);

                getLogger().debug(logBuffer.toString());
            }
            return;
        }
        foundMessage = null;
        long messageStart = ((Long) mList.get(key)).longValue();
        if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
            StringBuffer logBuffer =
                    new StringBuffer(128)
                    .append(this.getClass().getName())
                    .append("Load message starting at offset ")
                    .append(messageStart)
                    .append(" from file ")
                    .append(mboxFile);

            getLogger().debug(logBuffer.toString());
        }
        // Now try and find the position in the file
        RandomAccessFile ins = null;
        try {
            ins = new RandomAccessFile(mboxFile, "r");
            if (messageStart != 0) {
                ins.seek(messageStart - 1);
            }
            this.parseMboxFile(ins, new MBoxMailRepository.MessageAction() {
                public boolean messageAction(String messageSeparator, String bodyText, long messageStart) {
                    try {
                        if (key.equals(generateKeyValue(bodyText))) {
                            foundMessage = convertTextToMimeMessage(bodyText);
                            return true;
                        }
                    } catch (NoSuchAlgorithmException e) {
                        getLogger().error("MD5 not supported! ",e);
                    }
                    return false;
                }
            });
            ins.close();

        } catch (FileNotFoundException e) {
            getLogger().error("Unable to save(open) file (File not found) " + mboxFile, e);
        } catch (IOException e) {
            getLogger().error("Unable to write file (General I/O problem) " + mboxFile, e);
        }
        if (foundMessage == null) {
            if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
                StringBuffer logBuffer =
                        new StringBuffer(128)
                        .append(this.getClass().getName())
                        .append("select - message not found ")
                        .append(mboxFile);

                getLogger().debug(logBuffer.toString());
            }
        }
    }

    /**
     * Load the message keys and file pointer offsets from disk
     */
    protected void loadKeys() {
        if (mList!=null) {
            return;
        }
        RandomAccessFile ins = null;
        try {
            ins = new RandomAccessFile(mboxFile, "r");
            this.mList = new Hashtable();
            this.parseMboxFile(ins, new MBoxMailRepository.MessageAction() {
                public boolean messageAction(String messageSeparator, String bodyText, long messageStart) {
                    try {
                        mList.put(generateKeyValue(bodyText), new Long(messageStart));
                    } catch (NoSuchAlgorithmException e) {
                        getLogger().error("MD5 not supported! ",e);
                    }
                    return false; // Not done yet
                }
            });
            ins.close();
            //System.out.println("Done Load keys!");
        } catch (FileNotFoundException e) {
            getLogger().error("Unable to save(open) file (File not found) " + mboxFile, e);
        } catch (IOException e) {
            getLogger().error("Unable to write file (General I/O problem) " + mboxFile, e);
        }
    }


    /**
     * @see org.apache.avalon.framework.context.Contextualizable#contextualize(org.apache.avalon.framework.context.Context)
     */
    public void contextualize(final Context context)
            throws ContextException {
        this.context = context;
    }

    /**
     * Store the given email in the current mbox file
     * @param mc The mail to store
     */
    public void store(MailImpl mc) {

        if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
            StringBuffer logBuffer =
                    new StringBuffer(128)
                    .append(this.getClass().getName())
                    .append("Will store message to file ")
                    .append(mboxFile);

            getLogger().debug(logBuffer.toString());
        }
        this.mList = null;
        // Now make up the from header
        String fromHeader = null;
        String message = null;
        try {
            message = getRawMessage(mc.getMessage());
            fromHeader = "From " + ((InternetAddress)mc.getMessage().getFrom()[0]).getAddress() + " " + dy.format(Calendar.getInstance().getTime());
        } catch (IOException e) {
            getLogger().error("Unable to parse mime message for " + mboxFile, e);
        } catch (MessagingException e) {
            getLogger().error("Unable to parse mime message for " + mboxFile, e);
        }
        // And save only the new stuff to disk
        RandomAccessFile saveFile = null;
        try {
            saveFile = new RandomAccessFile(mboxFile, "rw");
            saveFile.seek(saveFile.length()); // Move to the end
            saveFile.writeBytes((fromHeader + "\n"));
            saveFile.writeBytes((message + "\n"));
            saveFile.close();

        } catch (FileNotFoundException e) {
            getLogger().error("Unable to save(open) file (File not found) " + mboxFile, e);
        } catch (IOException e) {
            getLogger().error("Unable to write file (General I/O problem) " + mboxFile, e);
        }
    }

    /**
     * Return the list of the current messages' keys
     * @return A list of the keys of the emails currently loaded
     */
    public Iterator list() {
        loadKeys();
        if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
            StringBuffer logBuffer =
                    new StringBuffer(128)
                    .append(this.getClass().getName())
                    .append(mList.size())
                    .append(" keys to be iterated over.");

            getLogger().debug(logBuffer.toString());
        }
        return mList.keySet().iterator();
    }

    /**
     * Get a message from the backing store (disk)
     * @param key
     * @return The mail found from the key. Returns null if the key is not found
     */
    public MailImpl retrieve(String key) {

        loadKeys();
        MailImpl res = null;
        try {
            findMessage(key);
            if (foundMessage == null) {
                getLogger().error("found message is null!");
                return null;
            }
            res = new MailImpl(foundMessage);
            res.setName(key);
            if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
                StringBuffer logBuffer =
                        new StringBuffer(128)
                        .append(this.getClass().getName())
                        .append("Retrieving entry for key ")
                        .append(key);

                getLogger().debug(logBuffer.toString());
            }
        } catch (MessagingException e) {
            getLogger().error("Unable to parse mime message for " + mboxFile + "\n" + e.getMessage(), e);
        } finally {
            // Reset so we don't find the same message again
            foundMessage = null;
        }
        return res;

    }

    /**
     * Remove an existing message
     * @param mail
     */
    public void remove(MailImpl mail) {
        // Convert the message into a key
        Vector delVec = new Vector();
        delVec.addElement(mail);
        remove(delVec);
    }

    /**
     * Attempt to get a lock on the mbox by creating
     * the file mboxname.lock
     * @throws Exception
     */
    protected void lockMBox() throws Exception {
        // Create the lock file (if possible)
        String lockFileName = mboxFile + LOCKEXT;
        int sleepCount = 0;
        File mBoxLock = new File(lockFileName);
        if (!mBoxLock.createNewFile()) {
            // This is not good, somebody got the lock before me
            // So wait for a file
            while (!mBoxLock.createNewFile() && sleepCount < MAXSLEEPTIMES) {
                try {
                    if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
                        StringBuffer logBuffer =
                                new StringBuffer(128)
                                .append(this.getClass().getName())
                                .append("Waiting for lock on file ")
                                .append(mboxFile);

                        getLogger().debug(logBuffer.toString());
                    }

                    Thread.sleep(LOCKSLEEPDELAY);
                    sleepCount++;
                } catch (InterruptedException e) {
                    getLogger().error("File lock wait for " + mboxFile + " interrupted!",e);

                }
            }
            if (sleepCount >= MAXSLEEPTIMES) {
                throw new Exception("Unable to get lock on file " + mboxFile);
            }
        }
    }

    /**
     * Unlock a previously locked mbox file
     */
    protected void unlockMBox() {
        // Just delete the MBOX file
        String lockFileName = mboxFile + LOCKEXT;
        File mBoxLock = new File(lockFileName);
        if (!mBoxLock.delete()) {
            StringBuffer logBuffer =
                    new StringBuffer(128)
                    .append(this.getClass().getName())
                    .append("Failed to delete lock file ")
                    .append(lockFileName);
            getLogger().error(logBuffer.toString());
        }
    }



    /**
     * Remove a list of messages from disk
     * The collection is simply a list of mails to delete
     * @param mails
     */
    public void remove(final Collection mails)
    {
        if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
            StringBuffer logBuffer =
                    new StringBuffer(128)
                    .append(this.getClass().getName())
                    .append("Removing entry for key ")
                    .append(mails);

            getLogger().debug(logBuffer.toString());
        }
        // The plan is as follows:
        // Attempt to locate the message in the file
        // by reading through the
        // once we've done that then seek to the file
        try {
            RandomAccessFile ins = new RandomAccessFile(mboxFile, "r"); // The source
            final RandomAccessFile outputFile = new RandomAccessFile(mboxFile + WORKEXT, "rw"); // The destination
            parseMboxFile(ins, new MessageAction() {
                public boolean messageAction(String messageSeparator, String bodyText, long messageStart) {
                    // Write out the messages as we go, until we reach the key we want
                    try {
                        String currentKey=generateKeyValue(bodyText);
                        boolean foundKey=false;
                        Iterator mailList = mails.iterator();
                        String key;
                        while (mailList.hasNext()) {
                            // Attempt to find the current key in the array
                            key = ((MailImpl)mailList.next()).getName();
                            if (key.equals(currentKey)) {
                                // Don't write the message to disk
                                foundKey = true;
                                break;
                            }
                        }
                        if (foundKey == false)
                        {
                            // We didn't find the key in the array so we will keep it
                            outputFile.writeBytes(messageSeparator + "\n");
                            outputFile.writeBytes(bodyText);

                        }
                    } catch (NoSuchAlgorithmException e) {
                        getLogger().error("MD5 not supported! ",e);
                    } catch (IOException e) {
                        getLogger().error("Unable to write file (General I/O problem) " + mboxFile, e);
                    }
                    return false;
                }
            });
            ins.close();
            outputFile.close();
            // Delete the old mbox file
            File mbox = new File(mboxFile);
            mbox.delete();
            // And rename the lock file to be the new mbox
            mbox = new File(mboxFile + WORKEXT);
            if (!mbox.renameTo(new File(mboxFile)))
            {
                 System.out.println("Failed to rename file!");
            }

            // Now delete the keys in mails from the main hash
            Iterator mailList = mails.iterator();
            String key;
            while (mailList.hasNext()) {
                // Attempt to find the current key in the array
                key = ((MailImpl)mailList.next()).getName();
                mList.remove(key);
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
            getLogger().error("Unable to save(open) file (File not found) " + mboxFile, e);
        } catch (IOException e) {
            e.printStackTrace();
            getLogger().error("Unable to write file (General I/O problem) " + mboxFile, e);
        }

    }

    /**
     * Remove a mail from the mbox file
     * @param key The key of the mail to delete
     */
    public void remove(String key) {
        loadKeys();
        try {
            lockMBox();
        } catch (Exception e) {
            getLogger().error("Lock failed!",e);
            return; // No lock, so exit
        }
        ArrayList keys = new ArrayList();
        keys.add(key);

        this.remove(keys);
        unlockMBox();
    }

    /**
     * Not implemented
     * @param key
     * @return
     */
    public boolean lock(String key) {
        return false;
    }

    /**
     * Not implemented
     * @param key
     * @return
     */
    public boolean unlock(String key) {
        return false;
    }


    public void compose(ComponentManager componentManager) throws ComponentException {
    }

    /**
     * Configure the component
     * @param conf
     * @throws ConfigurationException
     */
    public void configure(Configuration conf) throws ConfigurationException {
        String destination;
        this.mList = null;
        destination = conf.getAttribute("destinationURL");
        if (destination.charAt(destination.length() - 1) == '/') {
            // Remove the trailing / as well as the protocol marker
            mboxFile = destination.substring("mbox://".length(), destination.lastIndexOf("/"));
        } else {
            mboxFile = destination.substring("mbox://".length());
        }

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("MBoxMailRepository.destinationURL: " + destination);
        }

        String checkType = conf.getAttribute("type");
        if (!(checkType.equals("MAIL") || checkType.equals("SPOOL"))) {
            String exceptionString = "Attempt to configure MboxMailRepository as " +
                    checkType;
            if (getLogger().isWarnEnabled()) {
                getLogger().warn(exceptionString);
            }
            throw new ConfigurationException(exceptionString);
        }
    }


    /**
     * Initialise the component
     * @throws Exception
     */
    public void initialize() throws Exception {
    }


    public static void main(String[] args) {
        // Test invocation
        MBoxMailRepository mbx = new MBoxMailRepository();
        mbx.mboxFile = "C:\\java\\test\\1998-05.txt";
        Iterator mList = mbx.list();
        while (mList.hasNext()) {
            String key = (String) mList.next();
            //System.out.println("key=" + key);
            /*MailImpl mi =  mbx.retrieve(key);
            try
            {
                System.out.println("Subject : " +  (mi.getMessage()).getSubject());
            }
            catch (MessagingException e)
            {
                e.printStackTrace();  //To change body of catch statement use Options | File Templates.
            } */

        }


/*        MailImpl mi = mbx.retrieve("ffffffb4ffffffe2f59fffffff291dffffffde4366243ffffff971d1f24");
        try {
            System.out.println("Subject : " + (mi.getMessage()).getSubject());
        } catch (MessagingException e) {
            e.printStackTrace();  //To change body of catch statement use Options | File Templates.
        }
        mbx.remove("ffffffb4ffffffe2f59fffffff291dffffffde4366243ffffff971d1f24");*/
    }
}
