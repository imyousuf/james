package org.apache.james.mailboxmanager.impl;

import javax.mail.Message;
import org.apache.james.mailboxmanager.GeneralMessageSet;

public class GeneralMessageSetImpl implements GeneralMessageSet {

    private int type;

    private long uidFrom;

    private long uidTo;

    private int msnFrom;

    private int msnTo;

    private GeneralMessageSetImpl() {
    }

    public int getType() {
        return type;
    }

    public long getUidFrom() throws IllegalStateException {
        if (type != TYPE_UID)
            throw new IllegalStateException("not in UID mode");
        return uidFrom;
    }

    public long getUidTo() throws IllegalStateException {
        if (type != TYPE_UID)
            throw new IllegalStateException("not in UID mode");
        return uidTo;
    }

    public int getMsnFrom() throws IllegalStateException {
        return msnFrom;
    }

    public int getMsnTo() throws IllegalStateException {
        return msnTo;
    }

    public String getKey() throws IllegalStateException {
        // TODO Auto-generated method stub
        return null;
    }

    public Message getMessage() throws IllegalStateException {
        // TODO Auto-generated method stub
        return null;
    }

    public static GeneralMessageSet oneUid(long uid) {
        GeneralMessageSetImpl gms = new GeneralMessageSetImpl();
        gms.type = TYPE_UID;
        gms.uidFrom = uid;
        gms.uidTo = uid;
        return gms;
    }

    public static GeneralMessageSet all() {
        GeneralMessageSetImpl gms = new GeneralMessageSetImpl();
        gms.type = TYPE_ALL;
        return gms;
    }

    public static GeneralMessageSet range(long lowVal, long highVal,
            boolean useUids) {
        GeneralMessageSetImpl gms = new GeneralMessageSetImpl();
        if (highVal==Long.MAX_VALUE) {
            highVal=-1;
        }
        if (useUids) {
            gms.type = TYPE_UID;
            gms.uidFrom = lowVal;
            gms.uidTo = highVal;
        } else {
            gms.type = TYPE_MSN;
            gms.msnFrom = (int) lowVal;
            gms.msnTo = (int) highVal;
        }
        return gms;
    }

    public static GeneralMessageSet uidRange(long from, long to) {
        GeneralMessageSetImpl gms = new GeneralMessageSetImpl();
        gms.type = TYPE_UID;
        gms.uidFrom = from;
        gms.uidTo = to;
        return gms;
    }

    public String toString() {
        return "TYPE: " + type + " UID: " + uidFrom + ":" + uidTo;
    }

    public boolean isValid() {
        if (type == TYPE_ALL) {
            return true;
        } else if (type == TYPE_UID) {
            if (uidTo < 0) {
                return true;
            } else {
                return (uidFrom <= uidTo);
            }
        } else if (type == TYPE_MSN) {
            if (msnTo < 0) {
                return true;
            } else {
                return (msnFrom <= msnTo);
            }
        } else {
            return false;
        }

    }

    public static GeneralMessageSet msnRange(int from, int to) {
        GeneralMessageSetImpl gms = new GeneralMessageSetImpl();
        gms.type = TYPE_MSN;
        gms.msnFrom = from;
        gms.msnTo = to;
        return gms;
    }

    public static GeneralMessageSet oneMsn(int msn) {
        GeneralMessageSetImpl gms = new GeneralMessageSetImpl();
        gms.type = TYPE_MSN;
        gms.msnFrom = msn;
        gms.msnTo = msn;
        return gms;
    }
}
