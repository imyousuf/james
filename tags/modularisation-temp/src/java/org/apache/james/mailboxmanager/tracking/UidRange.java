package org.apache.james.mailboxmanager.tracking;

public class UidRange implements Comparable {
    
    private long fromUid;
    
    private long toUid;
    
    public UidRange(long fromUid,long toUid) {
        this.fromUid=fromUid;
        this.toUid=toUid;
    }

    public long getFromUid() {
        return fromUid;
    }

    public long getToUid() {
        return toUid;
    }
    
    public String toString() {
        return fromUid+":"+toUid;
    }

    public int compareTo(Object o) {
        if (!(o instanceof UidRange)) {
            return 1;
        } else {
            UidRange that = (UidRange) o;
            return new Long(fromUid).compareTo(new Long(that.fromUid));
        }
    }

}
