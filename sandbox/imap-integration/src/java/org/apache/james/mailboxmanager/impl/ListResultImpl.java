package org.apache.james.mailboxmanager.impl;

import org.apache.james.mailboxmanager.ListResult;

public class ListResultImpl implements ListResult {

    private String name;
    private String delimiter;
    private String[] attributes=new String[0];

    public ListResultImpl(String name, String delimiter) {
        this.name=name;
        this.delimiter=delimiter;
    }

    public String[] getAttributes() {
        return attributes;
    }

    public String getHierarchyDelimiter() {
        return delimiter;
    }

    public String getName() {
        return name;
    }

}
