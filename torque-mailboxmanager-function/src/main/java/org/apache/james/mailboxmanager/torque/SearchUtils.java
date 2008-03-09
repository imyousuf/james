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

package org.apache.james.mailboxmanager.torque;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.mail.Flags;

import org.apache.james.mailboxmanager.SearchQuery;
import org.apache.james.mailboxmanager.SearchQuery.NumericRange;
import org.apache.james.mailboxmanager.torque.om.MessageFlags;
import org.apache.james.mailboxmanager.torque.om.MessageHeader;
import org.apache.james.mailboxmanager.torque.om.MessageRow;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.field.datetime.DateTime;
import org.apache.james.mime4j.field.datetime.parser.DateTimeParser;
import org.apache.james.mime4j.field.datetime.parser.ParseException;
import org.apache.torque.TorqueException;

/**
 * Uility methods to help perform search operations.
 */
class SearchUtils {
    
    /**
     * Does the row match the given criteria?
     * @param query <code>SearchQuery</code>, not null
     * @param row <code>MessageRow</code>, not null
     * @return true if the row matches the given criteria,
     * false otherwise
     * @throws TorqueException
     */
    public static boolean isMatch(final SearchQuery query, final MessageRow row) throws TorqueException {
        final List criteria = query.getCriterias();
        boolean result = true;
        if (criteria != null) {
            for (Iterator it = criteria.iterator(); it.hasNext();) {
                final SearchQuery.Criterion criterion = (SearchQuery.Criterion) it.next();
                if (!isMatch(criterion, row)) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }
    
    /**
     * Does the row match the given criterion?
     * @param query <code>SearchQuery.Criterion</code>, not null
     * @param row <code>MessageRow</code>, not null
     * @return true if the row matches the given criterion,
     * false otherwise
     * @throws TorqueException
     */
    public static boolean isMatch(SearchQuery.Criterion criterion, MessageRow row) throws TorqueException {
        final boolean result;
        if (criterion instanceof SearchQuery.InternalDateCriterion) {
            result = matches((SearchQuery.InternalDateCriterion) criterion, row);
        } else if (criterion instanceof SearchQuery.SizeCriterion) {
            result = matches((SearchQuery.SizeCriterion) criterion, row);
        } else if (criterion instanceof SearchQuery.HeaderCriterion) {
            result = matches((SearchQuery.HeaderCriterion) criterion, row);
        } else if (criterion instanceof SearchQuery.UidCriterion) {
            result = matches((SearchQuery.UidCriterion) criterion, row);
        } else if (criterion instanceof SearchQuery.FlagCriterion) {
            result = matches((SearchQuery.FlagCriterion) criterion, row);
        } else if (criterion instanceof SearchQuery.TextCriterion) {
            result = matches((SearchQuery.TextCriterion) criterion, row);
        } else if (criterion instanceof SearchQuery.AllCriterion) {
            result = true;
        } else if (criterion instanceof SearchQuery.ConjunctionCriterion) {
            result = matches((SearchQuery.ConjunctionCriterion) criterion, row);
        } else {
            throw new UnsupportedSearchException();
        }
        return result;
    }
    
    
    private static boolean matches(SearchQuery.TextCriterion criterion, MessageRow row) throws TorqueException {
        try {
            final SearchQuery.ContainsOperator operator = criterion.getOperator();
            final String value = operator.getValue();
            final int type = criterion.getType();
            switch (type) {
                case SearchQuery.TextCriterion.BODY: return bodyContains(value, row);
                case SearchQuery.TextCriterion.FULL_MESSAGE: return messageContains(value, row);
                default: throw new UnsupportedSearchException();
            }
        } catch (IOException e) {
            throw new TorqueException(e);
        } catch (MimeException e) {
            throw new TorqueException(e);
        }
    }
    
    private static boolean bodyContains(String value, MessageRow row) throws TorqueException, IOException, MimeException {
        final InputStream input = MessageRowUtils.toInput(row);
        final MessageSearcher searcher = new MessageSearcher(value, true, false);
        final boolean result = searcher.isFoundIn(input);
        return result;
    }

    private static boolean messageContains(String value, MessageRow row) throws TorqueException, IOException, MimeException {
        final InputStream input = MessageRowUtils.toInput(row);
        final MessageSearcher searcher = new MessageSearcher(value, true, true);
        final boolean result = searcher.isFoundIn(input);
        return result;
    }

    private static boolean matches(SearchQuery.ConjunctionCriterion criterion, MessageRow row) throws TorqueException {
        final int type = criterion.getType();
        final List criteria = criterion.getCriteria();
        switch (type) {
            case SearchQuery.ConjunctionCriterion.NOR: return nor(criteria, row);
            case SearchQuery.ConjunctionCriterion.OR: return or(criteria, row);
            case SearchQuery.ConjunctionCriterion.AND: return and(criteria, row);
            default: return false;
        }
    }
    
    private static boolean and(final List criteria, final MessageRow row) throws TorqueException {
        boolean result = true;
        for (Iterator it = criteria.iterator(); it.hasNext();) {
            final SearchQuery.Criterion criterion = (SearchQuery.Criterion) it.next();
            final boolean matches = isMatch(criterion, row);
            if (!matches) {
                result = false;
                break;
            }
        }
        return result;
    }
    
    private static boolean or(final List criteria, final MessageRow row) throws TorqueException {
        boolean result = false;
        for (Iterator it = criteria.iterator(); it.hasNext();) {
            final SearchQuery.Criterion criterion = (SearchQuery.Criterion) it.next();
            final boolean matches = isMatch(criterion, row);
            if (matches) {
                result = true;
                break;
            }
        }
        return result;
    }
    
    private static boolean nor(final List criteria, final MessageRow row) throws TorqueException {
        boolean result = true;
        for (Iterator it = criteria.iterator(); it.hasNext();) {
            final SearchQuery.Criterion criterion = (SearchQuery.Criterion) it.next();
            final boolean matches = isMatch(criterion, row);
            if (matches) {
                result = false;
                break;
            }
        }
        return result;
    }

    private static boolean matches(SearchQuery.FlagCriterion criterion, MessageRow row) throws TorqueException {
        final SearchQuery.BooleanOperator operator = criterion.getOperator();
        final boolean isSet = operator.isSet();
        final Flags.Flag flag = criterion.getFlag();
        final MessageFlags messageFlags = row.getMessageFlags();
        final boolean result;
        if (flag == Flags.Flag.ANSWERED) {
            result = isSet == messageFlags.getAnswered();
        } else if (flag == Flags.Flag.SEEN) {
            result = isSet == messageFlags.getSeen(); 
        } else if (flag == Flags.Flag.DRAFT) {
            result = isSet == messageFlags.getDraft(); 
        } else if (flag == Flags.Flag.FLAGGED) {
            result = isSet == messageFlags.getFlagged();
        } else if (flag == Flags.Flag.RECENT) {
            result = isSet == messageFlags.getRecent();
        } else if (flag == Flags.Flag.DELETED) {
            result = isSet == messageFlags.getDeleted();
        } else {
            result = false;
        }
        return result;
    }
    
    private static boolean matches(SearchQuery.UidCriterion criterion, MessageRow row) throws TorqueException {
        final SearchQuery.InOperator operator = criterion.getOperator();
        final NumericRange[] ranges = operator.getRange();
        final long uid = row.getUid();
        final int length = ranges.length;
        boolean result = false;
        for (int i = 0; i < length; i++) {
            final NumericRange numericRange = ranges[i];
            if (numericRange.isIn(uid)) {
                result = true;
                break;
            }
        }
        return result;
    }
    
    private static boolean matches(SearchQuery.HeaderCriterion criterion, MessageRow row) throws TorqueException {
        final SearchQuery.HeaderOperator operator = criterion.getOperator();
        final String headerName = criterion.getHeaderName();
        final boolean result;
        if (operator instanceof SearchQuery.DateOperator) {
            result = matches((SearchQuery.DateOperator) operator, headerName, row);
        } else if (operator instanceof SearchQuery.ContainsOperator) {
            result = matches((SearchQuery.ContainsOperator)operator, headerName, row);
        } else if (operator instanceof SearchQuery.ExistsOperator) {
            result = exists(headerName, row);
        } else {
            throw new UnsupportedSearchException();
        }
        return result;
    }

    private static boolean exists(String headerName, MessageRow row) throws TorqueException {
        boolean result = false;
        final List headers = row.getMessageHeaders();
        for (Iterator it = headers.iterator(); it.hasNext();) {
            final MessageHeader header = (MessageHeader) it.next();
            final String name = header.getField();
            if (headerName.equalsIgnoreCase(name)) {
                result = true;
                break;
            }
        }
        return result;
    }

    private static boolean matches(final SearchQuery.ContainsOperator operator, final String headerName, final MessageRow row) throws TorqueException {
        final String text = operator.getValue().toUpperCase();
        boolean result = false;
        final List headers = row.getMessageHeaders();
        for (Iterator it = headers.iterator(); it.hasNext();) {
            final MessageHeader header = (MessageHeader) it.next();
            final String name = header.getField();
            if (headerName.equalsIgnoreCase(name)) {
                final String value = header.getValue();
                if (value != null) {
                    if (value.toUpperCase().contains(text)) {
                        result = true;
                        break;
                    }
                }
            }
        }
        return result;
    }

    private static boolean matches(final SearchQuery.DateOperator operator, final String headerName, final MessageRow row) throws TorqueException {
        final int day = operator.getDay();
        final int month = operator.getMonth();
        final int year = operator.getYear();
        final int iso = toISODate(day, month, year);
        final String value = headerValue(headerName, row);
        if (value == null) {
            return false;
        } else {
            try {
                final int isoFieldValue = toISODate(value);
                final int type = operator.getType();
                switch (type) {
                    case SearchQuery.DateOperator.AFTER: return iso < isoFieldValue; 
                    case SearchQuery.DateOperator.BEFORE: return iso > isoFieldValue; 
                    case SearchQuery.DateOperator.ON: return iso == isoFieldValue;
                    default: throw new UnsupportedSearchException();
                }
            } catch (ParseException e) {
                return false;
            } 
        }
    }

    private static String headerValue(final String headerName, final MessageRow row) throws TorqueException {
        final List headers = row.getMessageHeaders();
        String value = null;
        for (Iterator it = headers.iterator(); it.hasNext();) {
            final MessageHeader header = (MessageHeader) it.next();
            final String name = header.getField();
            if (headerName.equalsIgnoreCase(name)) {
                value = header.getValue();
                break;
            }
        }
        return value;
    }

    private static int toISODate(String value) throws ParseException {
        final StringReader reader = new StringReader(value);
        final DateTime dateTime = new DateTimeParser(reader).parseAll();
        final int isoFieldValue = toISODate(dateTime.getDay(), dateTime.getMonth(), dateTime.getYear());
        return isoFieldValue;
    }
    
    private static boolean matches(SearchQuery.SizeCriterion criterion, MessageRow row) throws UnsupportedSearchException {
        final SearchQuery.NumericOperator operator = criterion.getOperator();
        final int size = row.getSize();
        final long value = operator.getValue();
        final int type = operator.getType();
        switch (type) {
            case SearchQuery.NumericOperator.LESS_THAN: return size < value;
            case SearchQuery.NumericOperator.GREATER_THAN: return size > value;
            case SearchQuery.NumericOperator.EQUALS: return size == value;
            default: throw new UnsupportedSearchException();
        }
    }
    
    private static boolean matches(SearchQuery.InternalDateCriterion criterion, MessageRow row) throws UnsupportedSearchException {
        final SearchQuery.DateOperator operator = criterion.getOperator();
        final boolean result = matchesInternalDate(operator, row);
        return result;
    }

    private static boolean matchesInternalDate(final SearchQuery.DateOperator operator, final MessageRow row) throws UnsupportedSearchException {
        final int day = operator.getDay();
        final int month = operator.getMonth();
        final int year = operator.getYear();
        final Date internalDate = row.getInternalDate();
        final int type = operator.getType();
        switch (type) {
            case SearchQuery.DateOperator.ON: return on(day, month, year, internalDate);
            case SearchQuery.DateOperator.BEFORE: return before(day, month, year, internalDate);
            case SearchQuery.DateOperator.AFTER: return after(day, month, year, internalDate);
            default: throw new UnsupportedSearchException();
        }
    }
    
    private static boolean on(final int day, final int month, final int year, final Date date) {
        final Calendar gmt = getGMT();
        gmt.setTime(date);
        return day == gmt.get(Calendar.DAY_OF_MONTH) 
                    && month == (gmt.get(Calendar.MONTH) + 1) 
                            && year == gmt.get(Calendar.YEAR);
    }
    
    private static boolean before(final int day, final int month, final int year, final Date date) {
        return toISODate(date) < toISODate(day, month, year);
    }
    
    private static boolean after(final int day, final int month, final int year, final Date date) {
        return toISODate(date) > toISODate(day, month, year);
    }
    
    private static int toISODate(final Date date) {
        final Calendar gmt = getGMT();
        gmt.setTime(date);
        final int day = gmt.get(Calendar.DAY_OF_MONTH);
        final int month = (gmt.get(Calendar.MONTH) + 1); 
        final int year = gmt.get(Calendar.YEAR);
        final int result = toISODate(day, month, year);
        return result;
    }
    
    private static Calendar getGMT() {
        return Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.UK);
    }
    
    private static int toISODate(final int day, final int month, final int year) {
        final int result = (year * 10000) + (month * 100) + day;
        return result;
    }
}
