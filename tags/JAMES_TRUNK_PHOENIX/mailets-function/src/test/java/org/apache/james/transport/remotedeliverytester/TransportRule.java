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

import org.apache.james.transport.remotedeliverytester.ProcMail.Listing;
import org.apache.james.transport.remotedeliverytester.Tester.TestStatus;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

/**
 * The TransportRule is a rule for the tester.
 * It define the behaviour of a given server.
 */
public interface TransportRule {
    void onConnect(TestStatus status, String server) throws MessagingException;
    void onSendMessage(TestStatus status, String server, ProcMail.Listing pmails) throws MessagingException, SendFailedException;
    void onClose(TestStatus status, String server) throws MessagingException;
    boolean onSupportsExtension(TestStatus status, String server, String ext, boolean value);


    public abstract class Default implements TransportRule {
        public void onConnect(TestStatus status, String server) throws MessagingException {
        }
        public void onSendMessage(TestStatus status, String server, ProcMail.Listing pmails) throws MessagingException, SendFailedException {
        }
        public void onClose(TestStatus status, String server) throws MessagingException {
        }
        public boolean onSupportsExtension(TestStatus status, String server, String ext, boolean value) {
            return value;
        }
    }
    
    /**
     * Server rule string format:
     * id-rule.[*-userrule].[match*-userrule].string
     * 
     * - id-rule are for "connect"-time exceptions
     * - *-userrule are for "send"-time exceptions involving every mail (not depending on the recipients address)
     * - match*-userrule are for "send"-time exceptions involving specific recipients (starting with match - ** to match every address)
     * 
     * Available rules:
     * - me[v]: MessagingException
     * - sfe[v]: SendFailedException
     * - smtpafeXXX[v]: SMTPAddressFailedException with XXX returnCode
     * - smtpaseXXX[v]: SMTPAddressSuccededException with XXX returnCode
     * - smtpsfeXXX[v]: SMTPSendFailedException with XXX returnCode
     * - null: NullPointerException
     * - io: IOException (nested in a MessagingException)
     * - rpt: repeat the previous rule pattern 
     * - (*) if a "mail" rule (one involving addresses) ends with "v" resulting in a "valid" address (the failure is about some other error: valid unsent)
     */
    public class NameExpression extends Default {
        
        private static final String STAR_MATCHER = "ANY";
        
        // Se false ai recipient validi non vengono effettivamente fatti invii 
        private boolean propSendValid = true;
        
        public NameExpression(boolean sendValid) {
            propSendValid = sendValid;
        }
        
        /**
         * Take a string like "name-action[-count]-[action2[-count2]...]"
         * and convert it in a list "name, action, action, ..., action2, action2, ..., ..."
         * @param data
         * @return
         */
        private String[] getRules(String data) {
            String[] pieces = data.split("-");
            Vector actions = new Vector();
            actions.add(pieces[0]);
            for (int i = 1; i < pieces.length; i += 2) {
                int cnt = i + 1 < pieces.length ? Integer.parseInt(pieces[i + 1]) : 1;
                for (int j = 0; j < cnt; j++) actions.add(pieces[i]);
            }
            return (String[]) actions.toArray(new String[0]);
        }
        
        private MessagingException getException(String[] actions, int idx, ProcMail pmail, String prefix) {
            MessagingException result = null;
            if (idx + 1 >= actions.length && actions[actions.length - 1].equals("rpt")) idx = 1 + (idx % actions.length - 2);
            String action = idx + 1 < actions.length ? (String) actions[idx + 1] : actions[actions.length - 1];
            if (action.equals("me") || action.equals("ex")) {
                String flag = action.substring(2).trim();
                boolean valid = flag != null && flag.equalsIgnoreCase("v"); 
                result = new MessagingException("000" + (valid ? "V " : "I ") + prefix + "#" + idx);
            } else if (action.equals("sfe")) {
                String flag = action.substring(3).trim();
                boolean valid = flag != null && flag.equalsIgnoreCase("v"); 
                result = new SendFailedException("000" + (valid ? "V " : "I ") + prefix + "#" + idx);
            } else if (action.startsWith("smtpafe")) {
                String retcodes = action.substring(7, 10).trim();
                int retcode = 500;
                if (retcodes != null && retcodes.length() == 3) retcode = Integer.parseInt(retcodes);
                String flag = action.substring(10).trim();
                boolean valid = flag != null && flag.equalsIgnoreCase("v"); 
                result = new SMTPAddressFailedException(pmail.getRecipient().toInternetAddress(), "CMD", retcode, retcode + (valid ? "V " : "I ") + pmail.getRecipient().toString()+ ":"+ prefix + "#" + idx);
            } else if (action.startsWith("smtpase")) {
                String retcodes = action.substring(7).trim();
                int retcode = 200;
                if (retcodes != null && retcodes.length() == 3) retcode = Integer.parseInt(retcodes);
                String flag = action.substring(10).trim();
                boolean valid = flag != null && flag.equalsIgnoreCase("v"); 
                result = new SMTPAddressSucceededException(pmail.getRecipient().toInternetAddress(), "CMD", retcode, retcode + (valid ? "V " : "I ") + pmail.getRecipient().toString()+ ":"+ prefix + "#" + idx);
            } else if (action.startsWith("smtpsfe")) {
                String retcodes = action.substring(7).trim();
                int retcode = 500;
                if (retcodes != null && retcodes.length() == 3) retcode = Integer.parseInt(retcodes);
                String flag = action.substring(10).trim();
                boolean valid = flag != null && flag.equalsIgnoreCase("v"); 
                result = new SMTPSendFailedException("CMD", retcode, retcode + (valid ? "V " : "I ") + prefix + "#" + idx, null, null, null, null);
                
            } else if (action.startsWith("null")) {
                result = new NullMessagingException();
                
            } else if (action.startsWith("io")) {
                result = new IOMessagingException();
            }
            
            return result;
        }
        
        private boolean isValid(Exception ex) {
            return ex != null && ex.getMessage() != null && ex.getMessage().length() > 4 && ex.getMessage().charAt(3) == 'V';
        }
        
        /**
         * Calculate the priority of an exception (in order to choose the right exception on multiple/conflicts)
         * @param ex
         * @return
         */
        private int getPrio(Exception ex) {
            // Addresses exceptions are low priority
            // Take care of exception hierarchy: parents are the last in this ifs list
            if (ex instanceof NullMessagingException) return 7000;
            if (ex instanceof IOMessagingException) return 6000;
            if (ex instanceof SMTPSendFailedException) return 5000 + ((SMTPAddressFailedException)ex).getReturnCode();
            if (ex instanceof SMTPAddressFailedException) return 2000 + ((SMTPAddressFailedException)ex).getReturnCode();
            if (ex instanceof SMTPAddressSucceededException) return 1000 + ((SMTPAddressSucceededException)ex).getReturnCode();
            if (ex instanceof SendFailedException) return 4000;
            if (ex instanceof MessagingException) return 3000;
            return -1;
        }
        
        /**
         * Returns the exception with the greatest priority.
         * @param newex
         * @param oldex
         * @return
         */
        private MessagingException getMajor(MessagingException newex, MessagingException oldex) {
            if (newex == null) return oldex;
            if (oldex == null) return newex;
            return getPrio(newex) >= getPrio(oldex) ? newex : oldex;
        }

        private void addException(HashMap map, MessagingException ex, Address recipient) {
            if (map.get(recipient) != null) {
                MessagingException oldex = (MessagingException) map.get(recipient);
                ex = getMajor(ex, oldex);
            }
            map.put(recipient, ex);
        }
        
        /**
         * Clone the exception, sets the "invalid" addresses as invalidAddress (in the clone)
         * If the exception does not support "invalidaddress" declaration rethrow the original exception.
         * @return
         */
        private MessagingException generateClone(Exception ex, Address[] validUnsent, Address[] invalid, MessagingException next) {
            // Attenzione: le eccezioni devono essere in ordine di gerarchia (le + generiche - MessagingException - per ultime)  
            if (ex instanceof SMTPSendFailedException) {
                return new SMTPSendFailedException(((SMTPSendFailedException)ex).getCommand(), ((SMTPSendFailedException)ex).getReturnCode(), ex.getMessage(), next, new Address[0], validUnsent, invalid);
                
            } else if (ex instanceof SendFailedException) {
                return new SendFailedException(ex.getMessage(), next, new Address[0], validUnsent, invalid);
                
            } else if ((ex instanceof MessagingException) || (ex instanceof SMTPAddressSucceededException) || (ex instanceof SMTPAddressFailedException)) {
                //if (next != null) ex.setNextException(next);
                return (MessagingException) ex;
            } 
            return null;
        }
        
        private MessagingException generateParent(Exception majorChildEx, Address[] validSent, Address[] validUnsent, Address[] invalid, MessagingException next) {
            // NOTE: exceptions must be checked considering their hierarchy
            if (majorChildEx instanceof SMTPAddressSucceededException) {
                // If the major exception is an address exception we generate a SendFailedException (non SMTP). If we generate an SMTP exception the return code must be 2xx (e.g: 250)
                // ERR return new SMTPSendFailedException(((SMTPAddressSucceededException)majorChildEx).getCommand(), ((SMTPAddressSucceededException)majorChildEx).getReturnCode(), majorChildEx.getMessage(), next, validSent, validUnsent, invalid);
                //return new SMTPSendFailedException(((SMTPAddressSucceededException)majorChildEx).getCommand(), 250, "250 "+majorChildEx.getMessage(), next, validSent, validUnsent, invalid);
                return new SendFailedException(majorChildEx.getMessage(), next, validSent, validUnsent, invalid);
                
            } else if (majorChildEx instanceof SMTPAddressFailedException) {
                // ERR: return new SMTPSendFailedException(((SMTPAddressFailedException)majorChildEx).getCommand(), ((SMTPAddressFailedException)majorChildEx).getReturnCode(), majorChildEx.getMessage(), next, validSent, validUnsent, invalid);
                //return new SMTPSendFailedException(((SMTPAddressFailedException)majorChildEx).getCommand(), 250, "250 "+majorChildEx.getMessage(), next, validSent, validUnsent, invalid);
                return new SendFailedException(majorChildEx.getMessage(), next, validSent, validUnsent, invalid);
                
            } else if (majorChildEx instanceof SMTPSendFailedException) {
                return new SMTPSendFailedException(((SMTPSendFailedException)majorChildEx).getCommand(), ((SMTPSendFailedException)majorChildEx).getReturnCode(), majorChildEx.getMessage(), next, validSent, validUnsent, invalid);
                
            } else if (majorChildEx instanceof SendFailedException) {
                return new SendFailedException(majorChildEx.getMessage(), next, validSent, validUnsent, invalid);
                
            } else if (majorChildEx instanceof MessagingException) {
                //if (next != null) majorChildEx.setNextException(next);
                return (MessagingException) majorChildEx;
            } 
            
            return null;
        }
        
        private MessagingException getRootException(HashMap map) {
            // x recipient: sfe (generate an sfe in root), smtpase (generate an smtafe), smtpafe (generate an smtafe), 
            // x root: me, sfe, smtpafe
            
            MessagingException current = null, prev = null, major = null;
            Vector invalid = new Vector();
            Vector validSent = new Vector();
            Vector validUnsent = new Vector();
            Iterator i = map.keySet().iterator();
            while (i.hasNext()) {
                Address address = (Address) i.next();
                if (address != null) {
                    current = (MessagingException) map.get(address);
                    if (current != null) {
                        if (prev != null) current.setNextException(prev);
                        if (major != null) major = getMajor(major, current);
                        else major = current;
                        prev = current;
                        
                        if (!(current instanceof SMTPAddressSucceededException)) {
                            if (isValid(current)) validUnsent.add(address);
                            else invalid.add(address);
                        } 
                        else if (propSendValid) validSent.add(address); else validUnsent.add(address);
                    } 
                    else if (propSendValid) validSent.add(address); else validUnsent.add(address);
                }
            }
            
            // on generic exception (not a per-recipient exception) clone it with all "unsents" (validSent -> validUnsent)
            MessagingException ex1 = (MessagingException) map.get(null);
            if (ex1 != null) {
                validUnsent.addAll(validSent);
                ex1 = generateClone(ex1, (Address[]) validUnsent.toArray(new Address[0]), (Address[]) invalid.toArray(new Address[0]), prev);
            }
            
            // Create a container exception for the above exceptions
            MessagingException ex2 = major != null ? (MessagingException) generateParent(major, (Address[]) validSent.toArray(new Address[0]), (Address[]) validUnsent.toArray(new Address[0]), (Address[]) invalid.toArray(new Address[0]), prev) : null;
            
            return getMajor(ex1, ex2);
            
        }
        
        public void onConnect(TestStatus status, String server) throws MessagingException {
            String serverData = server;
            if (serverData.indexOf("://") >= 0) serverData = serverData.substring(serverData.indexOf("://") + 3);
            if (serverData.indexOf(":") >= 0) serverData = serverData.substring(0, serverData.indexOf(":"));
            String[] pieces = serverData.split("\\.");
            String[] actions = getRules(pieces[0]);
            int idx = status.getTransportServerConnectionCount(server);
            
            MessagingException ex = getException(actions, idx, null, server);
            if (ex != null) throwME(ex);
        }

        public void onSendMessage(TestStatus status, String server, Listing pmails) throws MessagingException, SendFailedException {
            String serverData = server;
            if (serverData.indexOf("://") >= 0) serverData = serverData.substring(serverData.indexOf("://") + 3);
            if (serverData.indexOf(":") >= 0) serverData = serverData.substring(0, serverData.indexOf(":"));
            String[] serverPieces = serverData.split("\\.");
            
            HashMap exceptionMap = new HashMap();
            int idx = status.getTransportServerSendCount(server);
            
            for (int i = 1; i < serverPieces.length - 1; i++) {
                String[] actions = getRules(serverPieces[i]);
                System.out.println("Actions1!");
                for (int k = 0; k < actions.length; k++) System.out.println(actions[k]);
                if (actions[0].endsWith(STAR_MATCHER) && actions[0].length() > 1) {
                    String match = actions[0].substring(0, actions[0].length() - STAR_MATCHER.length());
                    System.out.println("Match!");
                    System.out.println(match);
                    for (int j = 0; j < pmails.size(); j++) if (pmails.get(j).getRecipient().toString().startsWith(match) || match.equals(STAR_MATCHER)) {
                        MessagingException ex = getException(actions, idx, pmails.get(j), server);
                        addException(exceptionMap, ex, pmails.get(j).getRecipient().toInternetAddress());
                    } else addException(exceptionMap, null, pmails.get(j).getRecipient().toInternetAddress());
                }
            }
            
            for (int i = 1; i < serverPieces.length - 1; i++) {
                String[] actions = getRules(serverPieces[i]);
                System.out.println("Actions2!");
                for (int k = 0; k < actions.length; k++) System.out.println(actions[k]);
                if (actions[0].equals(STAR_MATCHER)) {
                    MessagingException ex = getException(actions, idx, null, server);
                    addException(exceptionMap, ex, null);
                }
            }
            
            MessagingException ex = getRootException(exceptionMap);
            if (ex != null) throwME(ex);
        }

        public void throwME(MessagingException ex) throws MessagingException {
            if (ex instanceof NullMessagingException) throw new NullPointerException();
            if (ex instanceof IOMessagingException) {
                MessagingException me = new MessagingException();
                me.setNextException(new IOException());
                throw me;
            }
            throw ex;
        }
    }
    
}
