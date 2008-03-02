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

package org.apache.james.mailboxmanager;

import java.util.ArrayList;
import java.util.List;

import javax.mail.Flags.Flag;

/**
 * <p>Models a query used to search for messages.
 * A query is the logical <code>AND</code> of the contained criteria.
 * </p><p>
 * Each <code>Criterion</code> is composed of an <code>Operator</code>
 * (combining value and operation) together with field
 * information (optional since the criteria type may imply a particular
 * field).
 * Factory methods are provided for criteria.
 * </p> 
 */
public class SearchQuery {

    /**
     * Creates a filter for message size less than the given value
     * @param value messages with size less than this value will be selected
     * by the returned criterion
     * @return <code>Criterion</code>, not null
     */
    public static final Criterion sizeLessThan(long value) {
        return new SizeCriterion(new NumericOperator(value, NumericOperator.LESS_THAN));
    }

    /**
     * Creates a filter for message size greater than the given value
     * @param value messages with size greater than this value will be selected
     * by the returned criterion
     * @return <code>Criterion</code>, not null
     */
    public static final Criterion sizeGreaterThan(long value) {
        return new SizeCriterion(new NumericOperator(value, NumericOperator.GREATER_THAN));
    }
    
    /**
     * Creates a filter matching messages with internal date after the given date.
     * @param day one based day of the month
     * @param month one based month of the year
     * @param year year
     * @return <code>Criterion</code>, not null
     */
    public static final Criterion internalDateAfter(int day, int month, int year) {
        return new InternalDateCriterion(new DateOperator(DateOperator.AFTER, day, month, year));
    }

    /**
     * Creates a filter matching messages with internal date on the given date.
     * @param day one based day of the month
     * @param month one based month of the year
     * @param year year
     * @return <code>Criterion</code>, not null
     */
    public static final Criterion internalDateOn(int day, int month, int year) {
        return new InternalDateCriterion(new DateOperator(DateOperator.ON, day, month, year));
    }
    

    /**
     * Creates a filter matching messages with internal date before the given date.
     * @param day one based day of the month
     * @param month one based month of the year
     * @param year year
     * @return <code>Criterion</code>, not null
     */
    public static final Criterion internalDateBefore(int day, int month, int year) {
        return new InternalDateCriterion(new DateOperator(DateOperator.BEFORE, day, month, year));
    }
    

    /**
     * Creates a filter matching messages with the date of the given header
     * after the given date. If the header's value is not a date then it will
     * not be included.
     * @param headerName name of the header whose value will be compared, not null
     * @param day one based day of the month
     * @param month one based month of the year
     * @param year year
     * @return <code>Criterion</code>, not null
     */
    public static final Criterion headerDateAfter(String headerName, int day, int month, int year) {
        return new HeaderCriterion(headerName, new DateOperator(DateOperator.AFTER, day, month, year));
    }
    
    /**
     * Creates a filter matching messages with the date of the given header
     * on the given date. If the header's value is not a date then it will
     * not be included.
     * @param headerName name of the header whose value will be compared, not null
     * @param day one based day of the month
     * @param month one based month of the year
     * @param year year
     * @return <code>Criterion</code>, not null
     */
    public static final Criterion headerDateOn(String headerName, int day, int month, int year) {
        return new HeaderCriterion(headerName, new DateOperator(DateOperator.ON, day, month, year));
    }
    
    /**
     * Creates a filter matching messages with the date of the given header
     * before the given date. If the header's value is not a date then it will
     * not be included.
     * @param headerName name of the header whose value will be compared, not null
     * @param day one based day of the month
     * @param month one based month of the year
     * @param year year
     * @return <code>Criterion</code>, not null
     */
    public static final Criterion headerDateBefore(String headerName, int day, int month, int year) {
        return new HeaderCriterion(headerName, new DateOperator(DateOperator.BEFORE, day, month, year));
    }
    
    /**
     * Creates a filter matching messages whose header value contains the given value.
     * @param headerName name of the header whose value will be compared, not null
     * @param value when null or empty the existance of the header will be checked, 
     * otherwise contained value 
     * @return <code>Criterion</code>, not null
     */
    public static final Criterion headerContains(String headerName, String value) {
        if (value == null || value.length() == 0) {
            return headerExists(headerName);
        } else {
            return new HeaderCriterion(headerName, new ContainsOperator(value));
        }
    }
    
    /**
     * Creates a filter matching messages with a header matching the given name.
     * @param headerName name of the header whose value will be compared, not null
     * @return <code>Criterion</code>, not null
     */
    public static final Criterion headerExists(String headerName) {
        return new HeaderCriterion(headerName, ExistsOperator.exists());
    }
    
    /**
     * Creates a filter matching messages which contains the given text
     * either within the body or in the headers. Implementations may choose
     * to ignore mime parts which cannot be decoded to text.
     * @param value search value 
     * @return <code>Criterion</code>, not null
     */
    public static final Criterion mailContains(String value) {
        return new TextCriterion(value, TextCriterion.FULL_MESSAGE);
    }
    
    /**
     * Creates a filter matching messages which contains the given text
     * within the body. Implementations may choose
     * to ignore mime parts which cannot be decoded to text.
     * @param value search value 
     * @return <code>Criterion</code>, not null
     */
    public static final Criterion bodyContains(String value) {
        return new TextCriterion(value, TextCriterion.BODY);
    }
    
    /**
     * Creates a filter matching messages within any of the given ranges.
     * @param range <code>NumericRange</code>'s, not null
     * @return <code>Criterion</code>, not null
     */
    public static final Criterion uid(NumericRange[] range) {
        return new UidCriterion(range);
    }
    
    /**
     * Creates a filter composing the two different criteria.
     * @param one <code>Criterion</code>, not null
     * @param two <code>Criterion</code>, not null
     * @return <code>Criterion</code>, not null
     */
    public static final Criterion or(Criterion one, Criterion two) {
        final List criteria = new ArrayList();
        criteria.add(one);
        criteria.add(two);
        return new ConjunctionCriterion(ConjunctionCriterion.OR, criteria);
    }
    
    /**
     * Creates a filter composing the two different criteria.
     * @param one <code>Criterion</code>, not null
     * @param two <code>Criterion</code>, not null
     * @return <code>Criterion</code>, not null
     */
    public static final Criterion and(Criterion one, Criterion two) {
        final List criteria = new ArrayList();
        criteria.add(one);
        criteria.add(two);
        return new ConjunctionCriterion(ConjunctionCriterion.AND, criteria);
    }
    
    /**
     * Creates a filter inverting the given criteria.
     * @param criterion <code>Criterion</code>, not null
     * @return <code>Criterion</code>, not null
     */
    public static final Criterion not(Criterion criterion) {
        final List criteria = new ArrayList();
        criteria.add(criterion);
        return new ConjunctionCriterion(ConjunctionCriterion.NOR, criteria);
    }
    
    /**
     * Creates a filter on the given flag.
     * @param flag <code>Flag</code>, not null
     * @param isSet true if the messages with the flag set should be matched,
     * false otherwise 
     * @return <code>Criterion</code>, not null
     */
    public static final Criterion flagSet(final Flag flag, final boolean isSet) {
        final Criterion result;
        if (isSet) {
            result = flagIsSet(flag);
        } else {
            result = flagIsUnSet(flag);
        }
        return result;
    }
    
    /**
     * Creates a filter on the given flag selecting messages where the given
     * flag is selected.
     * @param flag <code>Flag</code>, not null
     * @return <code>Criterion</code>, not null
     */
    public static final Criterion flagIsSet(final Flag flag) {
        return new FlagCriterion(flag, BooleanOperator.set());
    }
    
    /**
     * Creates a filter on the given flag selecting messages where the given
     * flag is not selected.
     * @param flag <code>Flag</code>, not null
     * @return <code>Criterion</code>, not null
     */
    public static final Criterion flagIsUnSet(final Flag flag) {
        return new FlagCriterion(flag, BooleanOperator.unset());
    }
    
    /**
     * Creates a filter on the given flag.
     * @param flag <code>Flag</code>, not null
     * @param isSet true if the messages with the flag set should be matched,
     * false otherwise 
     * @return <code>Criterion</code>, not null
     */
    public static final Criterion flagSet(final String flag, final boolean isSet) {
        final Criterion result;
        if (isSet) {
            result = flagIsSet(flag);
        } else {
            result = flagIsUnSet(flag);
        }
        return result;
    }
    
    /**
     * Creates a filter on the given flag selecting messages where the given
     * flag is selected.
     * @param flag <code>Flag</code>, not null
     * @return <code>Criterion</code>, not null
     */
    public static final Criterion flagIsSet(final String flag) {
        return new CustomFlagCriterion(flag, BooleanOperator.set());
    }
    
    /**
     * Creates a filter on the given flag selecting messages where the given
     * flag is not selected.
     * @param flag <code>Flag</code>, not null
     * @return <code>Criterion</code>, not null
     */
    public static final Criterion flagIsUnSet(final String flag) {
        return new CustomFlagCriterion(flag, BooleanOperator.unset());
    }
    
    /**
     * Creates a filter matching all messages.
     * @return <code>Criterion</code>, not null
     */
    public static final Criterion all() {
        return AllCriterion.all();
    }
    
    
    List criterias = new ArrayList();
	
	public void andCriteria(Criterion crit) {
		criterias.add(crit);
	}
	
	public List getCriterias() {
		return criterias;
	}
	
	// @Override
	public String toString() {
		return "Search:"+criterias.toString();
	}
    
    /**
     * Numbers within a particular range.
     * Range includes both high and low boundaries.
     * May be a single value.
     * {@link Long#MAX_VALUE} represents unlimited in either direction.
     */
	public static final class NumericRange {
	    private final long lowValue;
        private final long highValue;
        
        public NumericRange(final long value) {
            super();
            this.lowValue = value;
            this.highValue = value;
        }
        
        public NumericRange(final long lowValue, final long highValue) {
            super();
            this.lowValue = lowValue;
            this.highValue = highValue;
        }
        
        public final long getHighValue() {
            return highValue;
        }
        
        public final long getLowValue() {
            return lowValue;
        }
        
        public String toString() {
            return "[" + lowValue + "->" + highValue + "]";
        }
    }
    
    /**
     * Marker superclass for criteria.
     */
    public static abstract class Criterion {}
    
    /**
     * Conjuction applying to the contained criteria.
     * {@link #getType} indicates how the conjoined criteria
     * should be related.
     */
    public static final class ConjunctionCriterion extends Criterion {
        /** Logical <code>AND</code> */
        public static final int AND = 1;
        /** Logical <code>OR</code> */
        public static final int OR = 2;
        /** Logical <code>NOT</code> */
        public static final int NOR = 3;
        
        private final int type;
        private final List criteria;
        
        public ConjunctionCriterion(final int type, final List criteria) {
            super();
            this.type = type;
            this.criteria = criteria;
        }
        
        /**
         * Gets the criteria related through this conjuction.
         * @return <code>List</code> of {@link Criterion}
         */
        public final List getCriteria() {
            return criteria;
        }
        
        /**
         * Gets the type of conjunction.
         * @return the type, either {@link #AND}, {@link #OR} or {@link NOR}
         */
        public final int getType() {
            return type;
        }
        
        
    }
    
    /**
     * Any message.
     */
    public static final class AllCriterion extends Criterion {
        private static final AllCriterion ALL = new AllCriterion();
        
        private static final Criterion all() {
            return ALL;
        }
    }
    
    /**
     * Message text.
     */
    public static final class TextCriterion extends Criterion {
        /**
         * Only the message body content.
         */
        public static final int BODY = 1;
        /**
         * The full message content including headers.
         */
        public static final int FULL_MESSAGE = 2;
        
        private final int type;
        private final ContainsOperator operator;

        private TextCriterion(final String value, final int type) {
            super();
            this.operator = new ContainsOperator(value);
            this.type = type;
        }
        
        /**
         * Gets the type of text to be searched.
         * @return the type, either {@link #BODY} or {@link #FULL_MESSAGE}
         */
        public final int getType() {
            return type;
        }

        /**
         * Gets the search operation and value to be evaluated.
         * @return the <code>Operator</code>, not null
         */
        public final ContainsOperator getOperator() {
            return operator;
        }
    }
    
    /**
     * Header value content search.
     */
    public static final class HeaderCriterion extends Criterion {
        private final HeaderOperator operator;
        private final String headerName;

        private HeaderCriterion(final String headerName, final HeaderOperator operator) {
            super();
            this.operator = operator;
            this.headerName = headerName;
        }

        /**
         * Gets the name of the header whose value is to be searched.
         * @return the headerName
         */
        public final String getHeaderName() {
            return headerName;
        }

        /**
         * Gets the search operation and value to be evaluated.
         * @return the <code>Operator</code>, not null
         */
        public final HeaderOperator getOperator() {
            return operator;
        }
    }
    
    /**
     * Filters on the internal date.
     */
    public static final class InternalDateCriterion extends Criterion {
        private final DateOperator operator;

        public InternalDateCriterion(final DateOperator operator) {
            super();
            this.operator = operator;
        }

        /**
         * Gets the search operation and value to be evaluated.
         * @return the <code>Operator</code>, not null
         */
        public final DateOperator getOperator() {
            return operator;
        }
    }
    
    /**
     * Filters on the size of the message in octets.
     */
    public static final class SizeCriterion extends Criterion {
        private final NumericOperator operator;
        private SizeCriterion(final NumericOperator operator) {
            super();
            this.operator = operator;
        }
        
        /**
         * Gets the search operation and value to be evaluated.
         * @return the <code>NumericOperator</code>, not null
         */
        public final NumericOperator getOperator() {
            return operator;
        }
    }
    
    /**
     * Filters on a custom flag valuation.
     */
    public static final class CustomFlagCriterion extends Criterion {
        private final String flag;
        private final BooleanOperator operator;
        
        private CustomFlagCriterion(final String flag, final BooleanOperator operator) {
            super();
            this.flag = flag;
            this.operator = operator;
        }
        
        /**
         * Gets the custom flag to be search.
         * @return the flag name, not null
         */
        public final String getFlag() {
            return flag;
        }
        
        /**
         * Gets the value to be tested.
         * @return the <code>BooleanOperator</code>, not null
         */
        public final BooleanOperator getOperator() {
            return operator;
        }
        
        
    }
    
    /**
     * Filters on a standard flag.
     */
    public static final class FlagCriterion extends Criterion {
        private final Flag flag;
        private final BooleanOperator operator;
        
        private FlagCriterion(final Flag flag, final BooleanOperator operator) {
            super();
            this.flag = flag;
            this.operator = operator;
        }

        /**
         * Gets the flag filtered on.
         * @return the flag, not null
         */
        public final Flag getFlag() {
            return flag;
        }

        /**
         * Gets the test to be preformed.
         * @return the <code>BooleanOperator</code>, not null
         */
        public final BooleanOperator getOperator() {
            return operator;
        }
    }
    
    /**
     * Filters on message identity.
     */
    public static final class UidCriterion extends Criterion {
        private final InOperator operator;

        public UidCriterion(final NumericRange[] ranges) {
            super();
            this.operator = new InOperator(ranges);
        }

        /**
         * Gets the filtering operation.
         * @return the <code>InOperator</code>
         */
        public final InOperator getOperator() {
            return operator;
        }
    }
    
    /**
     * Search operator.
     */
    public static abstract class Operator {}
    
    /**
     * Marks operator as suitable for header value searching.
     */
    public interface HeaderOperator {}
    
    /**
     * Contained value search.
     */
    public static final class ContainsOperator extends Operator implements HeaderOperator {
        private final String value;

        public ContainsOperator(final String value) {
            super();
            this.value = value;
        }

        /**
         * Gets the value to be searched for.
         * @return the value
         */
        public final String getValue() {
            return value;
        }
    }
    
    /**
     * Existance search.
     */
    public static final class ExistsOperator extends Operator implements HeaderOperator  {
        private static final ExistsOperator EXISTS = new ExistsOperator();
        
        public static final ExistsOperator exists() {
            return EXISTS;
        }
    }
        
    /**
     * Boolean value search.
     */
    public static final class BooleanOperator extends Operator {
        
        private static final BooleanOperator SET = new BooleanOperator(true);
        private static final BooleanOperator UNSET = new BooleanOperator(false);
        
        public static final BooleanOperator set() {
            return SET;
        }
        
        public static final BooleanOperator unset() {
            return UNSET;
        }
        
        private final boolean set;

        private BooleanOperator(final boolean set) {
            super();
            this.set = set;
        }

        /**
         * Is the search for set?
         * @return true indicates that set values 
         * should be selected, false indicates
         * that unset values should be selected
         */
        public final boolean isSet() {
            return set;
        }
    }
    
    /**
     * Searches numberic values.
     */
    public static final class NumericOperator extends Operator {
        public static final int EQUALS = 1;
        public static final int LESS_THAN = 2;
        public static final int GREATER_THAN = 3;
        
        private final long value;
        private final int type;
        
        private NumericOperator(final long value, final int type) {
            super();
            this.value = value;
            this.type = type;
        }

        /**
         * Gets the operation type
         * @return the type either {@link #EQUALS}, {@link #LESS_THAN} or {@link #GREATER_THAN}
         */
        public final int getType() {
            return type;
        }

        /**
         * Gets the value to be compared.
         * @return the value
         */
        public final long getValue() {
            return value;
        }
    }
    
    /**
     * Operates on a date.
     */
    public static final class DateOperator extends Operator implements HeaderOperator  {
        public static final int BEFORE = 1;
        public static final int AFTER = 2;
        public static final int ON = 3;
        
        private final int type;
        private final int day;
        private final int month;
        private final int year;
        
        public DateOperator(final int type, final int day, final int month, final int year) {
            super();
            this.type = type;
            this.day = day;
            this.month = month;
            this.year = year;
        }
        
        /**
         * Gets the day-of-the-month.
         * @return the day, one based
         */
        public final int getDay() {
            return day;
        }
        
        /**
         * Gets the month-of-the-year.
         * @return the month, one based
         */
        public final int getMonth() {
            return month;
        }
        
        /**
         * Gets the operator type.
         * @return the type, either {@link #BEFORE}, {@link #AFTER} or {@link ON}
         */
        public final int getType() {
            return type;
        }
        
        /**
         * Gets the year.
         * @return the year
         */
        public final int getYear() {
            return year;
        }
    }
    
    /**
     * Search for numbers within set of ranges.
     */
    public static final class InOperator extends Operator {
        private final NumericRange[] range;

        public InOperator(final NumericRange[] range) {
            super();
            this.range = range;
        }

        /**
         * Gets the filtering ranges.
         * Values falling within these ranges will be selected. 
         * @return the <code>NumericRange</code>'s search on, not null
         */
        public final NumericRange[] getRange() {
            return range;
        }
    }
}
