/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver;

import org.apache.james.BaseConnectionHandler;

import java.util.*;
//import org.apache.james.core.EnhancedMimeMessage;


/**
 * Provides methods useful for IMAP command objects.
 *
 * References: rfc 2060, rfc 2193, rfc 2221
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.1 on 17 Jan 2001
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
     * @returns a List of Integers, one per message in set
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
                        throw new IllegalArgumentException("Not a positive integer");
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
                    throw new IllegalArgumentException("Not a positive integer");
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
                        throw new IllegalArgumentException("Not a positive integer");
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
     * @returns a List of Integers, one per message in set
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
        Iterator it = uidsList.iterator();
        while (it.hasNext()) {
            getLogger().info ("uids present : " + (Integer)it.next() );
        }
        List response = new ArrayList();
        int checkComma = rawSet.indexOf(",");
        if (checkComma == -1) {
            int checkColon = rawSet.indexOf(":");
            if (checkColon == -1) {
                Integer seqNum = new Integer(rawSet.trim());
                if (seqNum.intValue() < 1) {
                    throw new IllegalArgumentException("Not a positive integer");
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
                    response.add(firstNum);
                    Collection uids;
                    if(uidsList.size() > 50) {
                        uids = new HashSet(uidsList);
                    } else {
                        uids = uidsList;
                    }
                    for (int i = (first + 1); i < last; i++) {
                        Integer test = new Integer(i);
                        if (uids.contains(test)) {
                            response.add(test);
                        }
                    }
                    response.add(lastNum);

                } else if (first == last) {
                    response.add(firstNum);
                } else {
                    throw new IllegalArgumentException("Not an increasing range");
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
        return response;
    }
}
