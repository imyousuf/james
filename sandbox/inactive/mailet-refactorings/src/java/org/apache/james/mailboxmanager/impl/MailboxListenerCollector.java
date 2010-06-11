package org.apache.james.mailboxmanager.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.james.mailboxmanager.MailboxListener;
import org.apache.james.mailboxmanager.MessageResult;

public class MailboxListenerCollector implements MailboxListener {
    
    protected List addedList =new ArrayList();
    protected List expungedList =new ArrayList();
    protected List flaggedList =new ArrayList();

    public void added(MessageResult mr) {
        addedList.add(mr);
    }

    public void expunged(MessageResult mr) {
        expungedList.add(mr);
    }

    public void flagsUpdated(MessageResult mr, MailboxListener silentListener) {
        flaggedList.add(mr);
    }
    
    public synchronized List getAddedList(boolean reset) {
        List list=addedList;
        if (reset) {
            addedList=new ArrayList();
        }
        return list;
    }

    public synchronized List getExpungedList(boolean reset) {
        List list=expungedList;
        if (reset) {
            expungedList=new ArrayList();
        }
        return list;
    }

    public synchronized List getFlaggedList(boolean reset) {
        List list=flaggedList;
        if (reset) {
            flaggedList=new ArrayList();
        }
        return list;
    }

    public void mailboxDeleted() {
    }

    public void mailboxRenamed(String origName, String newName) {
    }

    public void mailboxRenamed(String newName) {
    }

}
