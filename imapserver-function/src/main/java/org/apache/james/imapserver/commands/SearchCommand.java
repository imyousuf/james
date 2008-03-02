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

package org.apache.james.imapserver.commands;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.mail.Flags.Flag;

import org.apache.james.imapserver.ImapRequestLineReader;
import org.apache.james.imapserver.ImapResponse;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.ProtocolException;
import org.apache.james.imapserver.SelectedMailboxSession;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.SearchQuery;
import org.apache.james.mailboxmanager.MessageResult.FetchGroup;
import org.apache.james.mailboxmanager.SearchQuery.Criterion;
import org.apache.james.mailboxmanager.SearchQuery.NumericRange;
import org.apache.james.mailboxmanager.impl.FetchGroupImpl;
import org.apache.james.mailboxmanager.mailbox.ImapMailbox;
import org.apache.mailet.RFC2822Headers;

/**
 * Handles processeing for the SEARCH imap command.
 *
 * @version $Revision: 109034 $
 */
class SearchCommand extends SelectedStateCommand implements UidEnabledCommand
{
    public static final String HEADER = "HEADER";
    public static final String UID = "UID";
    public static final String NOT = "NOT";
    public static final String OR = "OR";
    public static final String SMALLER = "SMALLER";
    public static final String LARGER = "LARGER";
    public static final String SINCE = "SINCE";
    public static final String SENTSINCE = "SENTSINCE";
    public static final String SENTON = "SENTON";
    public static final String SENTBEFORE = "SENTBEFORE";
    public static final String ON = "ON";
    public static final String BEFORE = "BEFORE";
    public static final String UNKEYWORD = "UNKEYWORD";
    public static final String TO = "TO";
    public static final String TEXT = "TEXT";
    public static final String SUBJECT = "SUBJECT";
    public static final String KEYWORD = "KEYWORD";
    public static final String FROM = "FROM";
    public static final String CC = "CC";
    public static final String BODY = "BODY";
    public static final String BCC = "BCC";
    public static final String UNSEEN = "UNSEEN";
    public static final String UNFLAGGED = "UNFLAGGED";
    public static final String UNDRAFT = "UNDRAFT";
    public static final String UNDELETED = "UNDELETED";
    public static final String UNANSWERED = "UNANSWERED";
    public static final String SEEN = "SEEN";
    public static final String RECENT = "RECENT";
    public static final String OLD = "OLD";
    public static final String NEW = "NEW";
    public static final String FLAGGED = "FLAGGED";
    public static final String DRAFT = "DRAFT";
    public static final String DELETED = "DELETED";
    public static final String ANSWERED = "ANSWERED";
    public static final String ALL = "ALL";
    
    public final static Set BASE_SEARCH_TERMS = Collections.unmodifiableSet(new HashSet(Arrays.asList(new String[] {
            ALL, ANSWERED, DELETED, DRAFT, FLAGGED, OLD, RECENT, SEEN,
            UNANSWERED, UNDELETED, UNDRAFT, UNFLAGGED, UNSEEN
    })));
    public final static Set STRING_SEARCH_TERMS = Collections.unmodifiableSet(new HashSet(Arrays.asList(new String[] {
            BCC, BODY, CC, FROM, SUBJECT, TO
    })));
    public final static Set DATE_SEARCH_TERMS = Collections.unmodifiableSet(new HashSet(Arrays.asList(new String[] {
            BEFORE, ON, SENTBEFORE, SENTON, SENTSINCE , SINCE
    })));
    public final static Set SPECIAL_SEARCH_TERMS = new HashSet(Arrays.asList(new String[] {
            OR, NOT, UID, HEADER
    }));
    public final static Set NUMBER_SEARCH_TERMS = Collections.unmodifiableSet(new HashSet(Arrays.asList(new String[] {
            LARGER, SMALLER
    })));

    public static final String NAME = "SEARCH";
    public static final String ARGS = "<search term>";

    private SearchCommandParser parser = new SearchCommandParser();


    /** @see CommandTemplate#doProcess */
    protected void doProcess( ImapRequestLineReader request,
                              ImapResponse response,
                              ImapSession session )
            throws ProtocolException, MailboxException
    {
        doProcess( request, response, session, false );
    }

    public void doProcess( ImapRequestLineReader request,
                              ImapResponse response,
                              ImapSession session,
                              boolean useUids )
            throws ProtocolException, MailboxException
    {
        // Parse the search term from the request
        SearchQuery searchTerm = parser.searchTerm( request );
        parser.endLine( request );

        final SelectedMailboxSession selected = session.getSelected();
        ImapMailbox mailbox = selected.getMailbox();
        final FetchGroup result = FetchGroupImpl.MINIMAL;
        final Iterator it;
        try {
            it = mailbox.search(searchTerm,result, session.getMailboxSession());
        } catch (MailboxManagerException e) {
          throw new MailboxException(e);
        }
        StringBuffer idList = new StringBuffer();
        boolean first = true;
        while (it.hasNext()) {
            if ( first ) {
                first = false;
            } else {
                idList.append( SP );
            }
            final MessageResult message = (MessageResult) it.next();
            final long uid = message.getUid();
            if ( useUids ) {
                idList.append( uid );
            } else {
                final int msn = selected.msn(uid);
                idList.append( msn );
            }
        }
        
        response.commandResponse( this, idList.toString() );
        boolean omitExpunged = (!useUids);
        session.unsolicitedResponses( response, omitExpunged, useUids );
        response.commandComplete( this );
    }

    /** @see ImapCommand#getName */
    public String getName()
    {
        return NAME;
    }

    /** @see CommandTemplate#getArgSyntax */
    public String getArgSyntax()
    {
        return ARGS;
    }

    
    
    private class SearchCommandParser extends CommandParser
    {
        /**
         * Parses the request argument into a valid search term.
         */
        public SearchQuery searchTerm( ImapRequestLineReader request )
                throws ProtocolException
        {
        	SearchQuery search = new SearchQuery();

        	char next = request.nextChar();
            while ( next != '\n' && next != '\r') {
                
            	SearchQuery.Criterion crit = parseCriteria(request);
            	search.andCriteria(crit);
                next = request.nextChar();
                while ( next == ' ' ) {
                	request.consume();
                    next = request.nextChar();
                }
            }

            return search;
        }

		private Criterion parseCriteria(ImapRequestLineReader request) throws ProtocolException {
            String term = atom(request).toUpperCase();
            if (ALL.equals(term)) {
                return SearchQuery.all();
            } else if (NEW.equals(term)) {
                return SearchQuery.and(flag(RECENT), flag(UNSEEN));
            } else if (KEYWORD.equals(term)) {
                return keyword(true, request);
            } else if (UNKEYWORD.equals(term)) {
                return keyword(false, request);
            } else if (TEXT.equals(term)) {
                return SearchQuery.mailContains(astring(request));
            } else if (BODY.equals(term)) {
                return SearchQuery.bodyContains(astring(request));
            } else if (SearchCommand.BASE_SEARCH_TERMS.contains(term)) {
				return flag(term);
			} else if (SearchCommand.STRING_SEARCH_TERMS.contains(term)) {
				return header(term, astring(request));
			} else if (SearchCommand.NUMBER_SEARCH_TERMS.contains(term)) {
				return numeric(request, term);
			} else if (SearchCommand.DATE_SEARCH_TERMS.contains(term)) {
				return date(request, term);
			} else if ("HEADER".equals(term)) {
				return SearchQuery.headerContains(astring(request), astring(request));
			} else if ("UID".equals(term)) {
				return SearchQuery.uid(toNumericRange(parseIdRange(request)));
			} else if ("OR".equals(term)) {
				return SearchQuery.or(parseCriteria(request), parseCriteria(request));
			} else if ("NOT".equals(term)) {
				return SearchQuery.not(parseCriteria(request));
			} else {
				throw new ProtocolException("Term '"+term+"' not supported in the current search implementation!");
			}
		}
        
        private Criterion header(String term, String value) throws ProtocolException {
            final String headerName;
            if ( BCC.equals(term)) {
                headerName = RFC2822Headers.BCC;
            } else if (CC.equals(term)) {
                headerName = RFC2822Headers.CC;
            } else if (FROM.equals(term)) {
                headerName = RFC2822Headers.FROM;
            } else if (SUBJECT.equals(term)) {
                headerName = RFC2822Headers.SUBJECT;
            } else if (TO.equals(term)) { 
                headerName = RFC2822Headers.TO;
            } else {
                throw new ProtocolException("Unknown search key");
            }
            return SearchQuery.headerContains(headerName, value);
        }

        private Criterion keyword(boolean isSet, ImapRequestLineReader request) throws ProtocolException {
            final String keyword = astring(request);
            return SearchQuery.flagSet(keyword, isSet);
        }

        private Criterion flag(String term) throws ProtocolException {
            final Flag flag;
            final boolean value;
            if (ANSWERED.equals(term)) {
                flag = Flag.ANSWERED;
                value = true;
            } else if (DELETED.equals(term)) {
                flag = Flag.DELETED;
                value = true;                
            } else if (DRAFT.equals(term)) {
                flag = Flag.DRAFT;
                value = true;
            } else if (FLAGGED.equals(term)) {
                flag = Flag.FLAGGED;
                value = true;
            } else if (OLD.equals(term)) {
                flag = Flag.RECENT;
                value = false;
            } else if (RECENT.equals(term)) {
                flag = Flag.RECENT;
                value = true;
            } else if (SEEN.equals(term)) {
                flag = Flag.SEEN;
                value = true;
            } else if (UNANSWERED.equals(term)) {
                flag = Flag.ANSWERED;
                value = false;
            } else if (UNDELETED.equals(term)) {
                flag = Flag.DELETED;
                value = false;
            } else if (UNDRAFT.equals(term)) {
                flag = Flag.DRAFT;
                value = false;
            } else if (UNFLAGGED.equals(term)) {
                flag = Flag.FLAGGED;
                value = false;
            } else if (UNSEEN.equals(term)) {
                flag = Flag.SEEN;
                value = false;
            } else {
                throw new ProtocolException("Unknown search key");
            }
            final Criterion result = SearchQuery.flagSet(flag, value);
            return result;
        }

        private Criterion date(ImapRequestLineReader request, String term) throws ProtocolException {
            final Date date = date(request);
            final Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            final int day =  calendar.get(Calendar.DAY_OF_MONTH);
            final int month = calendar.get(Calendar.MONTH) + 1;
            final int year = calendar.get(Calendar.YEAR);
            final Criterion result;
            if (BEFORE.equals(term)) {
                result = SearchQuery.internalDateBefore(day, month, year);
            } else if (ON.equals(term)) {
                result = SearchQuery.internalDateOn(day, month, year);
            } else if (SENTBEFORE.equals(term)) {
                result = SearchQuery.headerDateBefore(RFC2822Headers.DATE, day, month, year);
            } else if (SENTON.equals(term)) {
                result = SearchQuery.headerDateBefore(RFC2822Headers.DATE, day, month, year);
            } else if (SENTSINCE.equals(term)) {
                result = SearchQuery.headerDateBefore(RFC2822Headers.DATE, day, month, year);
            } else if (SINCE.equals(term)) {
                result = SearchQuery.internalDateAfter(day, month, year);
            } else {
                throw new ProtocolException("Unknown search key");
            }
            return result;
        }

        private Criterion numeric(ImapRequestLineReader request, String term) throws ProtocolException {
            final long value = number(request);
            final Criterion result;
            if (LARGER.equals(term)) {
                result = SearchQuery.sizeGreaterThan(value);
            } else if (SMALLER.equals(term)) {
                result = SearchQuery.sizeLessThan(value);
            } else {
                throw new ProtocolException("Unknown search key");
            }
            return result;
        }

        private NumericRange[] toNumericRange(final IdRange[] ranges) {
            final NumericRange[] result;
            if (ranges == null) {
                result = null;
            } else {
                final int length = ranges.length;
                result = new NumericRange[length];
                for (int i=0;i<length; i++) {
                    result[i] = toNumericRange(ranges[i]);
                }
            }
            
            return result;
        }

        private NumericRange toNumericRange(IdRange range) {
            final NumericRange result;
            if (range == null) {
                result = null;
            } else {
                result = new NumericRange(range.getLowVal(), range.getHighVal());
            }
            return result;
        }

    }
    
    
}
/*
6.4.4.  SEARCH Command

   Arguments:  OPTIONAL [CHARSET] specification
               searching criteria (one or more)

   Responses:  REQUIRED untagged response: SEARCH

   Result:     OK - search completed
               NO - search error: can't search that [CHARSET] or
                    criteria
               BAD - command unknown or arguments invalid

      The SEARCH command searches the mailbox for messages that match
      the given searching criteria.  Searching criteria consist of one
      or more search keys.  The untagged SEARCH response from the server
      contains a listing of message sequence numbers corresponding to
      those messages that match the searching criteria.

      When multiple keys are specified, the result is the intersection
      (AND function) of all the messages that match those keys.  For
      example, the criteria DELETED FROM "SMITH" SINCE 1-Feb-1994 refers
      to all deleted messages from Smith that were placed in the mailbox
      since February 1, 1994.  A search key can also be a parenthesized
      list of one or more search keys (e.g. for use with the OR and NOT
      keys).

      Server implementations MAY exclude [MIME-IMB] body parts with
      terminal content media types other than TEXT and MESSAGE from
      consideration in SEARCH matching.

      The OPTIONAL [CHARSET] specification consists of the word
      "CHARSET" followed by a registered [CHARSET].  It indicates the
      [CHARSET] of the strings that appear in the search criteria.
      [MIME-IMB] content transfer encodings, and [MIME-HDRS] strings in
      [RFC-822]/[MIME-IMB] headers, MUST be decoded before comparing
      text in a [CHARSET] other than US-ASCII.  US-ASCII MUST be
      supported; other [CHARSET]s MAY be supported.  If the server does
      not support the specified [CHARSET], it MUST return a tagged NO
      response (not a BAD).

      In all search keys that use strings, a message matches the key if
      the string is a substring of the field.  The matching is case-
      insensitive.

      The defined search keys are as follows.  Refer to the Formal
      Syntax section for the precise syntactic definitions of the
      arguments.

      <message set>  Messages with message sequence numbers
                     corresponding to the specified message sequence
                     number set

      ALL            All messages in the mailbox; the default initial
                     key for ANDing.

      ANSWERED       Messages with the \Answered flag set.

      BCC <string>   Messages that contain the specified string in the
                     envelope structure's BCC field.

      BEFORE <date>  Messages whose internal date is earlier than the
                     specified date.

      BODY <string>  Messages that contain the specified string in the
                     body of the message.

      CC <string>    Messages that contain the specified string in the
                     envelope structure's CC field.

      DELETED        Messages with the \Deleted flag set.

      DRAFT          Messages with the \Draft flag set.

      FLAGGED        Messages with the \Flagged flag set.

      FROM <string>  Messages that contain the specified string in the
                     envelope structure's FROM field.

      HEADER <field-name> <string>
                     Messages that have a header with the specified
                     field-name (as defined in [RFC-822]) and that
                     contains the specified string in the [RFC-822]
                     field-body.

      KEYWORD <flag> Messages with the specified keyword set.

      LARGER <n>     Messages with an [RFC-822] size larger than the
                     specified number of octets.

      NEW            Messages that have the \Recent flag set but not the
                     \Seen flag.  This is functionally equivalent to
                     "(RECENT UNSEEN)".

      NOT <search-key>
                     Messages that do not match the specified search
                     key.

      OLD            Messages that do not have the \Recent flag set.
                     This is functionally equivalent to "NOT RECENT" (as
                     opposed to "NOT NEW").

      ON <date>      Messages whose internal date is within the
                     specified date.

      OR <search-key1> <search-key2>
                     Messages that match either search key.

      RECENT         Messages that have the \Recent flag set.

      SEEN           Messages that have the \Seen flag set.

      SENTBEFORE <date>
                     Messages whose [RFC-822] Date: header is earlier
                     than the specified date.

      SENTON <date>  Messages whose [RFC-822] Date: header is within the
                     specified date.

      SENTSINCE <date>
                     Messages whose [RFC-822] Date: header is within or
                     later than the specified date.

      SINCE <date>   Messages whose internal date is within or later
                     than the specified date.

      SMALLER <n>    Messages with an [RFC-822] size smaller than the
                     specified number of octets.

      SUBJECT <string>
                     Messages that contain the specified string in the
                     envelope structure's SUBJECT field.

      TEXT <string>  Messages that contain the specified string in the
                     header or body of the message.

      TO <string>    Messages that contain the specified string in the
                     envelope structure's TO field.

      UID <message set>
                     Messages with unique identifiers corresponding to
                     the specified unique identifier set.

      UNANSWERED     Messages that do not have the \Answered flag set.

      UNDELETED      Messages that do not have the \Deleted flag set.

      UNDRAFT        Messages that do not have the \Draft flag set.

      UNFLAGGED      Messages that do not have the \Flagged flag set.

      UNKEYWORD <flag>
                     Messages that do not have the specified keyword
                     set.

      UNSEEN         Messages that do not have the \Seen flag set.

   Example:    C: A282 SEARCH FLAGGED SINCE 1-Feb-1994 NOT FROM "Smith"
               S: * SEARCH 2 84 882
               S: A282 OK SEARCH completed



7.2.5.  SEARCH Response

   Contents:   zero or more numbers

      The SEARCH response occurs as a result of a SEARCH or UID SEARCH
      command.  The number(s) refer to those messages that match the
      search criteria.  For SEARCH, these are message sequence numbers;
      for UID SEARCH, these are unique identifiers.  Each number is
      delimited by a space.

   Example:    S: * SEARCH 2 3 6

*/
