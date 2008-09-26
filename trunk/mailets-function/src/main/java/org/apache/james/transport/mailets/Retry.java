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

import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.james.Constants;
import org.apache.james.services.SpoolRepository;
import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailetContext;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;

import javax.mail.MessagingException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * This Mailet retries delivery of a mail based on schedule specified in the
 * James configuration file by the 'delayTime' attribute. The format of the
 * 'delayTime' attribute is: [attempts*]delay[units]
 * <p>
 * For example, if the delay times were specified as follows:<br>
 * <delayTime> 4*15 minutes </delayTime> <delayTime> 3*1 hour </delayTime>
 * <delayTime> 3*4 hours </delayTime>
 * 
 * <maxRetries> 10 </maxRetries>
 * 
 * after the initial failure, the message will be retried by sending it to the
 * processor specified by the 'retryProcessor' attribute, as per the following
 * schedule: 1) 4 attempts will be made every 15 minutes. 2) 3 attempts will be
 * made every hour. 3) 3 attempts will be made every 4 hours.
 * 
 * If the message still fails, it will be sent for error processing to the
 * processor specified by the 'errorProcessor' attribute.
 * 
 * <p>
 * Following list summarizes all the attributes of this Mailet that can be
 * configured:
 * <ul>
 * <li><b>retryRepository</b> - Spool repository where mails are stored.
 * <li><b>delayTime</b> - Delay time (See description above).
 * <li><b>maxRetries</b> - Maximum no. of retry attempts.
 * <li><b>retryThreads</b> - No. of Threads used for retrying.
 * <li><b>retryProcessor</b> - Processor used for retrying.
 * <li><b>errorProcessor</b> - Error processor that will be used when all retry
 * attempts fail.
 * <li><b>isDebug</b> - Can be set to 'true' for debugging.
 * </ul>
 * 
 */
public class Retry extends GenericMailet implements Runnable {
    // Mail attribute that keeps track of # of retries.
    private static final String RETRY_COUNT = "RETRY_COUNT";

    // Mail attribute that keeps track of original error message.
    public static final String ORIGINAL_ERROR = "originalError";

    // Default Delay Time (Default is 6*60*60*1000 Milliseconds (6 hours)).
    private static final long DEFAULT_DELAY_TIME = 21600000;

    // Pattern to match [attempts*]delay[units].
    private static final String PATTERN_STRING = "\\s*([0-9]*\\s*[\\*])?\\s*([0-9]+)\\s*([a-z,A-Z]*)\\s*";

    // Compiled pattern of the above String.
    private static Pattern PATTERN = null;

    // Holds allowed units for delayTime together with factor to turn it into
    // the
    // equivalent time in milliseconds.
    private static final HashMap MULTIPLIERS = new HashMap(10);

    /*
     * Compiles pattern for processing delayTime entries. <p>Initializes
     * MULTIPLIERS with the supported unit quantifiers.
     */
    static {
        try {
            Perl5Compiler compiler = new Perl5Compiler();
            PATTERN = compiler.compile(PATTERN_STRING,
                    Perl5Compiler.READ_ONLY_MASK);
        } catch (MalformedPatternException mpe) {
            // This should never happen as the pattern string is hard coded.
            System.err.println("Malformed pattern: " + PATTERN_STRING);
            mpe.printStackTrace(System.err);
        }

        // Add allowed units and their respective multiplier.
        MULTIPLIERS.put("msec", new Integer(1));
        MULTIPLIERS.put("msecs", new Integer(1));
        MULTIPLIERS.put("sec", new Integer(1000));
        MULTIPLIERS.put("secs", new Integer(1000));
        MULTIPLIERS.put("minute", new Integer(1000 * 60));
        MULTIPLIERS.put("minutes", new Integer(1000 * 60));
        MULTIPLIERS.put("hour", new Integer(1000 * 60 * 60));
        MULTIPLIERS.put("hours", new Integer(1000 * 60 * 60));
        MULTIPLIERS.put("day", new Integer(1000 * 60 * 60 * 24));
        MULTIPLIERS.put("days", new Integer(1000 * 60 * 60 * 24));
    }

    /**
     * Used in the accept call to the spool. It will select the next mail ready
     * for processing according to the mails 'retrycount' and 'lastUpdated'
     * time.
     **/
    private class MultipleDelayFilter implements SpoolRepository.AcceptFilter {
        /**
         * Holds the time to wait for the youngest mail to get ready for
         * processing.
         **/
        long youngest = 0;

        /**
         * Uses the getNextDelay to determine if a mail is ready for processing
         * based on the delivered parameters errorMessage (which holds the
         * retrycount), lastUpdated and state.
         * 
         * @param key
         *            the name/key of the message
         * @param state
         *            the mails state
         * @param lastUpdated
         *            the mail was last written to the spool at this time.
         * @param errorMessage
         *            actually holds the retrycount as a string
         * @return {@code true} if message is ready for processing else {@code
         *         false}
         **/
        public boolean accept(String key, String state, long lastUpdated,
                String errorMessage) {
            int retries = Integer.parseInt(errorMessage);

            long delay = getNextDelay(retries);
            long timeToProcess = delay + lastUpdated;

            if (System.currentTimeMillis() > timeToProcess) {
                // We're ready to process this again
                return true;
            } else {
                // We're not ready to process this.
                if (youngest == 0 || youngest > timeToProcess) {
                    // Mark this as the next most likely possible mail to
                    // process
                    youngest = timeToProcess;
                }
                return false;
            }
        }

        /**
         * Returns the optimal time the SpoolRepository.accept(AcceptFilter)
         * method should wait before trying to find a mail ready for processing
         * again.
         **/
        public long getWaitTime() {
            if (youngest == 0) {
                return 0;
            } else {
                long duration = youngest - System.currentTimeMillis();
                youngest = 0;
                return duration <= 0 ? 1 : duration;
            }
        }
    }
    
    // Flag to define verbose logging messages.
    private boolean isDebug = false;

    // Repository used to store messages that will be retried.
    private SpoolRepository workRepository;

    // List of Delay Times. Controls frequency of retry attempts.
    private long[] delayTimes;

    // Maximum no. of retries (Defaults to 5).
    private int maxRetries = 5;

    // No. of threads used to process messages that should be retried.
    private int workersThreadCount = 1;

    // Collection that stores all worker threads.
    private Collection workersThreads = new Vector();

    // Processor that will be called for retrying. Defaults to "root" processor.
    private String retryProcessor = Mail.DEFAULT;

    // Processor that will be called if retrying fails after trying maximum no.
    // of
    // times. Defaults to "error" processor.
    private String errorProcessor = Mail.ERROR;

    // Flag used by 'run' method to end itself.
    private volatile boolean destroyed = false;

    // Matcher used in 'init' method to parse delayTimes specified in config
    // file.
    private Perl5Matcher delayTimeMatcher;

    // Filter used by 'accept' to check if message is ready for retrying.
    private MultipleDelayFilter delayFilter = new MultipleDelayFilter();

    // Path of the retry repository
    private String workRepositoryPath = null;

    /**
     * Initializes all arguments based on configuration values specified in the
     * James configuration file.
     * 
     * @throws MessagingException
     *             on failure to initialize attributes.
     */
    public void init() throws MessagingException {
        // Set isDebug flag.
        isDebug = (getInitParameter("debug") == null) ? false : new Boolean(getInitParameter("debug")).booleanValue();

        // Create list of Delay Times.
        ArrayList delayTimesList = new ArrayList();
        try {
            if (getInitParameter("delayTime") != null) {
                delayTimeMatcher = new Perl5Matcher();
                String delayTimesParm = getInitParameter("delayTime");

                // Split on commas
                StringTokenizer st = new StringTokenizer (delayTimesParm,",");
                while (st.hasMoreTokens()) {
                    String delayTime = st.nextToken();
                    delayTimesList.add (new Delay(delayTime));
                }
            } else {
                // Use default delayTime.
                delayTimesList.add(new Delay());
            }
        } catch (Exception e) {
            log("Invalid delayTime setting: " + getInitParameter("delayTime"));
        }

        try {
            // Get No. of Max Retries.
            if (getInitParameter("maxRetries") != null) {
                maxRetries = Integer.parseInt(getInitParameter("maxRetries"));
            }
    
            // Check consistency of 'maxRetries' with delayTimesList attempts.
            int totalAttempts = calcTotalAttempts(delayTimesList);
    
            // If inconsistency found, fix it.
            if (totalAttempts > maxRetries) {
                log("Total number of delayTime attempts exceeds maxRetries specified. "
                        + " Increasing maxRetries from "
                        + maxRetries
                        + " to "
                        + totalAttempts);
                maxRetries = totalAttempts;
            } else {
                int extra = maxRetries - totalAttempts;
                if (extra != 0) {
                    log("maxRetries is larger than total number of attempts specified.  "
                            + "Increasing last delayTime with "
                            + extra
                            + " attempts ");
    
                    // Add extra attempts to the last delayTime.
                    if (delayTimesList.size() != 0) {
                        // Get the last delayTime.
                        Delay delay = (Delay) delayTimesList.get(delayTimesList
                                .size() - 1);
    
                        // Increase no. of attempts.
                        delay.setAttempts(delay.getAttempts() + extra);
                        log("Delay of " + delay.getDelayTime()
                                + " msecs is now attempted: " + delay.getAttempts()
                                + " times");
                    } else {
                        throw new MessagingException(
                                "No delaytimes, cannot continue");
                    }
                }
            }
            delayTimes = expandDelays(delayTimesList);
        } catch (Exception e) {
            log("Invalid maxRetries setting: " + getInitParameter("maxRetries"));
        }

        ServiceManager compMgr = (ServiceManager) getMailetContext()
                .getAttribute(Constants.AVALON_COMPONENT_MANAGER);

        // Get the path for the 'Retry' repository. This is the place on the
        // file system where Mail objects will be saved during the 'retry'
        // processing. This can be changed to a repository on a database (e.g.
        // db://maildb/spool/retry).
        workRepositoryPath = getInitParameter("retryRepository");
        if (workRepositoryPath == null) {
            workRepositoryPath = "file://var/mail/retry/";
        }

        try {
            // Instantiate a MailRepository for mails that should be retried.
            Store mailstore = (Store) compMgr.lookup(Store.ROLE);

            DefaultConfiguration spoolConf = new DefaultConfiguration(
                    "repository", "generated:Retry");
            spoolConf.setAttribute("destinationURL", workRepositoryPath);
            spoolConf.setAttribute("type", "SPOOL");
            workRepository = (SpoolRepository) mailstore.select(spoolConf);
        } catch (ServiceException cnfe) {
            log("Failed to retrieve Store component:" + cnfe.getMessage());
            throw new MessagingException("Failed to retrieve Store component",
                    cnfe);
        }

        // Start Workers Threads.
        workersThreadCount = Integer.parseInt(getInitParameter("retryThreads"));
        for (int i = 0; i < workersThreadCount; i++) {
            String threadName = "Retry thread (" + i + ")";
            Thread t = new Thread(this, threadName);
            t.start();
            workersThreads.add(t);
        }

        // Get Retry Processor
        String processor = getInitParameter("retryProcessor");
        retryProcessor = (processor == null) ? Mail.DEFAULT : processor;

        // Get Error Processor
        processor = getInitParameter("errorProcessor");
        errorProcessor = (processor == null) ? Mail.ERROR : processor;
    }

    /**
     * Calculates Total no. of attempts for the specified delayList.
     * 
     * @param delayList
     *            list of 'Delay' objects
     * @return total no. of retry attempts
     */
    private int calcTotalAttempts (ArrayList delayList) {
        int sum = 0;
        Iterator i = delayList.iterator();
        while (i.hasNext()) {
            Delay delay = (Delay)i.next();
            sum += delay.getAttempts();
        }
        return sum;
    }

    /**
     * Expands an ArrayList containing Delay objects into an array holding the
     * only delaytime in the order.
     * <p>
     * 
     * For example, if the list has 2 Delay objects : First having attempts=2
     * and delaytime 4000 Second having attempts=1 and delaytime=300000
     * 
     * This will be expanded into this array:
     * <p>
     * 
     * long[0] = 4000
     * <p>
     * long[1] = 4000
     * <p>
     * long[2] = 300000
     * <p>
     * 
     * @param delayList
     *            the list to expand
     * @return the expanded list
     **/
    private long[] expandDelays(ArrayList delayList) {
        long[] delays = new long[calcTotalAttempts(delayList)];
        int idx = 0;
        for (int i = 0; i < delayList.size(); i++) {
            for (int j = 0; j < ((Delay) delayList.get(i)).getAttempts(); j++) {
                delays[idx++] = ((Delay) delayList.get(i)).getDelayTime();
            }
        }
        return delays;
    }

    /**
     * Returns, given a retry count, the next delay time to use.
     * 
     * @param retryCount
     *            the current retry count.
     * @return the next delay time to use
     **/
    private long getNextDelay(int retryCount) {
        if (retryCount > delayTimes.length) {
            return DEFAULT_DELAY_TIME;
        }
        return delayTimes[retryCount];
    }


    /**
     * This class is used to hold a delay time and its corresponding number of
     * retries.
     **/
    private class Delay {
        private int attempts = 1;

        private long delayTime = DEFAULT_DELAY_TIME;

        /**
         * This constructor expects Strings of the form
         * "[attempt\*]delaytime[unit]".
         * <p>
         * The optional attempt is the number of tries this delay should be used
         * (default = 1). The unit, if present, must be one of
         * (msec,sec,minute,hour,day). The default value of unit is 'msec'.
         * <p>
         * The constructor multiplies the delaytime by the relevant multiplier
         * for the unit, so the delayTime instance variable is always in msec.
         * 
         * @param initString
         *            the string to initialize this Delay object from
         **/
        public Delay(String initString) throws MessagingException {
            // Default unit value to 'msec'.
            String unit = "msec";

            if (delayTimeMatcher.matches(initString, PATTERN)) {
                MatchResult res = delayTimeMatcher.getMatch();

                // The capturing groups will now hold:
                // at 1: attempts * (if present)
                // at 2: delaytime
                // at 3: unit (if present)
                if (res.group(1) != null && !res.group(1).equals("")) {
                    // We have an attempt *
                    String attemptMatch = res.group(1);

                    // Strip the * and whitespace.
                    attemptMatch = attemptMatch.substring(0,
                            attemptMatch.length() - 1).trim();
                    attempts = Integer.parseInt(attemptMatch);
                }

                delayTime = Long.parseLong(res.group(2));

                if (!res.group(3).equals("")) {
                    // We have a value for 'unit'.
                    unit = res.group(3).toLowerCase(Locale.US);
                }
            } else {
                throw new MessagingException(initString + " does not match "
                        + PATTERN_STRING);
            }

            // Look for unit in the MULTIPLIERS Hashmap & calculate delayTime.
            if (MULTIPLIERS.get(unit) != null) {
                int multiplier = ((Integer) MULTIPLIERS.get(unit)).intValue();
                delayTime *= multiplier;
            } else {
                throw new MessagingException("Unknown unit: " + unit);
            }
        }

        /**
         * This constructor makes a default Delay object with attempts = 1 and
         * delayTime = DEFAULT_DELAY_TIME.
         **/
        public Delay() {
        }

        /**
         * @return the delayTime for this Delay
         **/
        public long getDelayTime() {
            return delayTime;
        }

        /**
         * @return the number attempts this Delay should be used.
         **/
        public int getAttempts() {
            return attempts;
        }

        /**
         * Set the number attempts this Delay should be used.
         **/
        public void setAttempts(int value) {
            attempts = value;
        }

        /**
         * Pretty prints this Delay
         **/
        public String toString() {
            String message = getAttempts() + "*" + getDelayTime() + "msecs";
            return message;
        }
    }

    public String getMailetInfo() {
        return "Retry Mailet";
    }

    /**
     * Checks if maximum retry count has been reached. If it is, then it
     * forwards the message to the error processor; otherwise writes it to the
     * retry repository.
     * 
     * @param mail
     *            the mail to be retried.
     * @throws MessagingException
     *             on failure to send it to the error processor.
     * 
     * @see org.apache.mailet.Mailet#service(org.apache.mailet.Mail)
     */
    public void service(Mail mail) throws MessagingException {
        if (isDebug) {
            log("Retrying mail " + mail.getName());
        }

        // Save the original error message.
        mail.setAttribute(ORIGINAL_ERROR, mail.getErrorMessage());

        // Get retry count and put it in the error message.
        // Note: 'errorMessage' is the only argument of 'accept' method in
        // SpoolRepository.AcceptFilter that can be used to pass the retry
        // count.
        String retryCount = (String) mail.getAttribute(RETRY_COUNT);
        if (retryCount == null) {
            retryCount = "0";
        }
        mail.setErrorMessage(retryCount);

        int retries = Integer.parseInt(retryCount);
        String message = "";

        // If maximum retries number hasn't reached, store message in retrying
        // repository.
        if (retries < maxRetries) {
            message = "Storing " + mail.getMessage().getMessageID()
                    + " to retry repository " + workRepositoryPath
                    + ", retry " + retries;
            log(message);

            mail.setAttribute(RETRY_COUNT, retryCount);
            workRepository.store(mail);
            mail.setState(Mail.GHOST);
        } else {
            // Forward message to 'errorProcessor'.
            message = "Sending " + mail.getMessage().getMessageID()
                    + " to error processor after retrying " + retries
                    + " times.";
            log(message);
            mail.setState(errorProcessor);
            MailetContext mc = getMailetContext();
            try {
                message = "Message failed after " + retries
                        + " retries with error " + "message: "
                        + mail.getAttribute(ORIGINAL_ERROR);
                mail.setErrorMessage(message);
                mc.sendMail(mail);
            } catch (MessagingException e) {
                // We shouldn't get an exception, because the mail was already
                // processed.
                log("Exception re-inserting failed mail: ", e);
                throw new MessagingException(
                        "Exception encountered while bouncing "
                                + "mail in Retry process.", e);
            }
        }
    }
    
    /**
     * Stops all the worker threads that are waiting for messages. This method is
     * called by the Mailet container before taking this Mailet out of service.
     */
    public synchronized void destroy() {
        // Mark flag so threads from this Mailet stop themselves
        destroyed = true;

        // Wake up all threads from waiting for an accept
        for (Iterator i = workersThreads.iterator(); i.hasNext(); ) {
            Thread t = (Thread)i.next();
            t.interrupt();
        }
        notifyAll();
    }

    /**
     * Handles checking the retrying spool for new mail and retrying them if
     * there are ready for retrying.
     */
    public void run() {
        try {
            while (!Thread.interrupted() && !destroyed) {
                try {
                    // Get the 'mail' object that is ready for retrying. If no
                    // message is
                    // ready, the 'accept' will block until message is ready.
                    // The amount
                    // of time to block is determined by the 'getWaitTime'
                    // method of the
                    // MultipleDelayFilter.
                    Mail mail = workRepository.accept(delayFilter);
                    String key = mail.getName();
                    try {
                        if (isDebug) {
                            String message = Thread.currentThread().getName()
                                    + " will process mail " + key;
                            log(message);
                        }

                        // Retry message
                        if (retry(mail)) {
                            // If retry attempt was successful, remove message.
                            // ContainerUtil.dispose(mail);
                            workRepository.remove(key);
                        } else {
                            // Something happened that will delay delivery.
                            // Store it back in the retry repository.
                            workRepository.store(mail);
                            ContainerUtil.dispose(mail);

                            // This is an update, so we have to unlock and
                            // notify or this mail is kept locked by this thread.
                            workRepository.unlock(key);
                            
                            // Note: We do not notify because we updated an
                            // already existing mail and we are now free to handle 
                            // more mails.
                            // Furthermore this mail should not be processed now
                            // because we have a retry time scheduling.
                        }
                        
                        // Clear the object handle to make sure it recycles
                        // this object.
                        mail = null;
                    } catch (Exception e) {
                        // Prevent unexpected exceptions from causing looping by
                        // removing message from outgoing.
                        // DO NOT CHANGE THIS to catch Error! For example, if
                        // there were an OutOfMemory condition caused because 
                        // something else in the server was abusing memory, we would 
                        // not want to start purging the retrying spool!
                        ContainerUtil.dispose(mail);
                        workRepository.remove(key);
                        throw e;
                    }
                } catch (Throwable e) {
                    if (!destroyed) {
                        log("Exception caught in Retry.run()", e);
                    }
                }
            }
        } finally {
            // Restore the thread state to non-interrupted.
            Thread.interrupted();
        }
    }

    /**
     * Retries delivery of a {@link Mail}.
     * 
     * @param mail
     *            mail to be retried.
     * @return {@code true} if message was resent successfully else {@code
     *         false}
     */
    private boolean retry(Mail mail) {
        if (isDebug) {
            log("Attempting to deliver " + mail.getName());
        }

        // Update retry count
        int retries = Integer.parseInt((String) mail.getAttribute(RETRY_COUNT));
        ++retries;
        mail.setErrorMessage(retries + "");
        mail.setAttribute(RETRY_COUNT, String.valueOf(retries));
        mail.setLastUpdated(new Date());

        // Call preprocessor
        preprocess(mail);

        // Send it to 'retry' processor
        mail.setState(retryProcessor);
        MailetContext mc = getMailetContext();
        try {
            String message = "Retrying message "
                    + mail.getMessage().getMessageID() + ".  Attempt #: "
                    + retries;
            log(message);
            mc.sendMail(mail);
        } catch (MessagingException e) {
            // We shouldn't get an exception, because the mail was already
            // processed
            log("Exception while retrying message. ", e);
            return false;
        }
        return true;
    }

    /**
     * Pre-processes the {@link Mail} object before resending.
     * <p>
     * This method can be used by subclasses to perform application specific
     * processing on the Mail object, such as, adding and/or removing
     * application specific Mail attributes etc. The default implementation
     * leaves the Mail object intact.
     * 
     * @param mail
     *            mail object that can be customized before resending.
     */
    protected void preprocess(Mail mail) {
    }
}
