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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchParameters {

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
        	ALL, ANSWERED, DELETED, DRAFT, FLAGGED, NEW, OLD, RECENT, SEEN,
        	UNANSWERED, UNDELETED, UNDRAFT, UNFLAGGED, UNSEEN
        })));
    
    public final static Set STRING_SEARCH_TERMS = Collections.unmodifiableSet(new HashSet(Arrays.asList(new String[] {
        	BCC, BODY, CC, FROM, KEYWORD, SUBJECT, TEXT, TO, UNKEYWORD
        })));
    public final static Set DATE_SEARCH_TERMS = Collections.unmodifiableSet(new HashSet(Arrays.asList(new String[] {
        	BEFORE, ON, SENTBEFORE, SENTON, SENTSINCE , SINCE
        })));
    public final static Set NUMBER_SEARCH_TERMS = Collections.unmodifiableSet(new HashSet(Arrays.asList(new String[] {
        	LARGER, SMALLER
        })));

	public final static Set SPECIAL_SEARCH_TERMS = new HashSet(Arrays.asList(new String[] {
        	OR, NOT,UID, HEADER
    }));

		
	List criterias = new ArrayList();
	
	public void addCriteria(SearchCriteria crit) {
		criterias.add(crit);
	}
	
	public List getCriterias() {
		return criterias;
	}
	
	// @Override
	public String toString() {
		return "Search:"+criterias.toString();
	}
	
	public static class SearchCriteria {
		
		
		public String getName() {
			return "search-criteria";
		}
		
		// @Override
		public String toString() {
			return "['"+getName()+"']";
		}
	}

	public static class NamedSearchCriteria extends SearchCriteria {
		String name;
		
		public NamedSearchCriteria(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		
		// @Override
		public String toString() {
			return "['"+name+"']";
		}
	}

	public static class StringSearchCriteria extends NamedSearchCriteria {
		String value;

		public StringSearchCriteria(String name, String value) {
			super(name);
			this.value = value;
		}
		
		public String getValue() {
			return value;
		}
		public String toString() {
			return "['"+name+"':'"+value+"']";
		}
	}
	
	public static class NumberSearchCriteria extends NamedSearchCriteria {
		long value;

		public NumberSearchCriteria(String name, long value) {
			super(name);
			this.value = value;
		}
		
		public long getValue() {
			return value;
		}
		public String toString() {
			return "['"+name+"':'"+value+"']";
		}
	}
	
	

	public static class DateSearchCriteria extends NamedSearchCriteria {
		Date value;

		public DateSearchCriteria(String name, Date value) {
			super(name);
			this.value = value;
		}
		
		public Date getValue() {
			return value;
		}
		public String toString() {
			return "['"+name+"':'"+value+"']";
		}
		
	}

	public static class HeaderSearchCriteria extends SearchCriteria {
		String fieldName;
		String value;
		
		public HeaderSearchCriteria(String fieldName, String value) {
			this.fieldName = fieldName;
			this.value = value;
		}
		public String getName() {
			return HEADER;
		}

		public String getFieldName() {
			return fieldName;
		}
		
		public String getValue() {
			return value;
		}
		
		public String toString() {
			return "[header:'"+fieldName+"':'"+value+"']";
		}

	}
	
	public static class UIDSearchCriteria extends SearchCriteria {
        NumericRange[] idRanges;

		public UIDSearchCriteria(NumericRange[] idRanges) {
			this.idRanges = idRanges;
		}
		public String getName() {
			return UID;
		}
		
		public NumericRange[] getIdRanges() {
			return idRanges;
		}
	}
	
	public static class NotSearchCriteria extends SearchCriteria {
		SearchCriteria inverse;
		
		public NotSearchCriteria(SearchCriteria inverse) {
			this.inverse = inverse;
		}
		
		public String getName() {
			return NOT;
		}

		public SearchCriteria getInverse() {
			return inverse;
		}
		
		public String toString() {
			return "[NOT "+inverse+"]";
		}
		
	}
	public static class OrSearchCriteria extends SearchCriteria {
		SearchCriteria a,b;
		
		public OrSearchCriteria (SearchCriteria a, SearchCriteria b) {
			this.a = a;
			this.b = b;
		}
		
		public String getName() {
			return OR;
		}


		public SearchCriteria getFirst() {
			return a;
		}
		
		public SearchCriteria getSecond() {
			return b;
		}
		public String toString() {
			return "[OR "+a+' '+b+"]";
		}
		
	}

	
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
}
