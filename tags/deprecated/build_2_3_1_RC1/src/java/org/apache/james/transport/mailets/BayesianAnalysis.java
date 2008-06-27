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

package org.apache.james.transport.mailets;

import org.apache.avalon.cornerstone.services.datasources.DataSourceSelector;
import org.apache.avalon.excalibur.datasource.DataSourceComponent;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.james.Constants;
import org.apache.james.util.JDBCBayesianAnalyzer;
import org.apache.james.util.JDBCUtil;
import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.RFC2822Headers;
import org.apache.mailet.dates.RFC822DateFormat;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.sql.Connection;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * <P>Spam detection mailet using bayesian analysis techniques.</P>
 * 
 * <P>Sets an email message header indicating the
 * probability that an email message is SPAM.</P>
 * 
 * <P>Based upon the principals described in:
 *   <a href="http://www.paulgraham.com/spam.html">A Plan For Spam</a>
 *   by Paul Graham.
 * Extended to Paul Grahams' <a href="http://paulgraham.com/better.html">Better Bayesian Filtering</a>.</P>
 * 
 * <P>The analysis capabilities are based on token frequencies (the <I>Corpus</I>) 
 * learned through a training process (see {@link BayesianAnalysisFeeder})
 * and stored in a JDBC database.
 * After a training session, the Corpus must be rebuilt from the database in order to
 * acquire the new frequencies.
 * Every 10 minutes a special thread in this mailet will check if any
 * change was made to the database by the feeder, and rebuild the corpus if necessary.</p>
 * 
 * <p>A <CODE>org.apache.james.spam.probability</CODE> mail attribute will be created
 * containing the computed spam probability as a {@link java.lang.Double}.
 * The <CODE>headerName</CODE> message header string will be created containing such
 * probability in floating point representation.</p>
 * 
 * <P>Sample configuration:</P>
 * <PRE><CODE>
 * &lt;mailet match="All" class="BayesianAnalysis"&gt;
 *   &lt;repositoryPath&gt;db://maildb&lt;/repositoryPath&gt;
 *   &lt;!--
 *     Set this to the header name to add with the spam probability
 *     (default is "X-MessageIsSpamProbability").
 *   --&gt;
 *   &lt;headerName&gt;X-MessageIsSpamProbability&lt;/headerName&gt;
 *   &lt;!--
 *     Set this to true if you want to ignore messages coming from local senders
 *     (default is false).
 *     By local sender we mean a return-path with a local server part (server listed
 *     in &lt;servernames&gt; in config.xml).
 *   --&gt;
 *   &lt;ignoreLocalSender&gt;true&lt;/ignoreLocalSender&gt;
 *   &lt;!--
 *     Set this to the maximum message size (in bytes) that a message may have
 *     to be considered spam (default is 100000).
 *   --&gt;
 *   &lt;maxSize&gt;100000&lt;/maxSize&gt;
 * &lt;/mailet&gt;
 * </CODE></PRE>
 * 
 * <P>The probability of being spam is pre-pended to the subject if
 * it is &gt; 0.1 (10%).</P>
 * 
 * <P>The required tables are automatically created if not already there (see sqlResources.xml).
 * The token field in both the ham and spam tables is <B>case sensitive</B>.</P>
 * @see BayesianAnalysisFeeder
 * @see org.apache.james.util.BayesianAnalyzer
 * @see org.apache.james.util.JDBCBayesianAnalyzer
 * @version CVS $Revision: $ $Date: $
 * @since 2.3.0
 */

public class BayesianAnalysis
extends GenericMailet {
    /**
     * The JDBCUtil helper class
     */
    private final JDBCUtil theJDBCUtil = new JDBCUtil() {
        protected void delegatedLog(String logString) {
            log("BayesianAnalysis: " + logString);
        }
    };

    /**
     * The JDBCBayesianAnalyzer class that does all the work.
     */
    private JDBCBayesianAnalyzer analyzer = new JDBCBayesianAnalyzer() {
        protected void delegatedLog(String logString) {
            log("BayesianAnalysis: " + logString);
        }
    };
    
    private DataSourceComponent datasource;
    private String repositoryPath;
    
    private static final String MAIL_ATTRIBUTE_NAME = "org.apache.james.spam.probability";
    private static final String HEADER_NAME = "X-MessageIsSpamProbability";
    private static final long CORPUS_RELOAD_INTERVAL = 600000;
    private String headerName;
    private boolean ignoreLocalSender = false;
        
    /** The date format object used to generate RFC 822 compliant date headers. */
    private RFC822DateFormat rfc822DateFormat = new RFC822DateFormat();
    
    /**
     * Return a string describing this mailet.
     *
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "BayesianAnalysis Mailet";
    }
    
    /**
     * Holds value of property maxSize.
     */
    private int maxSize = 100000;

    /**
     * Holds value of property lastCorpusLoadTime.
     */
    private long lastCorpusLoadTime;
    
    /**
     * Getter for property maxSize.
     * @return Value of property maxSize.
     */
    public int getMaxSize() {

        return this.maxSize;
    }

    /**
     * Setter for property maxSize.
     * @param maxSize New value of property maxSize.
     */
    public void setMaxSize(int maxSize) {

        this.maxSize = maxSize;
    }

    /**
     * Getter for property lastCorpusLoadTime.
     * @return Value of property lastCorpusLoadTime.
     */
    public long getLastCorpusLoadTime() {
        
        return this.lastCorpusLoadTime;
    }
    
    /**
     * Sets lastCorpusLoadTime to System.currentTimeMillis().
     */
    private void touchLastCorpusLoadTime() {
        
        this.lastCorpusLoadTime = System.currentTimeMillis();
    }
    
    /**
     * Mailet initialization routine.
     * @throws MessagingException if a problem arises
     */
    public void init() throws MessagingException {
        repositoryPath = getInitParameter("repositoryPath");
        
        if (repositoryPath == null) {
            throw new MessagingException("repositoryPath is null");
        }
        
        headerName = getInitParameter("headerName",HEADER_NAME);
        
        ignoreLocalSender = Boolean.valueOf(getInitParameter("ignoreLocalSender")).booleanValue();
        
        if (ignoreLocalSender) {
            log("Will ignore messages coming from local senders");
        } else {
            log("Will analyze messages coming from local senders");
        }
        
        String maxSizeParam = getInitParameter("maxSize");
        if (maxSizeParam != null) {
            setMaxSize(Integer.parseInt(maxSizeParam));
        }
        log("maxSize: " + getMaxSize());
        
        initDb();
        
            CorpusLoader corpusLoader = new CorpusLoader(this);
            corpusLoader.setDaemon(true);
            corpusLoader.start();
            
    }
    
    private void initDb() throws MessagingException {
        
        try {
            ServiceManager serviceManager = (ServiceManager) getMailetContext().getAttribute(Constants.AVALON_COMPONENT_MANAGER);
            
            // Get the DataSourceSelector block
            DataSourceSelector datasources = (DataSourceSelector) serviceManager.lookup(DataSourceSelector.ROLE);
            
            // Get the data-source required.
            int stindex =   repositoryPath.indexOf("://") + 3;
            
            String datasourceName = repositoryPath.substring(stindex);
            
            datasource = (DataSourceComponent) datasources.select(datasourceName);
        } catch (Exception e) {
            throw new MessagingException("Can't get datasource", e);
        }
        
        try {
            analyzer.initSqlQueries(datasource.getConnection(), getMailetContext());
        } catch (Exception e) {
            throw new MessagingException("Exception initializing queries", e);
        }        
        
        try {
            loadData(datasource.getConnection());
        } catch (java.sql.SQLException se) {
            throw new MessagingException("SQLException loading data", se);
        }        
    }
    
    /**
     * Scans the mail and determines the spam probability.
     *
     * @param mail The Mail message to be scanned.
     * @throws MessagingException if a problem arises
     */
    public void service(Mail mail) throws MessagingException {
        
        try {
            MimeMessage message = mail.getMessage();
            
            if (ignoreLocalSender) {
                // ignore the message if the sender is local
                if (mail.getSender() != null
                        && getMailetContext().isLocalServer(mail.getSender().getHost())) {
                    return;
                }
            }
            
            String [] headerArray = message.getHeader(headerName);
            // ignore the message if already analyzed
            if (headerArray != null && headerArray.length > 0) {
                return;
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            double probability;
            
            if (message.getSize() < getMaxSize()) {
                message.writeTo(baos);
                probability = analyzer.computeSpamProbability(new BufferedReader(new StringReader(baos.toString())));
            } else {
                probability = 0.0;
            }
            
            mail.setAttribute(MAIL_ATTRIBUTE_NAME, new Double(probability));
            message.setHeader(headerName, Double.toString(probability));
            
            DecimalFormat probabilityForm = (DecimalFormat) DecimalFormat.getInstance();
            probabilityForm.applyPattern("##0.##%");
            String probabilityString = probabilityForm.format(probability);
            
            String senderString;
            if (mail.getSender() == null) {
                senderString = "null";
            } else {
                senderString = mail.getSender().toString();
            }
            if (probability > 0.1) {
                log(headerName
                        + ": "
                        + probabilityString
                        + "; From: "
                        + senderString
                        + "; Recipient(s): "
                        + getAddressesString(mail.getRecipients()));
                
                appendToSubject(message,
                        " [" + probabilityString
                        + (probability > 0.9 ? " SPAM" : " spam") + "]");
            }
            
            saveChanges(message);
            
        } catch (Exception e) {
            log("Exception: "
                    + e.getMessage(), e);
            throw new MessagingException("Exception thrown", e);
        }
    }
    
    private void loadData(Connection conn)
    throws java.sql.SQLException {
        
        try {
            // this is synchronized to avoid concurrent update of the corpus
            synchronized(JDBCBayesianAnalyzer.DATABASE_LOCK) {
                analyzer.tokenCountsClear();
                analyzer.loadHamNSpam(conn);
                analyzer.buildCorpus();
                analyzer.tokenCountsClear();
            }
            
            log("BayesianAnalysis Corpus loaded");
            
            touchLastCorpusLoadTime();
            
        } finally {
            if (conn != null) {
                theJDBCUtil.closeJDBCConnection(conn);
            }
        }
        
    }
    
    private String getAddressesString(Collection addresses) {
        if (addresses == null) {
            return "null";
        }
        
        Iterator iter = addresses.iterator();
        StringBuffer sb = new StringBuffer();
        sb.append('[');
        for (int i = 0; iter.hasNext(); i++) {
            sb.append(iter.next());
            if (i + 1 < addresses.size()) {
                sb.append(", ");
            }
        }
        sb.append(']');
        return sb.toString();
    }
    
    private void appendToSubject(MimeMessage message, String toAppend) {
        try {
            String subject = message.getSubject();
            
            if (subject == null) {
                message.setSubject(toAppend, "iso-8859-1");
            } else {
                message.setSubject(toAppend + " " + subject, "iso-8859-1");
            }
        } catch (MessagingException ex) {}
    }
    
    private void sendReplyFromPostmaster(Mail mail, String stringContent) throws MessagingException {
        try {
            MailAddress notifier = getMailetContext().getPostmaster();
            
            MailAddress senderMailAddress = mail.getSender();
            
            MimeMessage message = mail.getMessage();
            //Create the reply message
            MimeMessage reply = new MimeMessage(Session.getDefaultInstance(System.getProperties(), null));
            
            //Create the list of recipients in the Address[] format
            InternetAddress[] rcptAddr = new InternetAddress[1];
            rcptAddr[0] = senderMailAddress.toInternetAddress();
            reply.setRecipients(Message.RecipientType.TO, rcptAddr);
            
            //Set the sender...
            reply.setFrom(notifier.toInternetAddress());
            
            //Create the message body
            MimeMultipart multipart = new MimeMultipart();
            //Add message as the first mime body part
            MimeBodyPart part = new MimeBodyPart();
            part.setContent(stringContent, "text/plain");
            part.setHeader(RFC2822Headers.CONTENT_TYPE, "text/plain");
            multipart.addBodyPart(part);
            
            reply.setContent(multipart);
            reply.setHeader(RFC2822Headers.CONTENT_TYPE, multipart.getContentType());
            
            //Create the list of recipients in our MailAddress format
            Set recipients = new HashSet();
            recipients.add(senderMailAddress);
            
            //Set additional headers
            if (reply.getHeader(RFC2822Headers.DATE)==null){
                reply.setHeader(RFC2822Headers.DATE, rfc822DateFormat.format(new java.util.Date()));
            }
            String subject = message.getSubject();
            if (subject == null) {
                subject = "";
            }
            if (subject.indexOf("Re:") == 0){
                reply.setSubject(subject);
            } else {
                reply.setSubject("Re:" + subject);
            }
            reply.setHeader(RFC2822Headers.IN_REPLY_TO, message.getMessageID());
            
            //Send it off...
            getMailetContext().sendMail(notifier, recipients, reply);
        } catch (Exception e) {
            log("Exception found sending reply", e);
        }
    }
    
    /**
     * Saves changes resetting the original message id.
     */
    private void saveChanges(MimeMessage message) throws MessagingException {
        String messageId = message.getMessageID();
        message.saveChanges();
        if (messageId != null) {
            message.setHeader(RFC2822Headers.MESSAGE_ID, messageId);
        }
    }

    private static class CorpusLoader extends Thread {
        
        private BayesianAnalysis analysis;
        
        private CorpusLoader(BayesianAnalysis analysis) {
            super("BayesianAnalysis Corpus Loader");
            this.analysis = analysis;
        }
        
        /** Thread entry point.
         */
        public void run() {
        analysis.log("CorpusLoader thread started: will wake up every " + CORPUS_RELOAD_INTERVAL + " ms");
        
        try {
            Thread.sleep(CORPUS_RELOAD_INTERVAL);

            while (true) {
                if (analysis.getLastCorpusLoadTime() < JDBCBayesianAnalyzer.getLastDatabaseUpdateTime()) {
                    analysis.log("Reloading Corpus ...");
                    try {
                        analysis.loadData(analysis.datasource.getConnection());
                        analysis.log("Corpus reloaded");
                    } catch (java.sql.SQLException se) {
                        analysis.log("SQLException: ", se);
                    }
                    
                }
                
                if (Thread.interrupted()) {
                    break;
                }
                Thread.sleep(CORPUS_RELOAD_INTERVAL);
            }
        }
        catch (InterruptedException ex) {
            interrupt();
        }
        }
        
    }
    
}
