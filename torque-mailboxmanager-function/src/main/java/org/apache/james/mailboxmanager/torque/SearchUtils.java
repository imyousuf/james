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

import java.io.StringReader;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.james.mailboxmanager.SearchQuery;
import org.apache.james.mailboxmanager.torque.om.MessageHeader;
import org.apache.james.mailboxmanager.torque.om.MessageRow;
import org.apache.james.mime4j.field.datetime.DateTime;
import org.apache.james.mime4j.field.datetime.parser.DateTimeParser;
import org.apache.james.mime4j.field.datetime.parser.ParseException;
import org.apache.torque.TorqueException;

/**
 * Uility methods to help perform search operations.
 */
class SearchUtils {
    
    public static boolean matches(SearchQuery.Criterion criterion, MessageRow row) throws TorqueException {
        final boolean result;
        if (criterion instanceof SearchQuery.InternalDateCriterion) {
            result = matches((SearchQuery.InternalDateCriterion) criterion, row);
        } else if (criterion instanceof SearchQuery.SizeCriterion) {
            result = matches((SearchQuery.SizeCriterion) criterion, row);
        } else if (criterion instanceof SearchQuery.HeaderCriterion) {
            result = matches((SearchQuery.HeaderCriterion) criterion, row);
        } else {
            throw new UnsupportedSearchException();
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
        final String text = operator.getValue();
        boolean result = false;
        final List headers = row.getMessageHeaders();
        for (Iterator it = headers.iterator(); it.hasNext();) {
            final MessageHeader header = (MessageHeader) it.next();
            final String name = header.getField();
            if (headerName.equalsIgnoreCase(name)) {
                final String value = header.getValue();
                if (value.contains(text)) {
                    result = true;
                    break;
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
