/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.imapserver;

import org.apache.james.imapserver.AccessControlException;

import java.util.*;
//import org.apache.james.core.EnhancedMimeMessage;

/**
 * Provides methods useful for IMAP command objects.
 *
 * References: rfc 2060, rfc 2193, rfc 2221
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @author <a href="mailto:sascha@kulawik.de">Sascha Kulawik</a>
 * @version 0.2 on 29 Jul 2002
 */

public abstract class BaseCommand
    extends BaseConnectionHandler {

    //mainly to switch on stack traces and catch responses;
    private static final boolean DEEP_DEBUG = true;

    /**
     * Turns a protocol-compliant string representing a message sequence
     * number set into a List of integers. Use of the wildcard * (star) relies
     * on contiguous property of msns.
     *
     * @param rawSet the IMAP protocol compliant string to be decoded
     * @param exists the number of messages in this mailbox
     * @return a List of Integers, one per message in set
     */
    protected List decodeSet( String rawSet, int exists ) throws IllegalArgumentException {
        if (rawSet == null) {
            getLogger().debug("Null argument in decodeSet");
            throw new IllegalArgumentException("Null argument");
        } else if (rawSet.equals("")) {
            getLogger().debug("Empty argument in decodeSet");
            throw new IllegalArgumentException("Empty string argument");
        }
        getLogger().debug(" decodeSet called for: " + rawSet);
        System.out.println(" decodeSet called for: " + rawSet);
        List response = new ArrayList();

        int checkComma = rawSet.indexOf(",");
        if (checkComma == -1) {
            // No comma present
            int checkColon = rawSet.indexOf(":");
            if (checkColon == -1) {
                // No colon present (single integer)
                Integer seqNum;
                if ( rawSet.equals( "*" ) ) {
                    seqNum = new Integer( -1 );
                }
                else {
                    seqNum = new Integer(rawSet.trim());
                    if (seqNum.intValue() < 1) {
                        throw new IllegalArgumentException("Not a positive integer1");
                    }
                }
                response.add(seqNum);
            }
            else {
                // Simple sequence

                // Add the first number in the range.
                Integer firstNum = new Integer(rawSet.substring(0, checkColon));
                int first = firstNum.intValue();
                if ( first < 1  ) {
                    throw new IllegalArgumentException("Not a positive integer2");
                }
                response.add( firstNum );

                Integer lastNum;
                int last;
                if (rawSet.indexOf("*") != -1) {
                    // Range from firstNum to '*'
                    // Add -1, to indicate unended range.
                    lastNum = new Integer( -1 );
                }
                else {
                    // Get the final num, and add all numbers in range.
                    lastNum = new Integer(rawSet.substring(checkColon + 1));
                    last = lastNum.intValue();
                    if ( last < 1 ) {
                        throw new IllegalArgumentException("Not a positive integer3");
                    }
                    if ( last < first ) {
                        throw new IllegalArgumentException("Not an increasing range");
                    }

                    for (int i = (first + 1); i <= last; i++) {
                        response.add(new Integer(i));
                    }
                }
            }
        }
        else {
            // Comma present, compound range.
            try {
                String firstRawSet = rawSet.substring( 0, checkComma );
                String secondRawSet = rawSet.substring( checkComma + 1 );
                response.addAll(decodeSet(firstRawSet, exists));
                response.addAll(decodeSet(secondRawSet, exists));
            } catch (IllegalArgumentException e) {
                getLogger().debug("Wonky arguments in: " + rawSet + " " + e);
                throw e;
            }
        }
        return response;
    }

    /**
     * Turns a protocol-compliant string representing a uid set into a
     * List of integers. Where the string requests ranges or uses the * (star)
     * wildcard, the results are uids that exist in the mailbox. This
     * minimizes attempts to refer to non-existent messages.
     *
     * @param rawSet the IMAP protocol compliant string to be decoded
     * @param uidsList List of uids of messages in mailbox
     * @return a List of Integers, one per message in set
     */
    protected List decodeUIDSet( String rawSet, List uidsList )
        throws IllegalArgumentException {
        if (rawSet == null) {
            getLogger().debug("Null argument in decodeSet");
            throw new IllegalArgumentException("Null argument");
        } else if (rawSet.equals("")) {
            getLogger().debug("Empty argument in decodeSet");
            throw new IllegalArgumentException("Empty string argument");
        }
        getLogger().debug(" decodeUIDSet called for: " + rawSet);
        System.out.println(" decodeUIDSet called for: " + rawSet);
        Iterator it = uidsList.iterator();
        while (it.hasNext()) {
            System.out.println ("uids present : " + (Integer)it.next() );
        }
        List response = new ArrayList();
        int checkComma = rawSet.indexOf(",");
        if (checkComma == -1) {
            int checkColon = rawSet.indexOf(":");
            if (checkColon == -1) {
                Integer seqNum = new Integer(rawSet.trim());
                if (seqNum.intValue() < 1) {
                    throw new IllegalArgumentException("Not a positive integer4");
                } else {
                    response.add(seqNum);
                }
            } else {
                Integer firstNum = new Integer(rawSet.substring(0, checkColon));
                int first = firstNum.intValue();

                Integer lastNum;
                if (rawSet.indexOf("*") == -1) {
                    lastNum = new Integer(rawSet.substring(checkColon + 1));
                } else {
                    lastNum = (Integer)uidsList.get(uidsList.size()-1);
                }
                int last;
                
                last = lastNum.intValue();
                if (first < 1 || last < 1) {
                    throw new IllegalArgumentException("Not a positive integer");
                } else if (first < last) {
                    Collection uids;
                    if(uidsList.size() > 50) {
                        uids = new HashSet(uidsList);
                    } else {
                        uids = uidsList;
                    }
                    Iterator ite = uids.iterator();
                    while (ite.hasNext()) {
                        int uidsint = ((Integer) ite.next()).intValue();
                        System.out.println("SCHLEIFEN  f "+first+" l "+last+" uidsint "+uidsint);
                        
                        if (uidsint >= first && uidsint <= last) {
                            response.add(new Integer(uidsint));
                        }
                    }
                } else if (first == last) {
                    response.add(firstNum);
                } else {
                    // Requests as 5:* are requested from Clients like Outlook to check, if there
                    // are new Mails incoming since the last request.
                    // So here no response (NULL List) and no error throwing
                    System.out.println("NULLLIST");
                }
            }
        } else {
            try {
                String firstRawSet = rawSet.substring(0, checkComma);
                String secondRawSet = rawSet.substring(checkComma + 1);
                response.addAll(decodeUIDSet(firstRawSet, uidsList));
                response.addAll(decodeUIDSet(secondRawSet, uidsList));
            } catch (IllegalArgumentException e) {
                getLogger().debug("Wonky arguments in: " + rawSet + " " + e);
                throw e;
            }
        }
        System.out.println("RETURNING");
        return response;
    }
    
    protected ACLMailbox getMailbox( ImapSession session, String mailboxName, String command )
    {
        if ( session.getState() == ImapSessionState.SELECTED && session.getCurrentFolder().equals( mailboxName ) ) {
            return session.getCurrentMailbox();
        }
        else {
            try {
                return session.getImapHost().getMailbox( session.getCurrentUser(), mailboxName );
            }
            catch ( MailboxException me ) {
                if ( me.isRemote() ) {
                    session.noResponse( "[REFERRAL " + me.getRemoteServer() + "]" + "Remote mailbox" );
                }
                else {
                    session.noResponse( command, "Unknown mailbox" );
                    getLogger().info( "MailboxException in method getBox for user: "
                                      + session.getCurrentUser() + " mailboxName: " + mailboxName + " was "
                                      + me.getMessage() );
                }
                return null;
            }
            catch ( AccessControlException e ) {
                session.noResponse( command, "Unknown mailbox" );
                return null;
            }
        }
    }
}
