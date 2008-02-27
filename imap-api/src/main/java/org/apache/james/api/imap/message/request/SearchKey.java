package org.apache.james.api.imap.message.request;

import java.util.Arrays;

import org.apache.james.api.imap.message.IdRange;

/**
 * Atom key used by a search.
 * Build instances by factory methods.
 */
public final class SearchKey {

    // NUMBERS
    public static final int TYPE_SEQUENCE_SET = 1;
    public static final int TYPE_UID = 2; 
    // NO PARAMETERS
    public static final int TYPE_ALL = 3;
    public static final int TYPE_ANSWERED = 4;
    public static final int TYPE_DELETED = 5;
    public static final int TYPE_DRAFT = 6;
    public static final int TYPE_FLAGGED = 7;
    public static final int TYPE_NEW = 8;
    public static final int TYPE_OLD = 9;
    public static final int TYPE_RECENT = 10;
    public static final int TYPE_SEEN = 11;
    public static final int TYPE_UNANSWERED = 12;
    public static final int TYPE_UNDELETED = 13;
    public static final int TYPE_UNDRAFT = 14;
    public static final int TYPE_UNFLAGGED = 15;
    public static final int TYPE_UNSEEN = 16;
    // ONE VALUE
    public static final int TYPE_BCC = 17;
    public static final int TYPE_BODY = 18;
    public static final int TYPE_CC = 19;
    public static final int TYPE_FROM = 20;
    public static final int TYPE_KEYWORD = 21;
    public static final int TYPE_SUBJECT = 22;
    public static final int TYPE_TEXT = 23;
    public static final int TYPE_TO = 24;
    public static final int TYPE_UNKEYWORD = 25;
    // ONE DATE
    public static final int TYPE_BEFORE = 26;
    public static final int TYPE_ON = 27;
    public static final int TYPE_SENTBEFORE = 28;
    public static final int TYPE_SENTON = 29;
    public static final int TYPE_SENTSINCE = 30;
    public static final int TYPE_SINCE = 31;
    // FIELD VALUE
    public static final int TYPE_HEADER = 32;
    // ONE NUMBER
    public static final int TYPE_LARGER = 33;
    public static final int TYPE_SMALLER = 34;
    // NOT
    public static final int TYPE_NOT = 35;
    // OR
    public static final int TYPE_OR = 36;
    
    private static final SearchKey UNSEEN = new SearchKey(TYPE_UNSEEN,
            null, null, null, 0, null, null, null);

    private static final SearchKey UNFLAGGED = new SearchKey(
            TYPE_UNFLAGGED, null, null, null, 0, null, null, null);

    private static final SearchKey UNDRAFT = new SearchKey(TYPE_UNDRAFT,
            null, null, null, 0, null, null, null);

    private static final SearchKey UNDELETED = new SearchKey(
            TYPE_UNDELETED, null, null, null, 0, null, null, null);

    private static final SearchKey UNANSWERED = new SearchKey(
            TYPE_UNANSWERED, null, null, null, 0, null, null, null);

    private static final SearchKey SEEN = new SearchKey(TYPE_SEEN, null,
            null, null, 0, null, null, null);

    private static final SearchKey RECENT = new SearchKey(TYPE_RECENT,
            null, null, null, 0, null, null, null);

    private static final SearchKey OLD = new SearchKey(TYPE_OLD, null,
            null, null, 0, null, null, null);

    private static final SearchKey NEW = new SearchKey(TYPE_NEW, null,
            null, null, 0, null, null, null);

    private static final SearchKey FLAGGED = new SearchKey(TYPE_FLAGGED,
            null, null, null, 0, null, null, null);

    private static final SearchKey DRAFT = new SearchKey(TYPE_DRAFT, null,
            null, null, 0, null, null, null);

    private static final SearchKey DELETED = new SearchKey(TYPE_DELETED,
            null, null, null, 0, null, null, null);

    private static final SearchKey ANSWERED = new SearchKey(TYPE_ANSWERED,
            null, null, null, 0, null, null, null);

    private static final SearchKey ALL = new SearchKey(TYPE_ALL, null,
            null, null, 0, null, null, null);

    // NUMBERS
    public static SearchKey buildSequenceSet(IdRange[] ids) {
        return new SearchKey(TYPE_SEQUENCE_SET, null, null, null, 0, null,
                null, ids);
    }

    public static SearchKey buildUidSet(IdRange[] ids) {
        return new SearchKey(TYPE_UID, null, null, null, 0, null, null, ids);
    }

    // NO PARAMETERS
    public static SearchKey buildAll() {
        return ALL;
    }

    public static SearchKey buildAnswered() {
        return ANSWERED;
    }

    public static SearchKey buildDeleted() {
        return DELETED;
    }

    public static SearchKey buildDraft() {
        return DRAFT;
    }

    public static SearchKey buildFlagged() {
        return FLAGGED;
    }

    public static SearchKey buildNew() {
        return NEW;
    }

    public static SearchKey buildOld() {
        return OLD;
    }

    public static SearchKey buildRecent() {
        return RECENT;
    }

    public static SearchKey buildSeen() {
        return SEEN;
    }

    public static SearchKey buildUnanswered() {
        return UNANSWERED;
    }

    public static SearchKey buildUndeleted() {
        return UNDELETED;
    }

    public static SearchKey buildUndraft() {
        return UNDRAFT;
    }

    public static SearchKey buildUnflagged() {
        return UNFLAGGED;
    }

    public static SearchKey buildUnseen() {
        return UNSEEN;
    }

    // ONE VALUE
    public static SearchKey buildBcc(String value) {
        return new SearchKey(TYPE_BCC, null, null, null, 0, null, value,
                null);
    }

    public static SearchKey buildBody(String value) {
        return new SearchKey(TYPE_BODY, null, null, null, 0, null, value,
                null);
    }

    public static SearchKey buildCc(String value) {
        return new SearchKey(TYPE_CC, null, null, null, 0, null, value,
                null);
    }

    public static SearchKey buildFrom(String value) {
        return new SearchKey(TYPE_FROM, null, null, null, 0, null, value,
                null);
    }

    public static SearchKey buildKeyword(String value) {
        return new SearchKey(TYPE_KEYWORD, null, null, null, 0, null,
                value, null);
    }

    public static SearchKey buildSubject(String value) {
        return new SearchKey(TYPE_SUBJECT, null, null, null, 0, null,
                value, null);
    }

    public static SearchKey buildText(String value) {
        return new SearchKey(TYPE_TEXT, null, null, null, 0, null, value,
                null);
    }

    public static SearchKey buildTo(String value) {
        return new SearchKey(TYPE_TO, null, null, null, 0, null, value,
                null);
    }

    public static SearchKey buildUnkeyword(String value) {
        return new SearchKey(TYPE_UNKEYWORD, null, null, null, 0, null,
                value, null);
    }

    // ONE DATE
    public static SearchKey buildBefore(DayMonthYear date) {
        return new SearchKey(TYPE_BEFORE, date, null, null, 0, null, null,
                null);
    }

    public static SearchKey buildOn(DayMonthYear date) {
        return new SearchKey(TYPE_ON, date, null, null, 0, null, null, null);
    }

    public static SearchKey buildSentBefore(DayMonthYear date) {
        return new SearchKey(TYPE_SENTBEFORE, date, null, null, 0, null,
                null, null);
    }

    public static SearchKey buildSentOn(DayMonthYear date) {
        return new SearchKey(TYPE_SENTON, date, null, null, 0, null, null,
                null);
    }

    public static SearchKey buildSentSince(DayMonthYear date) {
        return new SearchKey(TYPE_SENTSINCE, date, null, null, 0, null,
                null, null);
    }

    public static SearchKey buildSince(DayMonthYear date) {
        return new SearchKey(TYPE_SINCE, date, null, null, 0, null, null,
                null);
    }

    // FIELD VALUE
    public static SearchKey buildHeader(String name, String value) {
        return new SearchKey(TYPE_HEADER, null, null, null, 0, name, value,
                null);
    }

    // ONE NUMBER
    public static SearchKey buildLarger(long size) {
        return new SearchKey(TYPE_LARGER, null, null, null, size, null,
                null, null);
    }

    public static SearchKey buildSmaller(long size) {
        return new SearchKey(TYPE_SMALLER, null, null, null, size, null,
                null, null);
    }

    // NOT
    public static SearchKey buildNot(SearchKey key) {
        return new SearchKey(TYPE_NOT, null, key, null, 0, null, null, null);
    }

    // OR
    public static SearchKey buildOr(SearchKey keyOne, SearchKey keyTwo) {
        return new SearchKey(TYPE_OR, null, keyOne, keyTwo, 0, null, null,
                null);
    }


    private final int type;
    private final DayMonthYear date;
    private final SearchKey one;
    private final SearchKey two;
    private final long size;
    private final String name;
    private final String value;
    private IdRange[] sequence;

    private SearchKey(final int type, final DayMonthYear date, final SearchKey one, final SearchKey two, 
            final long number, final String name, final String value, IdRange[] sequence) {
        super();
        this.type = type;
        this.date = date;
        this.one = one;
        this.two = two;
        this.size = number;
        this.name = name;
        this.value = value;
        this.sequence = sequence;
    }

    /**
     * Gets a date value to be search upon.
     * @return the date when: {@link #TYPE_BEFORE}, {@link #TYPE_ON},
     * {@link #TYPE_SENTBEFORE}, {@link #TYPE_SENTON}, {@link #TYPE_SENTSINCE},
     * {@link #TYPE_SINCE}; otherwise null
     */
    public final DayMonthYear getDate() {
        return date;
    }

    /**
     * Gets sequence numbers.
     * @return msn when {@link #TYPE_SEQUENCE_SET}, uids when {@link #TYPE_UID},
     * null otherwise
     */
    public final IdRange[] getSequenceNumbers() {
        return sequence;
    }

    /**
     * Gets the field name.
     * @return the field name when {@link #TYPE_HEADER},
     * null otherwise
     */
    public final String getName() {
        return name;
    }

    /**
     * Gets the size searched for.
     * @return the size when {@link #TYPE_LARGER} or {@link #TYPE_SMALLER},
     * otherwise 0
     */
    public final long getSize() {
        return size;
    }

    /**
     * Gets key one.
     * @return the key to be NOT'd when {@link #TYPE_NOT},
     * the first first to be OR'd when {@link #TYPE_OR},
     * null otherwise
     */
    public final SearchKey getKeyOne() {
        return one;
    }

    /**
     * Gets key two.
     * @return the second key to be OR'd when {@link #TYPE_OR},
     * otherwise null
     */
    public final SearchKey getKeyTwo() {
        return two;
    }

    /**
     * Gets the type of key.
     * @return the type
     */
    public final int getType() {
        return type;
    }


    /**
     * Gets the value to be searched for.
     * @return the value, 
     * or null when this type is not associated with a value.
     */
    public final String getValue() {
        return value;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((date == null) ? 0 : date.hashCode());
        result = PRIME * result + ((name == null) ? 0 : name.hashCode());
        result = PRIME * result + ((one == null) ? 0 : one.hashCode());
        result = PRIME * result + Arrays.hashCode(sequence);
        result = PRIME * result + (int) (size ^ (size >>> 32));
        result = PRIME * result + ((two == null) ? 0 : two.hashCode());
        result = PRIME * result + type;
        result = PRIME * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final SearchKey other = (SearchKey) obj;
        if (date == null) {
            if (other.date != null)
                return false;
        } else if (!date.equals(other.date))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (one == null) {
            if (other.one != null)
                return false;
        } else if (!one.equals(other.one))
            return false;
        if (!Arrays.equals(sequence, other.sequence))
            return false;
        if (size != other.size)
            return false;
        if (two == null) {
            if (other.two != null)
                return false;
        } else if (!two.equals(other.two))
            return false;
        if (type != other.type)
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }     
}